package edu.usc.polygraph.codegenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.usc.polygraph.Entity;
import edu.usc.polygraph.PolygraphUISettings;
import edu.usc.polygraph.Property;

public class CodeGeneratorFunctions {

	private static String nl = System.getProperty("line.separator");
	private static int variableCount = 0;

	public static HashMap<Transaction, HashSet<String>> buildHashmap(ArrayList<Transaction> trans) throws Exception {

		HashMap<Transaction, HashSet<String>> transHash = new HashMap<Transaction, HashSet<String>>();
		for (Transaction t : trans) {
			HashSet<String> info = new HashSet<String>();
			transHash.put(t, info);
			for (Entity e : t.entities) {

				for (Property p : e.getProperties()) {
					char type = '*';
					switch (p.getType()) {
					case PolygraphUISettings.NEW_VALUE_UPDATE:
					case PolygraphUISettings.VALUE_DELETED:
					case PolygraphUISettings.DECREMENT_UPDATE_INTERFACE:
					case PolygraphUISettings.INCREMENT_UPDATE:
						type = PolygraphUISettings.UPDATE_RECORD;
						break;
					case PolygraphUISettings.VALUE_READ:
					case PolygraphUISettings.VALUE_NA:
						type = PolygraphUISettings.READ_RECORD;
						break;
					}
					if (type == '*') {
						throw new Exception("Error:Unrecognized property type");
					}
					String key = e.getName() + PolygraphUISettings.KEY_SEPERATOR + p.getName() + PolygraphUISettings.KEY_SEPERATOR + type;
					info.add(key);

				}
			}
		}

		return transHash;

	}

	public static void getTopics(ArrayList<Transaction> trans, String topic) {
		HashSet<Transaction> visited = new HashSet<Transaction>();
		int count = 1;
		for (Transaction t : trans) {
			if (t.topic == null) {
				t.topic = topic + "-" + count;
				count++;

			}
			search(t, visited);
		}

	}

	public static String readFile(String filename) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
			result = sb.toString();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void search(Transaction t, HashSet<Transaction> visited) {
		if (!visited.contains(t)) {
			visited.add(t);
			if (t.conflicted == null) {

				return;
			}
			for (ConflictedTransaction nextTrans : t.conflicted) {
				if (!visited.contains(nextTrans.transaction)) {

					nextTrans.transaction.topic = t.topic;
					search(nextTrans.transaction, visited);
				}
			}
		}
	}

	public static ArrayList<Transaction> parseTrans(ArrayList<String> jsonStrings, String erFile) {
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		for (String jsonString : jsonStrings) {

			JSONObject obj = new JSONObject(jsonString);// readFile
			String tname = obj.getString("Name");
			// System.out.println(tname);
			JSONArray elements = obj.getJSONArray("Elements");

			Transaction trans = new Transaction(tname);
			trans.JSON = jsonString;
			transactions.add(trans);
			trans.entities = new ArrayList<Entity>();
			trans.relationships = new ArrayList<String>();
			trans.propsVars = new HashMap<String, String>();

			for (int i = 0; i < elements.length(); i++) {
				JSONObject element = (JSONObject) elements.get(i);
				String pre = "E-";
				String setType = element.getString("elementType");
				int eid = element.getInt("eid");
				if (setType.equals("Relationship")) {
					pre = "R-";
					trans.relationships.add(String.valueOf(eid));
				}
				JSONArray sets = element.getJSONArray("sets");

				for (int j = 0; j < sets.length(); j++) {

					JSONObject set = (JSONObject) sets.get(j);
					// String setType= set.getString("type");
					JSONArray pks = set.getJSONArray("pks");
					for (int p = 0; p < pks.length(); p++) {
						JSONObject pk = (JSONObject) pks.get(p);
						int pid = pk.getInt("pid");
						String varName = pk.getString("variable");
						trans.propsVars.put(j + "-" + pre + eid + "-" + pid, varName);
					}
					Property[] properties = null;
					String type = set.getString("type");
					if (type.equals("DELETE")) {
						JSONArray eORrArray = null;
						JSONObject erJSON = new JSONObject(erFile);
						if (setType.equals("Entity")) {
							eORrArray = erJSON.getJSONArray("Entities");
							JSONArray propsArrER = eORrArray.getJSONObject(eid).getJSONArray("properties");
							ArrayList<PObject> propsArr = new ArrayList<PObject>();
							for (int p = 0; p < propsArrER.length(); p++) {
								JSONObject prop = (JSONObject) propsArrER.get(p);
								if (!prop.getBoolean("pk")) {
									PObject pObj = new PObject(p, "");
									pObj.info.put("name", prop.getString("name"));
									pObj.info.put("variable", "v" + p);
									propsArr.add(pObj);
								}
							}
							properties = new Property[propsArr.size()];
							for (int p = 0; p < propsArr.size(); p++) {
								PObject prop = propsArr.get(p);
								int pid = prop.id;
								String varName = "";
								try {
									varName = prop.info.get("variable");
								} catch (Exception e) {
									System.out.println();
								}
								trans.propsVars.put(j + "-" + pre + eid + "-" + pid, varName);
								char propertyType = 'D';
								properties[p] = new Property(String.valueOf(pid), varName, propertyType);
							}

						} else if (setType.equals("Relationship")) {
							eORrArray = erJSON.getJSONArray("Relationships");
							JSONArray propsArrER = eORrArray.getJSONObject(eid).getJSONArray("properties");
							ArrayList<PObject> propsArr = new ArrayList<PObject>();
							for (int p = 0; p < propsArrER.length(); p++) {
								JSONObject prop = (JSONObject) propsArrER.get(p);
								if (!prop.getBoolean("pk")) {
									PObject pObj = new PObject(p, "");
									pObj.info.put("name", prop.getString("name"));
									pObj.info.put("variable", "v" + p);
									propsArr.add(pObj);
								}
							}
							properties = new Property[propsArr.size()];
							for (int p = 0; p < propsArr.size(); p++) {
								PObject prop = propsArr.get(p);
								int pid = prop.id;
								String varName = "";
								try {
									varName = prop.info.get("variable");
								} catch (Exception e) {
									System.out.println();
								}
								trans.propsVars.put(j + "-" + pre + eid + "-" + pid, varName);
								char propertyType = 'D';
								properties[p] = new Property(String.valueOf(pid), varName, propertyType);
							}
						}

					} else {
						JSONArray propsArr = set.getJSONArray("properties");
						properties = new Property[propsArr.length()];
						for (int p = 0; p < propsArr.length(); p++) {
							JSONObject prop = (JSONObject) propsArr.get(p);
							int pid = prop.getInt("pid");
							String varName = "";
							try {
								varName = prop.getString("variable");
							} catch (Exception e) {
								System.out.println();
							}
							trans.propsVars.put(j + "-" + pre + eid + "-" + pid, varName);
							char propertyType = prop.getString("type").charAt(0);
							properties[p] = new Property(String.valueOf(pid), varName, propertyType);
						}
					}
					Entity e = new Entity(null, String.valueOf(pre + eid), properties);
					trans.entities.add(e);

				} // end set loop

				// }
				// else if
				// (element.getString("elementType").equals("Relationship")){

				// }
			} // elements loop

		} // json file loop
		return transactions;

	}

	public static void parseEr(String jsonString, HashMap<String, Entity> entities, HashMap<String, Property> props) {
		JSONObject obj = new JSONObject(jsonString);// readFile(jsonString)
		JSONArray elements = obj.getJSONArray("Entities");

		int entityCount = 0;
		int relationCount = 0;
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("type").equals("Entity")) {

				String eid = "E-" + entityCount;
				entityCount++;
				String ename = element.getString("name");
				String ekey = "";
				JSONArray propsArr = element.getJSONArray("properties");
				for (int j = 0; j < propsArr.length(); j++) {
					JSONObject prop = (JSONObject) propsArr.get(j);
					boolean pk = prop.getBoolean("pk");
					int pid = j;
					String pname = prop.getString("name");
					String ptype = prop.getString("type");

					if (pk) {
						ekey = ekey + pid + "-";
					}
					Property p = new Property(pname, ptype, 'A');
					props.put(eid + "-" + pid, p);

				}
				// System.out.println(ename + " ... ekey: " + ekey);
				ekey = ekey.substring(0, ekey.length() - 1); // remove last char
				Entity e = new Entity(ekey, ename, null);
				entities.put(String.valueOf(eid), e);

			}
		}

		elements = obj.getJSONArray("Relationships");

		entityCount = 0;
		relationCount = 0;
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("type").equals("Relationship")) {

				String eid = "R-" + entityCount;
				entityCount++;
				String ename = element.getString("name");
				String ekey = "";
				JSONArray propsArr = element.getJSONArray("properties");
				for (int j = 0; j < propsArr.length(); j++) {
					JSONObject prop = (JSONObject) propsArr.get(j);
					boolean pk = prop.getBoolean("pk");
					int pid = j;
					String pname = prop.getString("name");
					String ptype = prop.getString("type");

					if (pk) {
						ekey = ekey + pid + "-";
					}
					Property p = new Property(pname, ptype, 'A');
					props.put(eid + "-" + pid, p);

				}
				if (ekey.isEmpty()) {
					ekey = null;
				} else
					ekey = ekey.substring(0, ekey.length() - 1); // remove last
																	// char
				Entity e = new Entity(ekey, ename, null);
				entities.put(String.valueOf(eid), e);

			}
		}

	}

	public static void removeUnconflictProps(ArrayList<Transaction> trans, HashMap<Transaction, HashSet<String>> hashTrans) throws Exception {

		for (int tIndex = trans.size() - 1; tIndex >= 0; tIndex--) {
			Transaction t = trans.get(tIndex);

			for (int eIndex = t.entities.size() - 1; eIndex >= 0; eIndex--) {
				Entity e = t.entities.get(eIndex);

				Property[] propsArray = e.getProperties();
				// if (propsArray.length==0){
				// t.entities.remove(eIndex);
				//
				// }
				if (propsArray == null)
					continue;
				for (int pIndex = propsArray.length - 1; pIndex >= 0; pIndex--) {
					Property p = e.getProperties()[pIndex];
					char type = '*';
					switch (p.getType()) {
					case PolygraphUISettings.NEW_VALUE_UPDATE:
					case PolygraphUISettings.VALUE_DELETED:
					case PolygraphUISettings.DECREMENT_UPDATE_INTERFACE:
					case PolygraphUISettings.INCREMENT_UPDATE:
						type = PolygraphUISettings.UPDATE_RECORD;
						break;
					case PolygraphUISettings.VALUE_READ:
					case PolygraphUISettings.VALUE_NA:
						type = PolygraphUISettings.READ_RECORD;
						break;
					}
					if (type == '*') {
						throw new Exception("Error:Unrecognized property type");
					}

					String key = e.getName() + PolygraphUISettings.KEY_SEPERATOR + p.getName();
					boolean r = buildConflict(t, hashTrans, key, type);
					if (!r) {
						// propsArray[pIndex] = propsArray[propsArray.length - 1];
						// e.setProperties(Arrays.copyOfRange(propsArray, 0, propsArray.length - 1));
						// propsArray = e.getProperties();
					}
					if (propsArray.length == 0) {
						// t.entities.remove(eIndex);
						break;
					}
				} // property loop

				if (t.entities.size() == 0) {
					trans.remove(tIndex);
					break;
				}
			} // entity loop

		} // trans loop

	}

	private static boolean buildConflict(Transaction t, HashMap<Transaction, HashSet<String>> hashTrans, String key, char type) {
		boolean result = false;
		if (type == PolygraphUISettings.READ_RECORD)
			type = PolygraphUISettings.UPDATE_RECORD;
		else
			type = PolygraphUISettings.READ_RECORD;

		String searchKey = key + PolygraphUISettings.KEY_SEPERATOR + type;

		for (Transaction trans : hashTrans.keySet()) {
			HashSet<String> h = hashTrans.get(trans);
			if (h.contains(searchKey)) {
				result = true;
				if (t.conflicted == null)
					t.conflicted = new ArrayList<ConflictedTransaction>();

				// if (!t.name.equals(trans.name)){
				t.conflicted.add(new ConflictedTransaction(trans, key));
				// }
			}
		}

		return result;
	}

	public static ArrayList<String> getCodeSnippet(String erFile, ArrayList<String> transFiles, String application) {

		HashMap<String, Entity> entities = new HashMap<String, Entity>();
		HashMap<String, Property> props = new HashMap<String, Property>();
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<String> name = new ArrayList<String>();
		try {

			ArrayList<Transaction> transactions2 = prepareDTs(erFile, transFiles, entities, props, application);
			for (Transaction t : transactions2) {
				HashMap<String, PObject> mEntities = getEntities(erFile, t.JSON);
				t.mEntities = mEntities;
			}
			HashMap<Transaction, HashSet<String>> transactionsMap = buildHashmap(transactions2);
			removeUnconflictProps2(transactions2, transactionsMap);
			for (Transaction t : transactions2) {
				// System.out.println(t);
				String entityArrName = "transEntities";
				// MR1
				String entityCode = generateEntitiesCode2(entityArrName, t);
				String logRecordCode = generateLogRecordCode2(entityArrName, t);
				String startCode = generateStartLogRecordCode2(entityArrName, t);

				result.add(startCode + "\n" + entityCode + "\n" + logRecordCode);
				name.add(t.name);
			}
			ArrayList<String> processedTopics = new ArrayList<String>();
			int numOfProcessedTransactions = 0;
			String currentTopic = "";
			while (numOfProcessedTransactions < transactions2.size()) {
				HashMap<String, PObject> topicEntities = new HashMap<String, PObject>();
				for (Transaction t : transactions2) {
					if (currentTopic.equals("") && (!processedTopics.contains(t.topic))) {
						currentTopic = t.topic;
						processedTopics.add(t.topic);
					}
					if (currentTopic.equals(t.topic)) {
						numOfProcessedTransactions++;
						for (String key : t.mEntities.keySet()) {
							if (topicEntities.containsKey(key)) {
								PObject obj = topicEntities.get(key);
								if (t.mEntities.get(key).info.get("opType").equals("INSERT")) {
									if (obj.info.get("insert") == null) {
										obj.info.put("insert", "\"" + t.name + "\"");
									} else {
										obj.info.put("insert", obj.info.get("insert") + ",\"" + t.name + "\"");
									}
								}
								HashMap<String, PObject> tProps = t.mEntities.get(key).props;
								for (PObject p : tProps.values()) {
									if (!obj.props.containsKey(p.id)) {
										obj.props.put(p.id + "", p);// TODO: check
									}
								}
							} else {
								PObject obj = new PObject(t.mEntities.get(key).id, t.mEntities.get(key).type);
								obj.info.put("name", t.mEntities.get(key).info.get("name"));
								// System.out.println(t.mEntities.get(key).info.get("opType"));
								if (t.mEntities.get(key).info.get("opType").equals("INSERT"))
									obj.info.put("insert", "\"" + t.name + "\"");
								HashMap<String, PObject> tProps = t.mEntities.get(key).props;
								for (PObject p : tProps.values()) {
									obj.props.put(p.id + "", p);// TODO: check
								}
								topicEntities.put(key, obj);
							}
						}
					}
				}
				result.add(writeJSONFile(topicEntities, currentTopic));
				// name.add(currentTopic.replace("_", ""));
				name.add(currentTopic);
				currentTopic = "";
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			System.exit(0);
		}
		result.addAll(name);
		return result;
	}

	private static void removeUnconflictProps2(ArrayList<Transaction> trans, HashMap<Transaction, HashSet<String>> transactionsMap) {
		HashMap<String, Boolean> allPropertiesConflicts = new HashMap<String, Boolean>();
		for (Transaction t : trans) {
			HashSet<String> tMap = transactionsMap.get(t);
			for (String s : tMap) {
				allPropertiesConflicts.put(s, false);
			}
		}
		Set<String> keys = allPropertiesConflicts.keySet();
		for (String key : keys) {
			if (!allPropertiesConflicts.get(key)) {
				String AntiKey;
				switch (key.charAt(key.length() - 1)) {
				case 'R':
					AntiKey = key.substring(0, key.length() - 1) + 'U';
					if (keys.contains(AntiKey)) {
						allPropertiesConflicts.put(key, true);
						allPropertiesConflicts.put(AntiKey, true);
					}
					break;
				case 'U':
					AntiKey = key.substring(0, key.length() - 1) + 'R';
					if (keys.contains(AntiKey)) {
						allPropertiesConflicts.put(key, true);
						allPropertiesConflicts.put(AntiKey, true);
					}
					break;
				default:
					System.out.println("ERROR: Incorrect type " + key.charAt(key.length() - 1));
					System.exit(0);
				}
			}
		}
		for (String key : keys) {
			if (!allPropertiesConflicts.get(key)) {
				for (Transaction t : trans) {
					HashSet<String> tMap = transactionsMap.get(t);
					if (tMap.remove(key)) {
						String[] tokens = key.split("[-]");
						PObject obj = t.mEntities.get(tokens[0] + "-" + tokens[1]);
						int pCount = 0;
						while (obj.props.remove(tokens[2] + "-" + pCount) != null) {
							obj.props.remove(tokens[2] + "-" + pCount);
							pCount++;
						}
					}
				}
			}
		}
	}

	private static HashMap<String, PObject> getEntities(String er, String ti) {
		HashMap<String, PObject> mEntities = new HashMap<String, PObject>();

		JSONObject erObj = new JSONObject(er);
		JSONObject tiObj = new JSONObject(ti);

		JSONArray erEntities = erObj.getJSONArray("Entities");
		JSONArray erRelationships = erObj.getJSONArray("Relationships");
		JSONArray tiEntities = tiObj.getJSONArray("Elements");

		for (int i = 0; i < tiEntities.length(); i++) {
			JSONArray sets = ((JSONObject) tiEntities.get(i)).getJSONArray("sets");
			int eid = ((JSONObject) tiEntities.get(i)).getInt("eid");
			String elementType = ((JSONObject) tiEntities.get(i)).getString("elementType");
			JSONObject objFromEr = null;
			switch (elementType) {
			case "Entity":
				objFromEr = (JSONObject) erEntities.get(eid);
				break;
			case "Relationship":
				objFromEr = (JSONObject) erRelationships.get(eid);
				break;
			}

			for (int j = 0; j < sets.length(); j++) {
				JSONObject objJSON = ((JSONObject) sets.get(j));
				PObject obj = new PObject(eid, elementType);
				obj.info.put("name", replaceInvalidChar(objFromEr.getString("name")));
				obj.info.put("setType", objFromEr.getString("type"));
				obj.info.put("opType", objJSON.getString("type"));
				obj.info.put("single", String.valueOf(objJSON.getBoolean("single")));
				obj.info.put("stop", objJSON.getString("stop"));
				mEntities.put(elementType.charAt(0) + "-" + eid, obj);

				JSONArray pks = objJSON.getJSONArray("pks");
				for (int k = 0; k < pks.length(); k++) {
					int pid = ((JSONObject) pks.get(k)).getInt("pid");
					JSONObject pFromEr = (JSONObject) objFromEr.getJSONArray("properties").get(pid);
					PObject p = new PObject(pid, "");
					p.info.put("name", replaceInvalidChar(pFromEr.getString("name")));
					p.info.put("type", pFromEr.getString("type"));
					p.info.put("variable", ((JSONObject) pks.get(k)).getString("variable"));
					obj.pks.put(pid, p);
				}

				JSONArray props = objJSON.getJSONArray("properties");
				for (int k = 0; k < props.length(); k++) {
					int pid = ((JSONObject) props.get(k)).getInt("pid");
					JSONObject pFromEr = (JSONObject) objFromEr.getJSONArray("properties").get(pid);
					PObject p = new PObject(pid, "");
					p.info.put("name", replaceInvalidChar(pFromEr.getString("name")));
					p.info.put("type", pFromEr.getString("type"));
					p.info.put("variable", ((JSONObject) props.get(k)).getString("variable"));
					p.info.put("opType", ((JSONObject) props.get(k)).getString("type"));
					int pcount = 0;
					while (obj.props.get(pid + "-" + pcount) != null) {
						pcount++;
					}
					obj.props.put(pid + "-" + pcount, p);
					// obj.props.add(p);
				}

				if (obj.info.get("opType").equals("DELETE")) {
					boolean done = false;
					while (!done) {
						JSONObject prop = (JSONObject) objFromEr.getJSONArray("properties").get(i);
						if(!prop.getBoolean("pk")){
							done = true;
							int pid = i;
							PObject p = new PObject(pid, "");
							p.info.put("name", replaceInvalidChar(prop.getString("name")));
							p.info.put("type", prop.getString("type"));
							p.info.put("variable", "NULL");
							p.info.put("opType", "D");
							int pcount = 0;
							while (obj.props.get(pid + "-" + pcount) != null) {
								pcount++;
							}
							obj.props.put(pid + "-" + pcount, p);
						} else {
							i++;
						}
					}
				}
			}
		}
		return mEntities;
	}

	public static List<Integer> computePartitioningAttribute(String ER, String Ti) {
		// ER =
		// CodeGeneratorFunctions.readFile("/home/mr1/JSON_Tests/test.txt");
		// Ti =
		// CodeGeneratorFunctions.readFile("/home/mr1/JSON_Tests/test_T1.txt");
		List<Graph<JSONObject>> gs = Graph.buildTreeGraph(ER, Ti);
		List<Integer> PA = new ArrayList<Integer>();
		if (gs != null) {
			int i = 0;
			for (Graph<JSONObject> g : gs) {
				boolean result = g.validateTree();
				if (result) {
					if (g.isOneOccurance(g.getRoot())) {
//						System.out.println("gs[" + i + "] is a tree, with root " + g.getRoot().name);
						PA.add(g.getRoot().id);
					} else {
//						System.out.println("gs[" + i + "] is a tree, but its root (" + g.getRoot().name + ") was refrenced multiple times.");
					}
				} else {
//					System.out.println("g[" + i + "] is not a tree");
				}
//				System.out.println("----------------");
				i++;
			}
		} else {
//			System.out.println("No trees were constructed.");
		}
		return PA;
	}

	public static int convertStringToInt(String str) {
		int result = 0;
		for (int i = 0; i < str.length(); i++) {
			result += str.charAt(i);
		}
		return result;
	}

	public static int getPartitionKey(List<Integer> PAs) {
		int result = 0;
		for (int i = 0; i < PAs.size(); i++) {
			result += PAs.get(i) * Math.pow(10, PAs.size() - 1 - i);
		}
		return result;
	}

	public static void main(String[] args) {
		/*
		 * List<Integer> a = new ArrayList<Integer>(); a.add(1);a.add(10);a.add(12);a.add(31);a.add(15);a.add( convertStringToInt("test")); System.out.println(a.toString() + ": " +
		 * getPartitionInt(a)); List<Integer> b = new ArrayList<Integer>(); b.add(1);b.add(10);b.add(12);b.add(31); List<Integer> c = new ArrayList<Integer>();
		 * c.add(13);c.add(10);c.add(12);c.add(31);c.add(1); List<Integer> d = new ArrayList<Integer>(); d.add(1);d.add(4); List<Integer> e = new ArrayList<Integer>(); e.add(5);e.add(10);e.add(1);
		 * List<List<Integer>> all = new ArrayList<List<Integer>>(); all.add(a);all.add(b);all.add(c);all.add(d);all.add(e); List<Integer> v = getIntersection(all); System.out.println(v.toString());
		 */
		String application = "test";

		String linuxPath = "/home/mr1/JSON_Tests/ycsb/";// "/home/mr1/TATP/";
		String winPath = "C:/Users/MR1/Dropbox/PhD/Polygraph/files/";
		// String erFile = readFile("/home/mr1/TATP/TATP.txt");

		ArrayList<String> transFiles = new ArrayList<String>();

		// transFiles.add(readFile(linuxPath+"TATP_GetSub.txt"));
		// transFiles.add(readFile(linuxPath+"TATP_UpdateLocation.txt"));
		// transFiles.add(readFile(linuxPath+"TATP_UpdateSub.txt"));
		//
		// String erFile = readFile(linuxPath + "3Topics.txt");
		// transFiles.add(readFile(linuxPath + "3Topics_TPCC_NO.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_TPCC_PA.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_TPCC_DE.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_TPCC_OS.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_BG_AF1.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_BG_AF2.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_BG_TF1.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_BG_TF2.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_BG_VP.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_SEATS_FindFlight.txt"));
		// transFiles.add(readFile(linuxPath + "3Topics_SEATS_NR.txt"));
		// transFiles.add(readFile("/home/mr1/tpccApp/TPC-C_Delivery.txt"));
		//
		// String erFile = readFile(linuxPath + "TPC-C.txt");
		// transFiles.add(readFile(linuxPath + "TPC-C_New-Order.txt"));
		// transFiles.add(readFile(linuxPath + "TPC-C_Payment.txt"));
		// transFiles.add(readFile(linuxPath + "TPC-C_Delivery.txt"));
		// transFiles.add(readFile(linuxPath + "TPC-C_Order-Status.txt"));

		String erFile = readFile(linuxPath + "YCSB.txt");
		transFiles.add(readFile(linuxPath + "YCSB_Read.txt"));
		transFiles.add(readFile(linuxPath + "YCSB_Update.txt"));
		transFiles.add(readFile(linuxPath + "YCSB_Insert.txt"));
		transFiles.add(readFile(linuxPath + "YCSB_ReadModifyWrite.txt"));
		transFiles.add(readFile(linuxPath + "YCSB_Delete.txt"));

		ArrayList<String> results = getCodeSnippet(erFile, transFiles, application);
		System.out.println(results);
		// HashMap<String, Entity> entities= new HashMap<String,Entity>();
		// HashMap<String, Property> props= new HashMap<String,Property>();
		//
		//
		// try {
		//
		// String application="BG";
		// String erFile="/home/mr1/yazApp/yazApp.txt";
		// ArrayList<String> transFiles= new ArrayList<String>();
		// transFiles.add("/home/mr1/yazApp/yazApp_t1.txt");
		// transFiles.add("/home/mr1/yazApp/yazApp_t2.txt");
		// transFiles.add("/home/mr1/yazApp/yazApp_t3.txt");
		// transFiles.add("/home/mr1/yazApp/yazApp_t4.txt");
		//
		// ArrayList<Transaction>
		// transactions2=prepareDTs(erFile,transFiles,entities,props,application);
		// for (Transaction t:transactions2){
		// System.out.println(t);
		// String entityArrName="transEntities";
		// String
		// entityCode=generateEntitiesCode(entityArrName,t,entities,props);
		// int numPartitions=1;
		// String
		// logRecordCode=generateLogRecordCode(entityArrName,t,numPartitions);
		// String startCode=generateStartLogRecordCode(entityArrName, t,
		// numPartitions);
		// System.out.println("Entity Code:");
		// System.out.println(entityCode);
		// System.out.println("Start Code:");
		// System.out.println(startCode);
		// System.out.println("Log Code:");
		// System.out.println(logRecordCode);
		// System.out.println("======================================");
		//
		// }

		System.exit(0);

		// } catch (Exception ex) {
		// ex.printStackTrace();
		// System.exit(0);
		// }

		// Transaction t1= new Transaction("t1");
		//
		// t1.entities= new ArrayList<Entity>();
		// Property[] properties= new Property[2];
		// properties[0]= new Property("p1", null,
		// PolygraphUISettings.NEW_VALUE_UPDATE);
		// properties[1]= new Property("p1", null,
		// PolygraphUISettings.VALUE_READ);
		//
		// Entity member= new Entity(null, "MEMBER", properties);
		// t1.entities.add(member);
		//
		//
		// Transaction t2= new Transaction("t2");
		//
		//
		// t2.entities= new ArrayList<Entity>();
		// Property[] properties2= new Property[2];
		// properties2[0]= new Property("p1", null,
		// PolygraphUISettings.NEW_VALUE_UPDATE);
		// properties2[1]= new Property("p2", null,
		// PolygraphUISettings.VALUE_READ);
		//
		// Entity member2= new Entity(null, "MEMBER", properties2);
		// t2.entities.add(member2);
		//

		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		Transaction a = new Transaction("ViewProfile");
		a.entities = new ArrayList<Entity>();
		Property[] aproperties = new Property[1];
		aproperties[0] = new Property("fcount", null, PolygraphUISettings.VALUE_READ);
		// aproperties[1]= new Property("pcount", null,
		// PolygraphUISettings.VALUE_READ);

		Entity ae1 = new Entity(null, "member", aproperties);
		// Entity ae2= new Entity(null, "member", aproperties);

		a.entities.add(ae1);

		Transaction b = new Transaction("acceptFriend");
		b.entities = new ArrayList<Entity>();
		Property[] bproperties = new Property[1];
		bproperties[0] = new Property("fcount", null, PolygraphUISettings.INCREMENT_UPDATE);
		// bproperties[1]= new Property("pcount", null,
		// PolygraphUISettings.INCREMENT_UPDATE);

		Entity be1 = new Entity(null, "member", bproperties);
		// Property[] bproperties2= new Property[1];
		// bproperties2[0]= new Property("fcount", null,
		// PolygraphUISettings.INCREMENT_UPDATE);
		// Entity be2= new Entity(null, "member2", bproperties);
		b.entities.add(be1);
		// b.entities.add(be2);
		// Property[] bproperties2= new Property[1];
		// bproperties2[0]= new Property("e2", null,
		// PolygraphUISettings.VALUE_READ);
		// Entity be2= new Entity(null, "e2", bproperties2);
		// b.entities.add(be2);

		Transaction c = new Transaction("acceptFriend2");
		c.entities = new ArrayList<Entity>();
		Property[] cproperties = new Property[2];
		cproperties[0] = new Property("pcount", null, PolygraphUISettings.INCREMENT_UPDATE);
		cproperties[1] = new Property("pcount", null, PolygraphUISettings.VALUE_READ);

		Entity ce1 = new Entity(null, "member", cproperties);

		c.entities.add(ce1);

		Transaction d = new Transaction("acceptFriend3");
		d.entities = new ArrayList<Entity>();
		Property[] dproperties = new Property[2];
		dproperties[0] = new Property("pcount", null, PolygraphUISettings.INCREMENT_UPDATE);
		dproperties[1] = new Property("fcount", null, PolygraphUISettings.VALUE_READ);

		Entity de1 = new Entity(null, "member", dproperties);

		d.entities.add(de1);

		// Transaction c= new Transaction("c");
		// c.entities= new ArrayList<Entity>();
		// Property[] cproperties= new Property[1];
		// cproperties[0]= new Property("e3", null,
		// PolygraphUISettings.VALUE_READ);
		// Entity ce3= new Entity(null, "e3", cproperties);
		// c.entities.add(ce3);
		// Property[] cproperties2= new Property[1];
		// cproperties2[0]= new Property("e4", null,
		// PolygraphUISettings.NEW_VALUE_UPDATE);
		// Entity ce4= new Entity(null, "e4", cproperties2);
		// c.entities.add(ce4);
		//
		// Transaction d= new Transaction("d");
		// d.entities= new ArrayList<Entity>();
		// Property[] dproperties= new Property[1];
		// dproperties[0]= new Property("e4", null,
		// PolygraphUISettings.VALUE_READ);
		// Entity de4= new Entity(null, "e4", dproperties);
		// d.entities.add(de4);
		// Property[] dproperties2= new Property[1];
		// dproperties2[0]= new Property("e5", null,
		// PolygraphUISettings.NEW_VALUE_UPDATE);
		// Entity de5= new Entity(null, "e5", dproperties2);
		// d.entities.add(de5);
		//
		//
		// Transaction e= new Transaction("e");
		// e.entities= new ArrayList<Entity>();
		// Property[] eproperties= new Property[1];
		// eproperties[0]= new Property("e5", null,
		// PolygraphUISettings.NEW_VALUE_UPDATE);
		// Entity ee5= new Entity(null, "e5", eproperties);
		// e.entities.add(ee5);

		transactions.add(a);
		transactions.add(b);
		transactions.add(c);
		transactions.add(d);
		// transactions.add(e);

		HashMap<Transaction, HashSet<String>> transactionsMap = null;
		try {
			transactionsMap = buildHashmap(transactions);
			removeUnconflictProps(transactions, transactionsMap);
			getTopics(transactions, "MEMBER");
			// Graph<String> g = Graph.buildGraph("/home/mr1/app1.txt");
			// setKeys(transactions,g);
			for (Transaction t : transactions) {
				System.out.println(t);
				System.out.println("======================================");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(0);
		}

	}

	// private static String generateLogRecordCode(String entityArrName, Transaction t) {
	// t.topic = replaceInvalidChar(t.topic);
	// StringBuilder sb = new StringBuilder();
	//
	// sb.append("String logRecord=getLogRecordString('" + t.type + "', \"" + t.name + "\", String.valueOf(tid), startTime, endTime, " + entityArrName + ");" + nl);
	// // producer.send(new ProducerRecord<String, String>(tempTopic,
	// // partition, currentRecord.getId(), value));
	// // int partition = (key % numPartitions) + numPartitions;
	// // if (t.type != PolygraphUISettings.READ_RECORD)
	// // partition = (key % numPartitions) + (3 * numPartitions);
	// // sb.append("int numPartitionsE="+numPartitions+";"+nl);
	// if (t.type == PolygraphUISettings.READ_RECORD) {
	// sb.append("int partitionEnd=(" + t.key + "%numPartitions_" + t.topic + ")+numPartitions_" + t.topic + ";" + nl);
	// } else if (t.type != PolygraphUISettings.READ_RECORD) {
	// sb.append("int partitionEnd=(" + t.key + "%numPartitions_" + t.topic + ")+(3*numPartitions_" + t.topic + ");" + nl);
	//
	// }
	// sb.append("producer.send(new ProducerRecord<String, String>(\"" + t.topic + "\",partitionEnd, String.valueOf(tid), logRecord));" + nl);
	// sb.append("//********************************************************************//" + nl);
	//
	// return sb.toString();
	// }

	// private static String generateStartLogRecordCode(String entityArrName, Transaction t) {
	// t.topic = replaceInvalidChar(t.topic);
	// String nl = System.getProperty("line.separator");
	// StringBuilder sb = new StringBuilder();
	// sb.append("/*" + nl + "* Place this code segment before the start of the transaction." + nl + "* Set the value of tid to uniquely identify a transaction." + nl);
	// sb.append("/*" + nl + "* Set the number of partitions in numPartitions_" + t.topic + " constant" + nl);
	// sb.append("/*" + nl + "* Create topic " + t.topic + " with numPartitions_" + t.topic + "*4 partitions" + nl);
	// sb.append("*/" + nl);
	// sb.append("//********************************************************************//" + nl);
	//
	// sb.append("long startTime=System.nanoTime();" + nl);
	//
	// // sb.append("String logRecord=getLogRecordString("+t.type+",
	// // "+t.name+", recordKey, startTime, endTime, "+entityArrName+");"+nl);
	// // producer.send(new ProducerRecord<String, String>(tempTopic,
	// // partition, currentRecord.getId(), value));
	// // int partition = (key % numPartitions) + numPartitions;
	// // if (t.type != PolygraphUISettings.READ_RECORD)
	// // partition = (key % numPartitions) + (2 * numPartitions);
	// // sb.append("int numPartitionsS="+numPartitions+";"+nl);
	// if (t.type == PolygraphUISettings.READ_RECORD) {
	// sb.append("int partitionStart=(" + t.key + "%numPartitions_" + t.topic + ");" + nl);
	// } else if (t.type != PolygraphUISettings.READ_RECORD) {
	// sb.append("int partitionStart=(" + t.key + "%+numPartitions_" + t.topic + ")+(2*numPartitions_" + t.topic + ");" + nl);
	//
	// }
	// sb.append("producer.send(new ProducerRecord<String, String>(\"" + t.topic + "\",partitionStart, String.valueOf(tid), Long.toString(startTime)));" + nl);
	// sb.append("//********************************************************************//" + nl);
	// return sb.toString();
	// }

	private static String generateEntitiesCode(String entitiesArrName, Transaction t, HashMap<String, Entity> entities, HashMap<String, Property> props) {
		t.topic = replaceInvalidChar(t.topic);
		int count = 1;
		String nl = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("/*" + nl + "* Place this code segment after commit of the transaction." + nl + "* Set the value of tid to uniquely identify a transaction (same as the code segment before the start of the transaction)." + nl);
		sb.append("*/" + nl);

		sb.append("//********************************************************************//" + nl);
		ArrayList<Entity> transEntities = new ArrayList<Entity>();

		sb.append("ArrayList<Entity> " + entitiesArrName + "= new ArrayList<Entity>();" + nl);
		int setsCount = 0;
		String prevEid = "-1";
		for (Entity e : t.entities) {
			if (prevEid.equals(e.getName())) {
				setsCount++;
			} else
				setsCount = 0;
			prevEid = e.getName();
			Entity tempE = entities.get(e.getName());
			if (tempE.getKey() == null)
				continue;
			String pkeys[] = tempE.getKey().split("-");
			String entityKey = "";

			for (String pkey : pkeys) {
				String pVar = "String.valueOf(" + t.propsVars.get(setsCount + "-" + e.getName() + "-" + pkey) + ")";
				entityKey = entityKey + pVar + "+\"" + PolygraphUISettings.KEY_SEPERATOR + "\"+";
			}

			entityKey = entityKey.substring(0, entityKey.length() - 5);
			String entityKeyName = tempE.getName() + "_key_" + (count++);
			entityKeyName = replaceInvalidChar(entityKeyName);

			sb.append("String " + entityKeyName + "=" + entityKey + ";" + nl);
			int l = e.getProperties().length;
			Property[] properties = new Property[l];
			String propsArrName = "props_" + tempE.getName() + "_" + (count++);
			propsArrName = replaceInvalidChar(propsArrName);
			sb.append("Property[] " + propsArrName + "=new Property[" + l + "];" + nl);
			for (int j = 0; j < e.getProperties().length; j++) {
				Property p = e.getProperties()[j];
				Property propTemp = props.get(e.getName() + "-" + p.getName());
				String pname = propTemp.getName();
				String value = p.getValue();// t.propsVars.get(setsCount+"-"+e.getName()+"-"+p.getName());
				char pType = p.getType();
				if (pType == PolygraphUISettings.DECREMENT_UPDATE_INTERFACE) {
					pType = PolygraphUISettings.INCREMENT_UPDATE;
					value = value + "* -1";
				}
				properties[j] = new Property(pname, value, pType);
				sb.append(propsArrName + "[" + j + "]= new Property(\"" + pname + "\",String.valueOf(" + value + "), '" + pType + "');" + nl);

			} // prop loop
			Entity mye = new Entity(entityKey, tempE.getName(), properties);
			String entityName = "e_" + tempE.getName() + "_" + (count++);
			entityName = replaceInvalidChar(entityName);
			sb.append("Entity " + entityName + "= new Entity(" + entityKeyName + ",\"" + replaceInvalidChar(tempE.getName()) + "\"," + propsArrName + ");" + nl);
			transEntities.add(mye);
			sb.append(entitiesArrName + ".add(" + entityName + ");" + nl);

		} // entity loop
		return sb.toString();
	}

	private static String replaceInvalidChar(String entityKeyName) {
		entityKeyName = entityKeyName.replaceAll("^\\d", "_");
		entityKeyName = entityKeyName.replaceAll("[^\\w]", "_");
		return entityKeyName;
	}

	private static ArrayList<Transaction> prepareDTs(String erFile, ArrayList<String> jsonStrings, HashMap<String, Entity> entities, HashMap<String, Property> props, String application) {
		parseEr(erFile, entities, props);
		ArrayList<Transaction> transactions = parseTrans(jsonStrings, erFile);
		HashMap<Transaction, HashSet<String>> transactionsMap = null;

		try {
			transactionsMap = buildHashmap(transactions);
			removeUnconflictProps(transactions, transactionsMap);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}

		getTopics(transactions, application);
		Graph<String> g = Graph.buildGraph(erFile);
		setKeys(transactions, g, entities, props, erFile);

		for (Transaction t : transactions)
			t.setType();
		return transactions;
	}

	private static void setKeys(ArrayList<Transaction> transactions, Graph<String> g, HashMap<String, Entity> entities, HashMap<String, Property> props, String erString) {
		HashMap<String, ArrayList<Transaction>> groups = new HashMap<String, ArrayList<Transaction>>();
		for (Transaction transaction : transactions) {
			if (!groups.containsKey(transaction.topic)) {

				ArrayList<Transaction> trans = new ArrayList<Transaction>();
				groups.put(transaction.topic, trans);
			}
			groups.get(transaction.topic).add(transaction);

		}

		for (String group : groups.keySet()) {
			ArrayList<Transaction> groupTrans = groups.get(group);
			// HashSet<String> allowed= new HashSet<String>();
			// HashSet<String> referencedEnities= new HashSet<String>();
			// getRefEnt_AllowedEdg(groupTrans,referencedEnities,allowed);
			// HashSet<String> res = g.getPartitioningKeys(referencedEnities,
			// allowed);
			// setKeysForGroup(groupTrans,res,entities,props);

			List<List<Integer>> listOfPA = new ArrayList<List<Integer>>();
			for (Transaction ti : groupTrans) {
				listOfPA.add(computePartitioningAttribute(erString, ti.JSON));
			}
			List<Integer> PAs = getIntersection(listOfPA);
			for (Transaction ti : groupTrans) {
				ti.key = PAs;
			}

		} // end group

	}

	private static List<Integer> getIntersection(List<List<Integer>> listOfPA) {
		List<Integer> PAs = new ArrayList<Integer>();
		if (listOfPA.size() > 0) {
			PAs = listOfPA.get(0);
			for (int i = 1; i < listOfPA.size(); i++) {
				for (int j = 0; j < PAs.size(); j++) {
					if (!listOfPA.get(i).contains(PAs.get(j))) {
						PAs.remove(j);
						j--;
					}
				}
			}
		}
		return PAs;
	}

	// private static void setKeysForGroup(ArrayList<Transaction> groupTrans,
	// HashSet<String> res, HashMap<String, Entity> entities, HashMap<String,
	// Property> props) {
	// String key = null;
	// HashSet<String> validKeys = new HashSet<String>();
	// if (res.isEmpty()) {
	// key = "1";
	// } else {
	// int count = 0;
	// boolean firstCkey = true;
	// for (String ckey : res) {
	// boolean validKey = true;
	// for (Transaction t : groupTrans) {
	//
	// Entity e = entities.get(ckey);
	//
	// String[] pks = e.getKey().split("-");
	// if (pks.length != 1) {
	// validKey = false;
	// break;
	// } else {
	// String pid = pks[0];
	// Property propTemp = props.get(ckey + "-" + pid);
	// if (propTemp.getValue().equals("Double") ||
	// propTemp.getValue().equals("Integer")) {
	// if (t.propsVars.get("1-" + ckey + "-" + pid) != null) {
	// validKey = false;
	// break;
	// }
	//
	// } else {
	// validKey = false;
	// break;
	// }
	//
	// }
	// } // trans loop
	//
	// if (validKey) {
	// validKeys.add(ckey);
	// }
	//
	// } // cKey loop
	// key = null;
	// if (validKeys.isEmpty())
	// key = "1";
	// for (String ckey : validKeys) {
	// boolean validKey = true;
	//
	// count++;
	//
	// if (count == validKeys.size() && key == null) {
	// // last key dont delete
	// key = ckey;
	// if (firstCkey) {
	// // do clean
	// for (Transaction transaction : groupTrans) {
	//
	// for (int i = transaction.entities.size() - 1; i >= 0; i--) {
	// Entity e = transaction.entities.get(i);
	// if (firstCkey) { // remove any empty entity that is not among the
	// candidate keys. We only need to do it once
	// if (e.getProperties().length == 0 && !validKeys.contains(e.getName())) {
	// transaction.entities.remove(i);
	// }
	// }
	// }
	// }
	//
	// }
	// break;
	// }
	//
	// for (Transaction transaction : groupTrans) {
	//
	// for (int i = transaction.entities.size() - 1; i >= 0; i--) {
	// Entity e = transaction.entities.get(i);
	// if (ckey.equals(e.getName())) {
	// if (e.getProperties().length == 0) {
	// validKey = false;
	// transaction.entities.remove(i);
	// }
	// }
	//
	// if (firstCkey) { // remove any empty entity that is not among the
	// candidate keys. We only need to do it once
	// if (e.getProperties().length == 0 && !validKeys.contains(e.getName())) {
	// transaction.entities.remove(i);
	// }
	// }
	// }
	// }
	// if (validKey) {
	// key = ckey;
	// }
	//
	// firstCkey = false;
	// } // vKey loop
	//
	// }
	//
	// if (!key.equals("1")) { // get variable
	//
	// for (Transaction t : groupTrans) {
	//
	// Entity e = entities.get(key);
	// String[] pks = e.getKey().split("-");
	//
	// String pid = pks[0];
	// t.key = t.propsVars.get("0-" + key + "-" + pid);
	//
	// } // trans loop
	// }
	// if (key.equals("1")) { // key="1"
	// for (Transaction transaction : groupTrans) {
	// transaction.key = key;
	// }
	// }
	//
	// }

	private static void getRefEnt_AllowedEdg(ArrayList<Transaction> groupTrans, HashSet<String> referencedEnities, HashSet<String> allowed) {
		for (Transaction trans : groupTrans) {
			allowed.addAll(trans.relationships);
			for (Entity e : trans.entities) {
				if (e.getName().contains("E-")) {
					String eid = e.getName().substring(e.getName().indexOf('-') + 1);
					referencedEnities.add(eid);
				}
			}
		}

	}

	private static HashSet<String> getUniqueEntitiesMap(Transaction trans) {
		HashSet<String> entitiesHash = new HashSet<String>();
		HashSet<String> notAllowedEntities = new HashSet<String>();

		for (Entity e : trans.entities) {
			if (entitiesHash.contains(e.getName())) {
				entitiesHash.remove(e.getName());
				notAllowedEntities.add(e.getName());
			}
			if (!notAllowedEntities.contains(e.getName())) {
				entitiesHash.add(e.getName());
			}
		}
		return entitiesHash;

	}

	private static String generateEntitiesCode2(String entitiesArrName, Transaction t) {//TODO: fdf
		t.topic = replaceInvalidChar(t.topic);
		String nl = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("/*" + nl + "* Place this code segment after commit of the transaction." + nl);
		sb.append("* Set the value of tid to uniquely identify a transaction." + nl);
		sb.append("* Create an int variable \"numPartitions_" + t.topic+"\" and set it with the desired number of partitions." + nl);
		sb.append("* Create topic " + t.topic.toUpperCase() + " with numPartitions_" + t.topic + "*2 partitions" + nl);
		sb.append("* Set the KAFKA_HOSTS variable in PolygraphHelper.java" + nl);

		sb.append("*/" + nl);

		sb.append("//********************************************************************//" + nl);

		sb.append("ArrayList<Entity> " + entitiesArrName + "= new ArrayList<Entity>();" + nl);
		boolean single = true;
		for (PObject e : t.mEntities.values()) {
			if (e.props.size() > 0) {
				single = Boolean.valueOf(e.info.get("single"));
				// String E1_key_1=String.valueOf(q);
				// Property[] props_E1_2=new Property[0];
				String tab = "";
				if (!single) {
					sb.append("for (int i = 0; " + e.info.get("stop").replaceAll(";", "i") + "; i++) {");
					sb.append(nl);
					tab = "\t";
				}

				String key_variable = e.info.get("name") + "_key_" + variableCount++;
				String key_value = "";
				String seperator = "";
				for (int key : e.pks.keySet()) {
					PObject p = e.pks.get(key);
					key_value += seperator;
					if (!p.info.get("type").equals("String")) {
						key_value += "String.valueOf(";
					}
					key_value += p.info.get("variable").replaceAll(";", "i");
					if (!p.info.get("type").equals("String")) {
						key_value += ")";
					}
					seperator = " + \"-\" + ";
				}
				sb.append(tab);
				sb.append("String ");
				sb.append(key_variable);
				sb.append(" = ");
				sb.append(key_value);
				sb.append(";");
				sb.append(nl);

				sb.append(tab);
				sb.append("Property[] ");
				String propArray_variable = "props_" + e.info.get("name") + "_" + variableCount++;
				sb.append(propArray_variable);
				sb.append(" = new Property[");
				sb.append(e.props.size());
				sb.append("];");
				sb.append(nl);

				int count = 0;
				for (String key : e.props.keySet()) {
					PObject p = e.props.get(key);
					sb.append(tab);
					sb.append(propArray_variable);
					sb.append(String.format("[%d] = new Property(\"%s\", ", count++, p.info.get("name")));
					char type = p.info.get("opType").charAt(0);
					if (type == PolygraphUISettings.DECREMENT_UPDATE_INTERFACE) {
						type = PolygraphUISettings.INCREMENT_UPDATE;
						sb.append("-1 * ");
					}
					if (p.info.get("type").equals("String")) {
						sb.append("PolygraphHelper.escapeCharacters(");
						sb.append(p.info.get("variable").replaceAll(";", "i"));
						sb.append(")");
					} else if (p.info.get("type").equals("Date")) {
						sb.append("PolygraphHelper.escapeCharacters(String.valueOf(");
						sb.append(p.info.get("variable").replaceAll(";", "i"));
						sb.append("))");
					} else {
						sb.append("String.valueOf(");
						sb.append(p.info.get("variable").replaceAll(";", "i"));
						sb.append(")");
					}
					sb.append(String.format(", '%c');", type));
					sb.append(nl);
				}

				// Entity e_E1_3= new Entity(E1_key_1,"E1",props_E1_2);
				sb.append(tab);
				sb.append("Entity ");
				String entity_variable = "e_" + e.info.get("name") + "_" + variableCount++;
				sb.append(entity_variable);
				sb.append(" = new Entity(");
				sb.append(key_variable);
				sb.append(", \"");
				sb.append(e.info.get("name"));
				sb.append("\", ");
				sb.append(propArray_variable);
				sb.append(");");
				sb.append(nl);

				// transEntities.add(e_E1_3);
				sb.append(tab);
				sb.append(entitiesArrName);
				sb.append(".add(");
				sb.append(entity_variable);
				sb.append(");");
				sb.append(nl);

				if (!single) {
					sb.append("}");
					sb.append(nl);
				}
			}
		}
		return sb.toString();
	}

	private static String generateStartLogRecordCode2(String entityArrName, Transaction t) {
		t.topic = replaceInvalidChar(t.topic);
		StringBuilder sb = new StringBuilder();
		sb.append("/*" + nl + "* Place this code segment before the start of the transaction." + nl);
		sb.append("*/" + nl);
		sb.append("//********************************************************************//" + nl);
		sb.append("long startTime=System.nanoTime();" + nl);
		sb.append("//********************************************************************//" + nl);
		return sb.toString();
	}

	private static String generateLogRecordCode2(String entityArrName, Transaction t) {
		t.topic = replaceInvalidChar(t.topic);
		String nl = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();

		sb.append("long endTime=System.nanoTime();" + nl);
		sb.append("String logRecord = PolygraphHelper.getLogRecordString('" + t.type + "', \"" + t.name + "\", String.valueOf(tid), startTime, endTime, " + entityArrName + ");" + nl);
		String key_variable = "key_" + variableCount++;
		String getKey = getTransactionKey(t, key_variable);
		if (getKey != null) {
			sb.append(getKey);
		} else {
			key_variable = "1";
		}
		if (t.type == PolygraphUISettings.READ_RECORD) {
			sb.append("int partition = (" + key_variable + " % PolygraphHelper.numPartitions_" + t.topic + ");" + nl);
		} else {
			sb.append("int partition = (" + key_variable + " % PolygraphHelper.numPartitions_" + t.topic + ") + PolygraphHelper.numPartitions_" + t.topic + ";" + nl);

		}
		sb.append("PolygraphHelper.kafkaProducer.send(new ProducerRecord<String, String>(\"" + t.topic.toUpperCase() + "\",partition, String.valueOf(tid), logRecord));" + nl);
		sb.append("//********************************************************************//" + nl);

		return sb.toString();
	}

	private static String getTransactionKey(Transaction t, String key_variable) {
		String result = "";
		String keys_array_variable = "keys_" + variableCount++;
		int count = 0;
		for (int i = 0; i < t.key.size(); i++) {
			PObject obj = t.mEntities.get("E-" + t.key.get(i));
			for (int key : obj.pks.keySet()) {
				PObject p = obj.pks.get(key);
				if (p.info.get("type").equals("String")) {
					result += String.format("%s[%d] = PolygraphHelper.convertStringToInt(%s);", keys_array_variable, count++, p.info.get("variable")) + nl;
				} else if (p.info.get("type").equals("Double")) {
					result += String.format("%s[%d] = (int) %s;", keys_array_variable, count++, p.info.get("variable")) + nl;
				} else if (p.info.get("type").equals("Date")) {
					result += String.format("%s[%d] = PolygraphHelper.convertStringToInt(%s.toString());", keys_array_variable, count++, p.info.get("variable")) + nl;
				} else {
					result += String.format("%s[%d] = %s;", keys_array_variable, count++, p.info.get("variable")) + nl;
				}
			}
		}
		if (count == 0)
			return null;
		result += String.format("int %s = PolygraphHelper.getPartitionKey(%s);", key_variable, keys_array_variable) + nl;
		result = String.format("int[] %s = new int[%d];", keys_array_variable, count) + nl + result;
		return result;
	}

	private static String writeJSONFile(HashMap<String, PObject> topicEntities, String currentTopic) {
		// {"application":{ "name":"BG",
		// "entities":[{"name":"MEMBER","properties":["FRIEND_CNT","PENDING_CNT"]}]
		// }}
		String result = "";

		result += "{\"application\":\n\t{\"name\":\"" + currentTopic.toUpperCase() + "\",\"entities\":[\n";
		String eComma = "";
		for (PObject obj : topicEntities.values()) {
			if (obj.props.size() == 0)
				continue;
			result += eComma + "\t\t{\"name\":\"" + obj.info.get("name") + "\",\"properties\":[";
			String pComma = "";
			for (PObject p : obj.props.values()) {
				result += pComma + "\"" + p.info.get("name") + "\"";
				pComma = ", ";
			}
			String insertTrans = "";
			if (obj.info.get("insert") != null) {
				insertTrans = String.format(",\"inserttrans\":[%s]", obj.info.get("insert"));
			}
			result += "]" + insertTrans + "}";

			eComma = ",\n";
		}

		result += "\n\t]}\n}";
		return result;
	}

}