package edu.usc.polygraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.usc.polygraph.website.Common;
import edu.usc.polygraph.website.PolygraphUISettings;


public class KafkaScripts {

	private static final String APPLICATION = "application";
	private static final String APPLICATION_NAME = "name";
	private static final String APPLICATION_ENTITIES = "entities";
	private static final String ENTITY_NAME = "name";
	private static final String ENTITY_PROPERTIES = "properties";
	private static final String ENTITY_INSERT_TRANSACTIONS = "inserttrans";
	private static final String ENTITY_TYPE_INSERT = "INSERT";
	private static final String ENTITY_TYPE_UPDATE = "UPDATE";
	private static final String ENTITY_TYPE_RW = "READ&WRITE";
	private static final String SPLIT_TRANSACTION_INFO = "[:]";
	private static final String SPLIT_ENTITY_INFO = "[,]";
	private static final String SPLIT_PROPERTY_INFO = "[;]";
	private static final int TNAME_INDEX = 1;
	private static final int ENAME_INDEX = 1;
	private static final int PNAME_INDEX = 1;
	private static final int P_PK_INDEX = 2;
	private static final int E_EID_INDEX = 0;
	private static final int TE_EID_INDEX = 2;
	private static final int INDEX_OF_FIRST_PROPERTY_IN_E = 2;
	private static final int INDEX_OF_FIRST_PROPERTY_IN_TE = 4;
	private static final int INDEX_OF_FIRST_ENTITY = 2;
	private static final int INDEX_OF_ENTITY_TYPE = 3;

	public static void deleteTopic(String topic) {
		topic = topic.toUpperCase();
		try {
			// bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic
			// MEMBER
			File dir = new File(PolygraphUISettings.KAFKA_FLDR);
			String cmd = dir.getCanonicalPath() + "/bin/kafka-topics.sh --zookeeper " + PolygraphUISettings.ZOOKEEPER_HOST + " --delete --topic " + topic;
			System.out.println("delete cmd: " + cmd);
			Runtime.getRuntime().exec(cmd);
			// Process p = new ProcessBuilder(cmd).start();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}

	public static boolean isTopicExist(String topic) {
		topic = topic.toUpperCase();

		// bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic
		// MEMBER
		File dir = new File(PolygraphUISettings.KAFKA_FLDR);
		try {
			String cmd = dir.getCanonicalPath() + "/bin/kafka-topics.sh --zookeeper " + PolygraphUISettings.ZOOKEEPER_HOST + " --list ";

			System.out.println("command: " + cmd);
			String r = executeRuntime(cmd, true);
			System.out.println("topics found: " + r);
			if (r.toLowerCase().contains(topic.toLowerCase())) {
				System.out.println("looking for " + topic.toLowerCase() + " ,,,, result = true");
				return true;
			}
			System.out.println("looking for " + topic.toLowerCase() + " ,,,, result = false");
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
		return false;

	}

	public static boolean isTopicExist(String topic, String zookeeper) {
		topic = topic.toUpperCase();
		File dir = new File(PolygraphUISettings.KAFKA_FLDR);
		try {
			String cmd = "/home/mr1/StaleMeter/kafkaServer" + "/bin/kafka-topics.sh --zookeeper " + zookeeper + " --list ";

			System.out.println("command: " + cmd);
			String r = executeRuntime(cmd, true);
			System.out.println("topics found: " + r);
			if (r.toLowerCase().contains(topic.toLowerCase())) {
				System.out.println("looking for " + topic.toLowerCase() + " ,,,, result = true");
				return true;
			}
			System.out.println("looking for " + topic.toLowerCase() + " ,,,, result = false");
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return false;

	}

	public static boolean isValidatorRunning(String app) {
		String r = executeRuntime("jps -m | grep \"ValidationMain\" | grep -ie \"-app " + app + "\"", true);
		System.out.println("Validators Running found: " + r);
		if (r.toLowerCase().contains(app.toLowerCase())) {
			System.out.println("looking for " + app.toLowerCase() + " ,,,, result = true");
			return true;
		}
		System.out.println("looking for " + app.toLowerCase() + " ,,,, result = false");
		return false;
	}

	public static void killValidator(String app) {
		executeRuntime("jps -m | grep \"ValidationMain\" | grep -ie \"-app " + app + "\" | cut -b1-6 | xargs -t kill -9", true);
	}

	public static String createTopic(String topic, int numPartitions) {
		String output = "";
		topic = topic.toUpperCase();

		try {
			// --create --zookeeper localhost:2181 --replication-factor 1
			// --partitions 4 --topic test
			String cmd = "sudo " + PolygraphUISettings.KAFKA_FLDR + "bin/kafka-topics.sh --zookeeper " + PolygraphUISettings.ZOOKEEPER_HOST + " --create --replication-factor 1 --partitions " + numPartitions + " --topic " + topic;
			Process p = Runtime.getRuntime().exec(cmd);
			Thread.sleep(1000);
			InputStream stdout = p.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.contains("already exists")) {
					output = "already exists";
					break;
				}
				output = output + line;
			}
			// Process p = new ProcessBuilder(cmd).start();
			p.waitFor();
			stdout.close();
			reader.close();

		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		// output is: Created topic "test".
		// or: already exists.

		return output;
	}

	public enum Result {
		Running, Failed;
	}

	public static void launchValidators(String topic, String JSONFilePath, int numValidators, int numPartitions, int clientID, int numClientServers, String ResultPath) {
		// [topic] [numV] [numP] [clientID] [numClients] [resultsPath]/results
		// [kafkaIP] [jsonFile]
		String cmd = String.format("/home/mr1/StaleMeter/validScript_web.sh %s %d %d %d %d %s %s %s", topic, numValidators, numPartitions, clientID, numClientServers, ResultPath, PolygraphUISettings.KAFKA_HOST.split(":")[0], JSONFilePath);
		Common.localCommand(true, cmd);
	}

	// public static Result startDemo(int userid, int appid, String topic) {
	// String ResultPath = String.format(PolygraphUISettings.WEBSITE_RESULT_PATH, userid, appid);
	// System.out.println(ResultPath);
	// File x = new File(ResultPath);
	// try {
	// ResultPath = x.getCanonicalPath();
	// } catch (IOException e1) {
	// e1.printStackTrace(System.out);
	// }
	// System.out.println(ResultPath);
	// Common.CreateDir(ResultPath);
	// String JSONFilePath = ResultPath + PolygraphUISettings.WEBSITE_JSON_FILE;
	// System.out.println(JSONFilePath);
	// MySQLResponse rs = MySQL.getCode(userid, appid);
	// if (rs != null && rs.status == MySQLStatus.OK) {
	// writeJSONFile(rs.msg, JSONFilePath, topic);
	//
	// if (KafkaScripts.isTopicExist(topic)) {
	// if (KafkaScripts.isValidatorRunning(topic)) {
	// return Result.Running;
	// } else {
	// KafkaScripts.launchValidators(topic, JSONFilePath,
	// PolygraphUISettings.WEBSITE_NUM_OF_VALIDATORS, PolygraphUISettings.numOfPartitions,
	// PolygraphUISettings.clientID, PolygraphUISettings.numOfClients, ResultPath);
	// return Result.Running;
	// }
	// } else {
	// KafkaScripts.killValidator(topic);
	// KafkaScripts.createTopic(topic, PolygraphUISettings.WEBSITE_NUM_OF_VALIDATORS * 4);
	// KafkaScripts.launchValidators(topic, JSONFilePath, PolygraphUISettings.WEBSITE_NUM_OF_VALIDATORS,
	// PolygraphUISettings.numOfPartitions, PolygraphUISettings.clientID,
	// PolygraphUISettings.numOfClients, ResultPath);
	// return Result.Running;
	// }
	//
	// } else {
	// return Result.Failed;
	// }
	// }

	public static String executeRuntime(String cmd, boolean wait) {
		Process p;

		StringBuilder sb = new StringBuilder();
		try {

			p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", cmd });
			if (wait) {
				InputStream stdout = p.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
				String line = "";
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				p.waitFor();
			} else
				Thread.sleep(5000);
		} catch (Exception e2) {
			e2.printStackTrace(System.out);
		}

		return sb.toString();

	}

	public static void writeJSONFile(String text, String filePath, String appName) {
		// {"application":{ "name":"BG",
		// "entities":[{"name":"MEMBER","properties":["FRIEND_CNT","PENDING_CNT"]}]
		// }}
		PrintWriter printWriter = null;
		try {
			System.out.println("filePath: " + filePath);
			File file = new File(filePath);
			file.createNewFile();
			printWriter = new PrintWriter(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.out);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}

		ArrayList<String> Elines = new ArrayList<String>();
		ArrayList<String> Tlines = new ArrayList<String>();
		String[] lines = text.split("[\n]");
		int stage = 1;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].equals("#")) {
				stage = 2;
			} else if (stage == 1) {
				Elines.add(lines[i]);
			} else {
				Tlines.add(lines[i]);
			}
		}

		printWriter.write("{\"" + APPLICATION + "\":\n\t{\"" + APPLICATION_NAME + "\":\"" + appName.toUpperCase() + "\",\"" + APPLICATION_ENTITIES + "\":[\n");
		for (int i = 0; i < Elines.size(); i++) {
			if (Elines.get(i) == null || Elines.get(i).equals(""))
				continue;
			printWriter.write("\t\t{\"" + ENTITY_NAME + "\":\"");
			String[] Etokens = Elines.get(i).split("[,]");
			printWriter.write(Etokens[ENAME_INDEX].toUpperCase() + "\",\"" + ENTITY_PROPERTIES + "\":[");
			for (int j = INDEX_OF_FIRST_PROPERTY_IN_E; j < Etokens.length; j++) {
				String[] Ptokens = Etokens[j].split(SPLIT_PROPERTY_INFO);
				if (Ptokens[P_PK_INDEX].equals("false")) {
					printWriter.write("\"" + Ptokens[PNAME_INDEX].toUpperCase() + "\"");
					if (j + 1 != Etokens.length) {
						printWriter.write(",");
					}
				}
			}
			String insertTrans = getInsertTrans(Tlines, Etokens[E_EID_INDEX], Etokens.length - INDEX_OF_FIRST_PROPERTY_IN_E);
			printWriter.write("]" + insertTrans + "}");
			if (i + 1 != Elines.size()) {
				printWriter.write(",\n");
			} else {
				printWriter.write("\n");
			}
		}
		printWriter.write("\t]}\n}");

		printWriter.close();
	}

	private static String getInsertTrans(ArrayList<String> Tlines, String eID, int numOfProp) {
		boolean isInserted = false;
		boolean isPartiallyUpdated = false;
		HashSet<String> insertTransactions = new HashSet<String>();
		for (int i = 0; i < Tlines.size(); i++) {
			String[] Ttokens = Tlines.get(i).split(SPLIT_TRANSACTION_INFO);
			for (int j = INDEX_OF_FIRST_ENTITY; j < Ttokens.length; j++) {
				String[] Etokens = Ttokens[j].split(SPLIT_ENTITY_INFO);
				if (eID.equals(Etokens[TE_EID_INDEX])) {
					switch (Etokens[INDEX_OF_ENTITY_TYPE]) {
					case ENTITY_TYPE_INSERT:
						isInserted = true;
						insertTransactions.add(Ttokens[TNAME_INDEX]);
						break;
					case ENTITY_TYPE_UPDATE:
						int pNum = Etokens.length - INDEX_OF_FIRST_PROPERTY_IN_TE;
						if (numOfProp > pNum)
							isPartiallyUpdated = true;
						break;
					case ENTITY_TYPE_RW:
						break;
					}
				}
			}
			// 1,e1,1,READ,1;e1_p2;2;true;Integer;;v11;,2;e1_p4;4;true;Boolean;;v12;,4;e1_p1;1;false;String;R;v13;,
		}
		String result = "";
		if (isInserted && isPartiallyUpdated) {
			result = ",\"" + ENTITY_INSERT_TRANSACTIONS + "\":[";
			String comma = "";
			for (String t : insertTransactions) {
				result += comma + "\"" + t + "\"";
				comma = ",";
			}
			result += "]";
		}
		return result;
	}

	public static String getStatsJSON(StatsConsumer consumerStale, StatsConsumer consumerStats) {
		// consumerStats.updateStats();
		String stats=consumerStats.statsToJSON();
		if (stats==null)
			return null;
		String result = "{\"stats\" : " + stats;
		// consumerStale.updateStats();
		result += ",\"stales\" : " + consumerStale.staleToJSON() + "}";
		System.out.println(result);
		return result;
	}

	public static void main(String[] args) {
		String topic = "TPCCSTALE";
		String kafka = "10.0.0.205:9092";
		StatsConsumer consumerStale = new StatsConsumer(topic, topic, 1, kafka);
		HashMap<String, ArrayList<StaleLog>> hash = consumerStale.eStaleLog;
		StaleLog staleStaleLog = null;//hash.get(key).get(index);
		ArrayList<LogRecord> result = KafkaScripts.getForTheFirstTime(staleStaleLog, topic, kafka);
		LogRecord stale = LogRecord.createLogRecord("S,OS,46-102,1826791360892756,1826791364491979,CUS;1-10-1155;BALANCE:-10.0:R#YTD_P:10.0:R#P_CNT:1:R&ORD;1-10-1155-3097;OL_CNT:14:R#CARRID:0:R#OL_DEL_D:0:R&CUS*ORD;1-10-1155;CUS:CUS-1-10-1155:R#ORD:ORD-1-10-1155-3097:R");
		ArrayList<LogRecord> al = getMore(-708, 5074, stale, topic, kafka, 0, 1);
		System.out.println(al);
		/*
		 * String topic = "STATS_BGSTALE"; String topic2 = "BGSTALE"; System.out.println("%%%% topic = " + topic); StatsConsumer consumerStale = new StatsConsumer(topic, topic, 1,
		 * PolygraphUISettings.KAFKA_HOST); StatsConsumer consumerREADS = new StatsConsumer(topic2, topic2, 0, PolygraphUISettings.KAFKA_HOST); StatsConsumer consumerWRITES = new StatsConsumer(topic2,
		 * topic2, 1, PolygraphUISettings.KAFKA_HOST); // StatsConsumer consumerStats = new StatsConsumer(topic, topic, 0, PolygraphUISettings.KAFKA_HOST); getStatsJSON(consumerStale, consumerStale);
		 * // String result = KafkaScripts.getStatsJSON(consumerStale, consumerStats); StaleLog s = consumerStale.getOneLog(); ArrayList<LogRecord> al = new ArrayList<LogRecord>(); LogRecord stale =
		 * null; switch (s.type) { case 'R': stale = consumerREADS.getStaleLog(s); break; case 'Z': stale = consumerWRITES.getStaleLog(s); break; default: break; } stale.setType('S'); al.add(stale);
		 * long rOffset = s.lastReadOffset - PolygraphUISettings.numOfLogsBeforeLast; if (rOffset < 0) rOffset = 0; System.out.println("BR : al.size() = " + al.size());
		 * al.addAll(consumerREADS.getLogs(rOffset, stale.getId(), stale.getEndTime())); System.out.println("AR : al.size() = " + al.size()); long wOffset = s.lastWriteOffset -
		 * PolygraphUISettings.numOfLogsBeforeLast; if (wOffset < 0) wOffset = 0; System.out.println("BW : al.size() = " + al.size()); al.addAll(consumerWRITES.getLogs(wOffset, stale.getId(),
		 * stale.getEndTime())); System.out.println("AW : al.size() = " + al.size()); removeUnwanted(al, stale); System.out.println("ARU: al.size() = " + al.size()); for (int i = 0; i < 10; i++) {
		 * al.addAll(getMore(rOffset - (i * PolygraphUISettings.numOfLogsBeforeLast), wOffset - (i * PolygraphUISettings.numOfLogsBeforeLast), stale, topic2, PolygraphUISettings.KAFKA_HOST,
		 * s.readPartition, s.writePartition)); removeUnwanted(al, stale); System.out.println(i + "  : al.size() = " + al.size()); } // System.out.println(ArrayListToJSON(al));
		 * 
		 */
	}

	public static ArrayList<LogRecord> getForTheFirstTime(StaleLog s, String topic, String kafka) {
		StatsConsumer consumerREADS = new StatsConsumer(topic, topic, s.readPartition, kafka);
		StatsConsumer consumerWRITES = new StatsConsumer(topic, topic, s.writePartition, kafka);
		ArrayList<LogRecord> al = new ArrayList<LogRecord>();
		LogRecord stale = null;
		switch (s.type) {
		case 'R':
			stale = consumerREADS.getStaleLog(s);
			break;
		case 'Z':
			stale = consumerWRITES.getStaleLog(s);
			break;
		default:
			break;
		}
		stale.setType('S');
		
		al.add(stale);
		long offset = s.lastReadOffset - PolygraphUISettings.numOfLogsBeforeLast;
		if (offset < 0)
			offset = 0;
		al.addAll(consumerREADS.getLogs(offset, stale.getId(), stale.getEndTime()));
		offset = s.lastWriteOffset - PolygraphUISettings.numOfLogsBeforeLast;
		if (offset < 0)
			offset = 0;
		al.addAll(consumerWRITES.getLogs(offset, stale.getId(), stale.getEndTime()));
		// System.out.println("al.size() = " + al.size());
		removeUnwanted(al, stale);
		// System.out.println("al.size() = " + al.size());
		return al;
	}
	


	public static void removeUnwanted(ArrayList<LogRecord> al, LogRecord stale) {
		for (int i = 0; i < al.size(); i++) {
			if (!al.get(i).intersect(stale) || al.get(i).getActionName().equalsIgnoreCase("scan")) {
				al.remove(i--);
			}
		}
	}

	public static ArrayList<LogRecord> getMore(long rOffset, long wOffset, LogRecord stale, String topic, String kafka, int readPartition, int writePartition) {
		StatsConsumer consumerREADS = new StatsConsumer(topic, topic, readPartition, kafka);
		StatsConsumer consumerWRITES = new StatsConsumer(topic, topic, writePartition, kafka);
		ArrayList<LogRecord> al = new ArrayList<LogRecord>();
		try {
			al.addAll(consumerREADS.getLogs(rOffset, stale.getEndTime()));
			al.addAll(consumerWRITES.getLogs(wOffset, stale.getEndTime()));
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace(System.out);
			System.exit(0);
		}
		return al;
	}

	public static StaleLog getStaleLog(String topic, String kafka) {
		StatsConsumer consumerStale = new StatsConsumer(topic, topic, 1, kafka);
		StaleLog s = consumerStale.getOneLog();
		return s;
	}

	public static String ArrayListToJSON(ArrayList<LogRecord> results, ArrayList<StaleLog> staleAL) {
		String json = "{\"items\": [";
		for (int i = 0; (i < results.size()); i++) {
			System.out.println("ID:"+results.get(i).getId()+", "+results.get(i).getType());
			
			json += String.format("{\"id\" : \"%s\", \"start\":%d, \"end\":%d, \"entities\" : [", results.get(i).getId(), results.get(i).getStartTime(), results.get(i).getEndTime());
			String eComma = "";
			for (Entity e : results.get(i).getEntities()) {
				json += eComma + String.format("{\"name\" : \"%s\", \"properties\" : [", e.getEntityKey());
				String pComma = "";
				for (Property p : e.getProperties()) {
					String expected = "";
					if (results.get(i).getType() == 'S') {
						for (StaleLog s : staleAL) {
							if (s.tid.equals(results.get(i).getId())) {
								HashSet<String> al = s.expected.get(p.getProprtyKey(e));
								if(p.getType() == 'R'){
								if (al == null) {
									expected = "Same";
								} else {
									expected = al.toString();
								}
								}
								break;
							}
						}
					}
					expected=expected.replace("\\", "\\\\").replace("\"", "\\\"").replace("/", "\\/");

					p.setValue(p.getValue().replace("\\", "\\\\").replace("\"", "\\\"").replace("/", "\\/"));
					
					json += pComma + String.format("{\"name\" : \"%s\", \"type\" : \"%s\", \"value\" : \"%s\", \"expected\" : \"%s\"}", p.getName(), p.getType(), p.getValue(), expected);

					pComma = ",";
				}
				eComma = ",";
				json += "]}";
			}
			json += "],";
			json += String.format("\"className\" : \"%s\"}", (results.get(i).getType() == 'R' ? "Read" : (results.get(i).getType() == 'S' ? "Stale" : "Write")));
			if (i + 1 != results.size())
				json += ",";
		}
		json += "]}";
		System.out.println("json = " + json);
		return json;

	}
}
