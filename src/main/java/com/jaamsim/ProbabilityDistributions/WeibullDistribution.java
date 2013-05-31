/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.ProbabilityDistributions;

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;

/**
 * Weibull Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 452.
 */
public class WeibullDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Weibull distribution.",
	         example = "WeibullDist-1 Scale { 3.0 }")
	private final DoubleInput scaleInput;

	@Keyword(description = "The shape parameter for the Weibull distribution.  A decimal value > 0.0.  " +
			"Note: The CalculatedMean and CalculatedStandardDeviation outputs are valid only for shape = 1/N, 1, or 2.  " +
			"Other values for Shape are acceptable, but the CalculatedMean and CalculatedStandardDeviation outputs will be reported incorrectly as zero.",
	         example = "WeibullDist-1 Shape { 1.0 }")
	private final DoubleInput shapeInput;

	{
		scaleInput = new DoubleInput("Scale", "Key Inputs", 1.0d);
		scaleInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(scaleInput, true);

		shapeInput = new DoubleInput("Shape", "Key Inputs", 1.0d);
		shapeInput.setValidRange( 1.0e-10d, Double.POSITIVE_INFINITY);
		this.addInput(shapeInput, true);
	}

	@Override
	protected double getNextSample() {

		// Inverse transform method
		return  scaleInput.getValue() * Math.pow( - Math.log( randomGenerator1.nextDouble() ), 1.0/shapeInput.getValue() );
	}

	@Override
	protected double getMeanValue() {
		double shape = shapeInput.getValue();
		double scale = scaleInput.getValue();
		return scale / shape * this.gamma( 1.0 / shape );
	}

	@Override
	protected double getStandardDeviation() {
		double shape = shapeInput.getValue();
		double scale = scaleInput.getValue();
		return scale/shape * Math.sqrt( 2.0*shape*this.gamma(2.0/shape) - Math.pow( this.gamma(1.0/shape), 2.0 ) );
	}

	/**
	 * Gamma function.  A crude implementation for x = 0.5 or an integer.
	 * Needed for the getMeanValue() and getStandardDeviation() methods.
	 */
	private double gamma( double x ) {

		// Special case for x = 0.5
		if( x == 0.5 ) return Math.sqrt( Math.PI );

		// Only works for integer values
		int k = (int) x;
		if( Math.abs(x - k) > 1.0e-10  || ( x < 0.0 ) ) {
			return 0.0;
		}

		// Calculate k-1 factorial
		double ret = 1.0;
		for( int i=2; i<k; i++ ) {
			ret *= i;
		}
		return ret;
	}
}
