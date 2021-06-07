/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;

/**
 * Controls the execution of one or more runs of a given simulation model.
 * @author Harry King
 *
 */
public class RunManager implements RunListener {

	private final JaamSimModel simModel;
	private PrintStream outStream;  // location where the custom outputs will be written
	private int runNumber;    // labels each run when multiple runs are being made

	public RunManager(JaamSimModel sm) {
		simModel = sm;
	}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public void start(double pauseTime) {
		runNumber = simModel.getSimulation().getStartingRunNumber();
		simModel.setRunNumber(runNumber);
		simModel.start(pauseTime);
	}

	public void pause() {
		simModel.pause();
	}

	public void resume(double pauseTime) {
		simModel.resume(pauseTime);
	}

	public void reset() {
		if (outStream != null) {
			outStream.close();
			outStream = null;
		}
		runNumber = simModel.getSimulation().getStartingRunNumber();
		simModel.setRunNumber(runNumber);
		simModel.reset();
	}

	public void close() {
		if (outStream != null) {
			outStream.close();
			outStream = null;
		}
		simModel.closeLogFile();
		simModel.pause();
		simModel.close();
		simModel.clear();
	}

	@Override
	public void runEnded() {

		// Print the output report
		if (simModel.getSimulation().getPrintReport())
			InputAgent.printReport(simModel, EventManager.simSeconds());

		// Print the selected outputs
		if (simModel.getSimulation().getRunOutputList().getValue() != null) {
			PrintStream outStream = getOutStream();
			if (simModel.isFirstRun()) {
				InputAgent.printRunOutputHeaders(simModel, outStream);
			}
			InputAgent.printRunOutputs(simModel, outStream, EventManager.simSeconds());
		}

		if (simModel.isLastRun()) {
			return;
		}

		// Increment the run number
		runNumber++;
		simModel.setRunNumber(runNumber);
	}

	public PrintStream getOutStream() {
		if (outStream == null) {

			// Select either standard out or a file for the outputs
			outStream = System.out;
			if (!simModel.isScriptMode()) {
				String fileName = simModel.getReportFileName(".dat");
				if (fileName == null)
					throw new ErrorException("Cannot create the run output file");
				try {
					outStream = new PrintStream(fileName);
				}
				catch (FileNotFoundException e) {
					throw new InputErrorException(
							"FileNotFoundException thrown trying to open PrintStream: " + e );
				}
				catch (SecurityException e) {
					throw new InputErrorException(
							"SecurityException thrown trying to open PrintStream: " + e );
				}
			}
		}
		return outStream;
	}

}
