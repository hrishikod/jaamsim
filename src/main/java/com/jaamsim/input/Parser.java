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
package com.jaamsim.input;

import java.util.ArrayList;

public class Parser {

/**
 * Tokenize the given record and append to the given list of tokens
 *
 * Valid delimiter characters are space, tab and comma.
 *
 * @param tokens list of String tokens to append to
 * @param rec record to tokenize and append
 */
public static final void tokenize(ArrayList<String> tokens, String rec) {
	// Split the record into two pieces, the contents portion and possibly
	// a commented portion
	String[] contents = rec.split("\"", 2);

	// Split the contents along single-quoted substring boundaries to allow
	// us to parse quoted and unquoted sections separately
	String[] substring = contents[0].split("'", -1);
	for (int i = 0; i < substring.length; i++) {
		// Odd indices were single-quoted strings in the original record
		// restore the quotes and append the whole string as a single token
		// even if there was nothing between the quotes (an empty string)
		if (i % 2 != 0) {
			tokens.add(String.format("'%s'", substring[i]));
			continue;
		}

		// The even-index strings need tokenizing, we allow spaces, tabs and
		// commas to delimit token boundaries, we also want all braces {} to
		// appear as a single token
		// Ensure { or } has tabs separating if from adjacent characters
		String temp = substring[i].replaceAll("([\\{\\}])", "\t$1\t");
		// Split along space, comma and tab characters, treat consecutive
		// characters as one delimiter
		String[] delimTokens = temp.split("[ ,\t]+", 0);
		// Append the new tokens that have greater than zero length
		for (String each : delimTokens) {
			if (!each.isEmpty())
				tokens.add(each);
		}
	}

	// add any comments if they exist with a leading " prepended to denote it
	// as commented
	if (contents.length == 2)
		tokens.add(String.format("\"%s", contents[1]));
}
}
