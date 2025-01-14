/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;

public abstract class ArrayListInput<T> extends ListInput<ArrayList<T>> {

	public ArrayListInput(String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
	}

	@Override
	public int getListSize() {
		ArrayList<T> val = getValue();
		if (val == null)
			return 0;
		else
			return val.size();
	}

}
