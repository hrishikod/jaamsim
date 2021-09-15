/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.Federate;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.TimeUnit;

/**
 * Moves one or more Entities along a path at a constant speed.
 */
public class EntityConveyor extends LinkedService implements LineEntity {

	@Keyword(description = "The travel time for the conveyor.",
	         exampleList = {"10.0 s"})
	private final SampleInput travelTimeInput;

	@Keyword(description = "The width of the conveyor in pixels.",
	         exampleList = {"1"})
	private final IntegerInput widthInput;

	@Keyword(description = "The colour of the conveyor.",
	         exampleList = {"red"})
	private final ColourInput colorInput;

	private final ArrayList<ConveyorEntry> entryList;  // List of the entities being conveyed
	private double presentTravelTime;

	{
		operatingThresholdList.setHidden(true);
		waitQueue.setHidden(true);
		match.setHidden(true);
		processPosition.setHidden(true);
		forcedMaintenanceList.setHidden(true);
		forcedBreakdownList.setHidden(true);

		travelTimeInput = new SampleInput("TravelTime", KEY_INPUTS, new SampleConstant(0.0d));
		travelTimeInput.setValidRange(0.0, Double.POSITIVE_INFINITY);
		travelTimeInput.setUnitType(TimeUnit.class);
		travelTimeInput.setEntity(this);
		this.addInput(travelTimeInput);

		widthInput = new IntegerInput("LineWidth", FORMAT, 1);
		widthInput.setValidRange(1, Integer.MAX_VALUE);
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);
		this.addSynonym(widthInput, "Width");

		colorInput = new ColourInput("LineColour", FORMAT, ColourInput.BLACK);
		colorInput.setDefaultText("PolylineModel");
		this.addInput(colorInput);
		this.addSynonym(colorInput, "Colour");
		this.addSynonym(colorInput, "Color");
	}

	public EntityConveyor() {
		entryList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		entryList.clear();
		presentTravelTime = 0.0d;
	}

	@Override
	public void startUp() {
		super.startUp();
		presentTravelTime = travelTimeInput.getValue().getNextSample(0.0);
	}

	private static class ConveyorEntry {
		final DisplayEntity entity;
		double position;

		public ConveyorEntry(DisplayEntity ent, double pos) {
			entity = ent;
			position = pos;
		}

		@Override
		public String toString() {
			return String.format("(%s, %.6f)", entity, position);
		}
	}

	@Override
	public void addEntity(DisplayEntity ent ) {
		super.addEntity(ent);
		double simTime = this.getSimTime();

		// Update the positions of the entities on the conveyor
		this.updateProgress();

		// Update the travel time
		this.updateTravelTime(simTime);

		// Add the entity to the conveyor
		ConveyorEntry entry = new ConveyorEntry(ent, 0.0d);
		entryList.add(entry);
		//Added by Jalal
		
		ArrayList<String> atts = this.getAttributeNames();
		if(atts.size() > 0 && atts.get(0).equals("waitRTIOrder")) Federate.instance.onAddEntity(this);
						
		//Till here
		
		// If necessary, wake up the conveyor
		this.restart();
	}

	@Override
	protected boolean startProcessing(double simTime) {
		return !entryList.isEmpty();
	}

	@Override
	protected void processStep(double simTime) {

		// Remove the first entity from the conveyor and send it to the next component
		ConveyorEntry entry = entryList.remove(0);
		DisplayEntity ent = entry.entity;
		this.sendToNextComponent(ent);

		// Remove any other entities that have also reached the end
		double maxPos = Math.min(entry.position, 1.0d);
		while (!entryList.isEmpty() && entryList.get(0).position >= maxPos) {
			ent = entryList.remove(0).entity;
			this.sendToNextComponent(ent);
		}

		// Update the travel time
		this.updateTravelTime(simTime);
	}

	@Override
	protected double getStepDuration(double simTime) {

		// Calculate the time for the first entity to reach the end of the conveyor
		double dt = simTime - this.getLastUpdateTime();
		double dur = (1.0d - entryList.get(0).position)*presentTravelTime - dt;
		dur = Math.max(dur, 0);  // Round-off to the nearest tick can cause a negative value
		if (isTraceFlag()) trace(1, "getProcessingTime = %.6f", dur);
		return dur;
	}

	@Override
	public void updateProgress(double dt) {

		if (presentTravelTime == 0.0d)
			return;

		// Calculate the fractional distance travelled since the last update
		double frac = dt/presentTravelTime;
		if (MathUtils.near(frac, 0.0d))
			return;

		// Increment the positions of the entities on the conveyor
		if (isTraceFlag()) traceLine(2, "BEFORE - entryList=%s", entryList);
		for (ConveyorEntry entry : entryList) {
			entry.position += frac;
		}
		if (isTraceFlag()) traceLine(2, "AFTER - entryList=%s", entryList);
	}

	private void updateTravelTime(double simTime) {

		// Has the travel time changed?
		double newTime = travelTimeInput.getValue().getNextSample(simTime);
		if (newTime != presentTravelTime) {

			if (isTraceFlag()) {
				trace(1, "updateTravelTime");
				traceLine(2, "newTime=%.6f, presentTravelTime=%.6f", newTime, presentTravelTime);
			}

			// Set the new travel time
			presentTravelTime = newTime;

			// Adjust the time at which the next entity will reach the end of the conveyor
			// (required when an entity is added to a conveyor that already has entities in flight)
			this.resetProcess();
		}
	}

	@Override
	public void thresholdChanged() {
		if (isImmediateReleaseThresholdClosure()) {
			for (ConveyorEntry entry : entryList) {
				entry.position = 1.0d;
			}
		}
		super.thresholdChanged();
	}

	// ********************************************************************************************
	// GRAPHICS
	// ********************************************************************************************

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == colorInput || in == widthInput) {
			if (GUIFrame.getInstance() == null)
				return;
			GUIFrame.getInstance().updateLineButtons();
			return;
		}
	}

	public PolylineModel getPolylineModel() {
		DisplayModel dm = getDisplayModel();
		if (dm instanceof PolylineModel)
			return (PolylineModel) dm;
		return null;
	}

	@Override
	public boolean isOutlined() {
		return true;
	}

	@Override
	public int getLineWidth() {
		PolylineModel model = getPolylineModel();
		if (widthInput.isDefault() && model != null)
			return model.getLineWidth();
		return widthInput.getValue();
	}

	@Override
	public Color4d getLineColour() {
		PolylineModel model = getPolylineModel();
		if (colorInput.isDefault() && model != null)
			return model.getLineColour();
		return colorInput.getValue();
	}

	@Override
	public void updateGraphics(double simTime) {

		if (presentTravelTime == 0.0d || !usePointsInput())
			return;

		// Move each entity on the conveyor to its present position
		double frac = 0.0d;
		if (isBusy()) {
			frac = (simTime - this.getLastUpdateTime())/presentTravelTime;
		}
		for (int i=0; i<entryList.size(); i++) {
			ConveyorEntry entry = entryList.get(i);
			Vec3d localPos = PolylineInfo.getPositionOnPolyline(getCurvePoints(), entry.position + frac);
			entry.entity.setGlobalPosition(this.getGlobalPosition(localPos));
		}
	}

	// LinkDisplayable overrides
	@Override
	public Vec3d getSourcePoint() {
		ArrayList<Vec3d> points = pointsInput.getValue();
		if (points.size() == 0) {
			return getGlobalPosition();
		}
		return new Vec3d(points.get(points.size()-1));
	}
	@Override
	public Vec3d getSinkPoint() {
		ArrayList<Vec3d> points = pointsInput.getValue();
		if (points.size() == 0) {
			return getGlobalPosition();
		}
		return new Vec3d(points.get(0));
	}

	@Override
	public double getRadius() {
		return 0.2; // TODO: make this a tunable parameter
	}

}
