/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/
package edu.usc.polygraph.initial_data;



import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edu.usc.polygraph.DBState;
import edu.usc.polygraph.Utilities;
import edu.usc.polygraph.ValidationParams;



public class YCSBLoader  {

	public static final int LOAD_FIELD_LENGTH = 10;
	public static final int FIELD_LENGTH = 20;
	static int RECORD_COUNT=1000;




	
	

	public static HashMap<String, List<DBState>> generateInitialState(int scaleFactor) {
		return loadEntities(scaleFactor);
	}

	public static HashMap<String, List<DBState>> loadEntities(int scaleFactor) {
		HashMap<String, List<DBState>> initState = new HashMap<String, List<DBState>>();
		Random myGen = new Random(1444665544549L);
		int records = Math.round(RECORD_COUNT * scaleFactor);
		for (int i = 0; i < records; i++) {
			String eKey = Utilities.concatWithSeperator(ValidationParams.KEY_SEPERATOR, ValidationParams.USER_ENTITY, String.valueOf(i));
			String[] values = new String[10];
			for (int j = 0; j < 10; j++) {
				values[j] = TextGenerator.randomStr(myGen, LOAD_FIELD_LENGTH);
			}
			DBState st = new DBState(0, values);
			LinkedList<DBState> ll =new LinkedList<DBState>();
			ll.add(st);
			initState.put(eKey, ll);
		} // FOR
		return initState;
	}
}
