package edu.usc.polygraph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.TopicPartition;

import edu.usc.polygraph.utils.MyObject;
import edu.usc.polygraph.utils.UtilConstants;


public class staleLogger {

	private static int startTimeIndex = 3;
	private static BufferedWriter bufferWriter = null;
	private static LogRecord currentRead = null;
	private static int divCounter;
	private static int imageCounter;
	private static final String fileTemplate = "stale_";
	private static final String traceDir = "." + ValidationParams.DIR_SEPERATOR + "staleTraceFiles" + ValidationParams.DIR_SEPERATOR;
	private static final String ImagesFolder = "images";
	private static final String EXTRA_FOLDER = "extra";
	private static final String CSS_FILE = "style.css";
	private static final String donnotIntersectWithStaleColor = "#232323";
	private static final String donnotIntersectWithStaleLightColor = "#D3D3D3";
	private static final String staleReadLogRecordColor = "#FF0000";
	private static final String staleReadLogRecordLightColor = "#FFDFDF";
	private static final String intersectReadWithStaleColor = "#009000";
	private static final String intersectReadWithStaleLightColor = "#BFFFBF";
	private static final String intersectWriteWithStaleColor = "#3561AD";
	private static final String intersectWriteWithStaleLightColor = "#BFD7FF";
	private static final String JS_FILE = "script.js";
	private static final String tempTraces = traceDir + "tempTraces" + ValidationParams.DIR_SEPERATOR;
	private static final String ImagesDirForJava = traceDir + ImagesFolder + ValidationParams.DIR_SEPERATOR;
	private static final String ImagesDirForHTML = "." + ValidationParams.DIR_SEPERATOR + ImagesFolder + ValidationParams.DIR_SEPERATOR;
	private static final String EXTRA_ForJava = traceDir + EXTRA_FOLDER + ValidationParams.DIR_SEPERATOR;
	private static final String EXTRA_ForHTML = "." + ValidationParams.DIR_SEPERATOR + EXTRA_FOLDER + ValidationParams.DIR_SEPERATOR;
	private static final String CSS_FILE_ForJava = EXTRA_ForJava + CSS_FILE;
	private static final String CSS_FILE_ForHTML = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + EXTRA_ForHTML + CSS_FILE + "\">";
	private static final String JS_FILE_ForJava = EXTRA_ForJava + JS_FILE;
	private static final String JS_FILE_ForHTML = "<script src=\"" + EXTRA_ForHTML + JS_FILE + "\"></script>";
	private static final String showHideJavaScriptFunction = "function showHide(currentDiv, b){var div = document.getElementById(currentDiv);if(div.style.display !== 'none'){div.style.display = 'none';b.innerHTML='+';}else{div.style.display = 'block';b.innerHTML='-';}}";
	private static final String showRecordJavaScriptFunction = "function showRecord(id, str){document.getElementById(id).innerHTML=str;}";
	private static final String[] entitiesColors = { "background-color: #DAEEFF;", "background-color: #CDFFC0;", "background-color: #FAB3FF;", "background-color: #F7DD94;", "background-color: #FFA0A0;" };
	public static int readStartOffset, readEndOffset,updateStartOffset, updateEndOffset;
	public static void loggingStale(Validator v,String logDir,LogRecord record, ScheduleList validationSS, HashMap<Schedule, HashMap<String, String>> expectedValues, long updateOffset) {
		
	
		currentRead = record;
		divCounter = 1;
		imageCounter = 1;
		checkDirectory(traceDir);
		checkDirectory(tempTraces);
		checkDirectory(ImagesDirForJava);
		checkDirectory(EXTRA_ForJava);
		checkFile(CSS_FILE_ForJava, CSS_FILE);
		checkFile(JS_FILE_ForJava, JS_FILE);
		if(ValidationParams.USE_KAFKA){
			getLogsForRead(record,Validator.kafkaLogDir,updateOffset,v);
		}
//		creatCurrentReadFile(v.staleCounter);
		ArrayList<LogRecord> intervals = getAllIntervals(logDir,record, validationSS);
//		intervals.remove(0);
//		intervals.remove(0);
//		intervals.remove(0);

//		adjustIntervals(intervals,record,true,100000000);
//		writeIntervalsFromTo(v.staleCounter,"intervals", intervals);
//		printScheduleList(v.dbState,"Serial Schedules", record, validationSS, expectedValues, intervals);
		// ScheduleList SS = ValidationMain.computeSerialSchedule4(intervals, 0, null, null);
		// printScheduleList("Serial Schedules", record, SS, expectedValues);
		closeCurrentReadNumber();
		// System.exit(0);
	}

	private static void getLogsForRead(LogRecord record, String logDir, long updateOffset,Validator v) {
		String id="R"+record.getId();
		int thresholdSeconds=30;
		long startTime= record.getStartTime()-(thresholdSeconds*10^9);
		long endTime= record.getEndTime()+(thresholdSeconds*10^9);

		LogConsumer cRead= new LogConsumer(id, logDir+ValidationParams.DIR_SEPERATOR+id, new TopicPartition(Validator.application, record.getPartitionID()+Validator.numPartitions), startTime, endTime, record.getOffset());
		
		 id="U"+record.getId();
		LogConsumer cUpdate= new LogConsumer(id, logDir+ValidationParams.DIR_SEPERATOR+id, new TopicPartition(Validator.application, record.getPartitionID()+3*Validator.numPartitions), startTime, endTime, updateOffset);
		ExecutorService exec= Executors.newFixedThreadPool(2);
		ArrayList<Callable<Object>> tasks= new ArrayList<Callable<Object>>(2);
		tasks.add(cRead);
		tasks.add(cUpdate);
		
		try {
			exec.invokeAll(tasks);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	private static boolean checkDirectory(String newDir) {
		File dir = new File(newDir);
		boolean b = dir.mkdir();
	//	if (Validator.staleCounter == 1) {
			try {
				FileUtils.cleanDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace(System.out);
				System.exit(0);
			}
		//}
		return b;
	}

	@SuppressWarnings("unused")
	private static void creatCurrentReadFile(int staleCounter) {
		try {
			File ufile = new File(traceDir + fileTemplate + staleCounter + ".html");
			FileWriter ufstream = new FileWriter(ufile);
			bufferWriter = new BufferedWriter(ufstream);

			bufferWriter.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
			bufferWriter.write("<html><head><title>" + staleCounter + " - Read record (" + currentRead.getId() + ")</title>" + CSS_FILE_ForHTML + JS_FILE_ForHTML + "</head><body style=\"font-family: arial; font-size: 14px;\"><table width=\"100%\">"  + "<td valign=\"top\">");

		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}

	@SuppressWarnings("unused")
	private static String buttonString(int staleCounter) {
		return "<tr><td colspan=\"2\" style=\"height: 200px;\" align=\"center\" valign=\"top\"><div style=\"position: fixed;\"><table frame=\"box\" id=\"pre_next_buttons_table\" width=\"800px\" bgcolor=\"white\"><tr><td align=\"center\"><button onclick=\"location.href='" + fileTemplate + (staleCounter - 1) + ".html';\" style=\"height: 50px;width: 150px;\">Previous<br />Read</button></td><td>" + getReadRecordTableString(currentRead) + "</td><td align=\"center\"><button onclick=\"location.href='" + fileTemplate
				+ (staleCounter + 1) + ".html';\" style=\"height: 50px;width: 150px;\">Next<br />Read</button></td></tr></table></div>"/*<script>var w = (screen.width - 100) + 'px';document.getElementById(\"pre_next_buttons_table\").setAttribute(\"width\", w);</script>*/+"</td></tr>";
	}
	@SuppressWarnings("unused")
	private static String getReadRecordTableString(LogRecord record) {
		String recordsString = "<center><table border=1>";
		recordsString += "<tr><th colspan=4>" + getNameString(record) + "</th></tr>";
		for (Entity e : record.getEntities()) {
			recordsString += "<tr class=\"" + e.getName() + "\"><td rowspan=" + e.getProperties().length + ">" + e.getName() + "<br />" + e.getKey() + "</td>";
			boolean firstOne = true;
			for (Property p : e.getProperties()) {
				if (firstOne) {
					firstOne = false;
				} else {
					recordsString += "<tr class=\"" + e.getName() + "\">";
				}
				recordsString += "<td>" + p.getName() + "</td><td>" + p.getValue() + "</td><td>" + p.getType() + "</td></tr>";
			}
		}
		recordsString += "</table></center>";
		return recordsString;
	}

	public static void printScheduleList(HashMap<String, LinkedList<DBState>> dbState,String name, LogRecord readRecord, ScheduleList SS, HashMap<Schedule, HashMap<String, String>> AllExpectedValues, ArrayList<LogRecord> intervals) {
		try {
			bufferWriter.write("<div class=\"mainBlock\"><table width=\"100%\"><tr><td><table width=\"100%\" border=\"1\"><tr><th><center><div class=\"header\">" + name + "</div></center></th></tr>");

			String schedule = "";
			if (SS.schedules.size() > 0) {
				ArrayList<String> diffInImpacted = getDiffInImpacted(SS);
				for (int Si = 0; Si < SS.schedules.size(); Si++) {
					Schedule s = SS.schedules.get(Si);
					HashMap<String, String> expectedValues = AllExpectedValues.get(s);
					schedule = "<tr><td>";
					HashSet<String> eNames = new HashSet<String>();
					Entity[] entities = new Entity[currentRead.getEntities().length + diffInImpacted.size()];
					for (int i = 0; i < currentRead.getEntities().length; i++) {
						DBState st = s.getImpactedStates().get(currentRead.getEntities()[i].getEntityKey());
						if (st == null) {
							LinkedList<DBState> ll = dbState.get(currentRead.getEntities()[i].getEntityKey());
							if(ll != null)
							st = ll.getFirst();
						}
						if(st!=null){
						eNames.add(currentRead.getEntities()[i].getEntityKey());
						entities[i] = createEntity(currentRead.getEntities()[i].getKey(), currentRead.getEntities()[i].getName(), st);
						}
					}
					for(int i = 0; i < diffInImpacted.size(); i++){
						String currKey = diffInImpacted.get(i);
						DBState st = s.getImpactedStates().get(currKey);
						if(st == null || eNames.contains(currKey))
							continue;
						entities[i+currentRead.getEntities().length] = createEntity(currKey.substring(currKey.indexOf(ValidationParams.KEY_SEPERATOR)+1), currKey.substring(0, currKey.indexOf(ValidationParams.KEY_SEPERATOR)), st);						
					}
					LogRecord initRecord = new LogRecord("SS" + Si + " init", "SS" + Si + " init", 0, 0, ValidationParams.UPDATE_RECORD, entities);
					// String entitiesTableString = getRecordTableString(initRecord);

					// TODO:uncommant
					String entitiesTableString = "document.getElementById('result" + divCounter + "').appendChild(TR(" + getRecordTableParametersString(initRecord, "SSIR") + "))";
					// String entitiesTableString = getRecordTableString(initRecord); //"";// getInitTableString(s.getImpactedEntities());
					// schedule += "<a class=\"SSIR\" onclick=\"showRecord('result" + divCounter + "','" + entitiesTableString + "')\" >&nbsp;" + initRecord.getId() + "&nbsp;</a>";
					schedule += "<a class=\"SSIR\" onclick=\"" + entitiesTableString + "\" >&nbsp;" + initRecord.getId() + "&nbsp;</a>";
					// removed onmouseover
					for (LogRecord r : s.getRecords()) {
						// entitiesTableString = getRecordTableString(r);
						schedule += "-";
						String className = "donnotIntersectWithStale";
						if (r.getId().equals(currentRead.getId()))
							className = "staleReadLogRecord";
						else if (r.intersect(currentRead))
							className = "intersectWithStale";
						// schedule += "<a class=\"" + className + "\" onclick=\"showRecord('result" + divCounter + "','" + entitiesTableString + "')\" >&nbsp;" + r.getId() + "&nbsp;</a>";

						entitiesTableString = "document.getElementById('result" + divCounter + "').appendChild(TR(" + getRecordTableParametersString(r, className) + "))";
						schedule += "<a class=\"" + className + "\" onclick=\"" + entitiesTableString + "\">&nbsp;" + getNameString(r) + "&nbsp;</a>";
					}
					schedule += "</td></tr>";
					if (expectedValues != null)
						schedule += createExpectedValuesTR(readRecord, expectedValues);
					bufferWriter.write(schedule);
				}
			} else {
				bufferWriter.write("<tr><td>No Schedules</td></tr>");
			}
			bufferWriter.write("</table></td>");
			bufferWriter.write("<td style=\"width: 250px;\"><div id=\"result" + divCounter + "\"></div></td></tr></table></div>");
			divCounter++;
		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}

	private static ArrayList<String> getDiffInImpacted(ScheduleList SS) {
		ArrayList<String> entities = new ArrayList<String>();
		Set<String> keys = new HashSet<String>();
		for(int i = 0; i < SS.schedules.size(); i++){
			keys.addAll(SS.schedules.get(i).getImpactedStates().keySet());
		}
		for(String key : keys){
			
			DBState first = SS.schedules.get(0).getImpactedStates().get(key);
			if (first == null){
				entities.add(key);
				continue;
			}
			for (int i = 1; i < SS.schedules.size(); i++) {
				Schedule s = SS.schedules.get(i);
				DBState current = s.getImpactedStates().get(key);

				if (current == null) {
					entities.add(key);
					break;
				}
				if (first != current) {
					entities.add(key);
					break;
				}
			}
		}
		return entities;
	}

	private static String createExpectedValuesTR(LogRecord readRecord, HashMap<String, String> expectedValues) {
		String result = "<tr><td align=\"center\"><table class=\"compare\"><tr><th></th><th class=\"compare\">R [" + readRecord.getId() + "]</th><th class=\"compare\">Expected Value</th></tr>";
		for (Entity e : readRecord.getEntities()) {
			for (Property p : e.getProperties()) {
				String pKey = Property.getProprtyKey(e, p);
				String expected = expectedValues.get(pKey);
				result += "<tr><td class=\"compare\">" + pKey + "</td><td align=\"center\" class=\"compare\">" + p.getValue() + "</td><td align=\"center\" class=\"compare\">" + expected + "</td></tr>";
			}
		}
		result += "</table></td></tr>";
		return result;
	}

	private static Entity createEntity(String key, String name, DBState st) {
		if (st == null)
			return null;
		Property[] properties = new Property[st.value.length];
		int index = -1;
		for (int i = 0; i < ValidationParams.ENTITY_NAMES.length; i++) {
			if (name.equals(ValidationParams.ENTITY_NAMES[i]))
				index = i;
		}
		assert index != -1 : "Couldn't find Entity \"" + name + "\"";
		for (int i = 0; i < st.value.length; i++) {
			properties[i] = new Property(ValidationParams.ENTITY_PROPERTIES[index][i], st.value[i], '*');
		}
		Entity e = new Entity(key, name, properties);
		return e;
	}

	private static String getRecordTableString(LogRecord record) {
		String recordsString = "<center><table border=1>";
		recordsString += "<tr><th colspan=3>" + getNameString(record) + "</th></tr>";
		for (Entity e : record.getEntities()) {
			if (e == null) {
				continue;
			} else {
				recordsString += "<tr class=\\\'" + e.getName() + "\\\'><td>" + e.getName() + "</td><td colspan=\\\'2\\\'>" + e.getKey() + "</td></tr>";
				for (Property p : e.getProperties()) {
					recordsString += "<tr class=\\\'" + e.getName() + "\\\'><td>" + p.getName() + "</td><td>" + p.getValue() + "</td><td>" + p.getType() + "</td></tr>";
				}
			}
		}
		recordsString += "</table></center>";
		return recordsString;
	}

	private static String getRecordTableParametersString(LogRecord record, String className) {
		// TR('name', ['Saab', 'Volvo', 'BMW'], ['Saab2', 'Volvo2', 'BMW2']
		String recordsString;
		if (record.getId().contains("init")) {
			recordsString = '\'' + record.getId() + "', ";
		} else {
			recordsString = '\'' + getNameString(record) + "', ";
		}
		String str1, str2, str3, Estr, NUMstr;
		str1 = str2 = str3 = Estr = NUMstr = "[";
		String eSeperator = "";
		String seperator = "";
		for (Entity e : record.getEntities()) {
			if (e == null) {
				continue;
			}
			int sum = 0;
			// recordsString += "<tr class=\\\'" + e.getName() + "\\\'><td>" + e.getName() + "</td><td colspan=\\\'2\\\'>" + e.getKey() + "</td></tr>";
			for (Property p : e.getProperties()) {
				if (p == null)
					continue;
				str1 += seperator + "'" + p.getName() + "'";
				str2 += seperator + "'" + p.getValue() + "'";
				str3 += seperator + "'" + p.getType() + "'";
				seperator = ", ";
				sum++;
				// recordsString += "<tr class=\\\'" + e.getName() + "\\\'><td>" + p.getName() + "</td><td>" + p.getValue() + "</td><td>" + p.getType() + "</td></tr>";
			}
			Estr += eSeperator + "'" + e.getEntityKey() + "'";
			NUMstr += eSeperator + sum;
			eSeperator = ", ";
		}

		Estr += "]";
		NUMstr += "]";
		str1 += "]";
		str2 += "]";
		str3 += "]";

		recordsString += Estr + ", " + NUMstr + ", " + str1 + ", " + str2 + ", " + str3 + ", '" + className + "'";
		return recordsString;
	}

	public static void closeCurrentReadNumber() {
		try {
			bufferWriter.write("</td>");
			bufferWriter.write("</tr></table></body></html>");
			bufferWriter.flush();
			bufferWriter.close();
		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}

	private static void checkFile(String file, String name) {
		File f = new File(file);
		if (f.exists()) {
			f.delete();
		}
		try {
			FileWriter ufstream = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(ufstream);
			if (CSS_FILE.equals(name)) {
				for (int i = 0; i < ValidationParams.ENTITY_NAMES.length; i++) {
					bw.write("." + ValidationParams.ENTITY_NAMES[i] + " {" + ValidationParams.LINE_SEPERATOR);
					bw.write("\t" + entitiesColors[i % entitiesColors.length] + ValidationParams.LINE_SEPERATOR);
					bw.write("}" + ValidationParams.LINE_SEPERATOR);
				}
				bw.write(".SSIR {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: #FFEB3B;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
				bw.write("table.SSIRLight {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: #FFF9D1;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\twidth: 200px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write(".donnotIntersectWithStale {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + donnotIntersectWithStaleColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: white;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
				bw.write("table.donnotIntersectWithStaleLight {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + donnotIntersectWithStaleLightColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\twidth: 200px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write(".staleReadLogRecord {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + staleReadLogRecordColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: white;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
				bw.write("table.staleReadLogRecordLight {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + staleReadLogRecordLightColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\twidth: 200px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write(".intersectReadWithStaleColor {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + intersectReadWithStaleColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: white;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
				bw.write("table.intersectReadWithStaleColorLight {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + intersectReadWithStaleLightColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\twidth: 200px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write(".intersectWriteWithStaleColor {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + intersectWriteWithStaleColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: white;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
				bw.write("table.intersectWriteWithStaleColorLight {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: " + intersectWriteWithStaleLightColor + ";" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tcolor: black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\twidth: 200px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("div.header {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-size: 20px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tfont-weight: bold;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("div.mainBlock:nth-child(even) {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder-radius: 20px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder: 2px solid #000000;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tpadding: 20px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground: #eeeeee;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("div.mainBlock:nth-child(odd) {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder-radius: 20px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder: 2px solid #e1e1e1;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tpadding: 20px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground: #fbfbfb;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("table.compare {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder: solid black 1px;" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder-collapse: collapse;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("table.t1{" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: #ffffff;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
				bw.write("table.t0{" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tbackground-color: #bbbbbb;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("th.compare {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder: 1px solid black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("td.compare {" + ValidationParams.LINE_SEPERATOR);
				bw.write("\tborder: 1px solid black;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

			} else if (JS_FILE.equals(name)) {
				bw.write(showHideJavaScriptFunction + ValidationParams.LINE_SEPERATOR);
				bw.write(showRecordJavaScriptFunction + ValidationParams.LINE_SEPERATOR);

				bw.write("function TR(name, Earr, EPNum, arr1, arr2, arr3, className){" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var table = document.createElement(\"TABLE\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	table.setAttribute(\"id\", name+\"Table\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	table.setAttribute(\"border\", \"1\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	table.setAttribute(\"class\",className+\"Light\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var trth = document.createElement(\"TR\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var th1 = document.createElement(\"TH\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	th1.setAttribute(\"class\",className);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var th2 = document.createElement(\"TH\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	th1.appendChild(document.createTextNode(name));" + ValidationParams.LINE_SEPERATOR);
				bw.write("	th1.setAttribute(\"colspan\", \"2\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var ButtonElement = createButton(\"X\", \"delete\", \"deleteTable\", name+\"Table\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	th2.appendChild(ButtonElement);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	trth.appendChild(th1);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	trth.appendChild(th2);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	table.appendChild(trth);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var start = 0, i = 0;" + ValidationParams.LINE_SEPERATOR);
				bw.write("	for(var j = 0; j < Earr.length; j++){" + ValidationParams.LINE_SEPERATOR);
				bw.write("		var trE = document.createElement(\"TR\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("		var tdE = document.createElement(\"TD\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("		tdE.setAttribute(\"colspan\", \"3\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("		tdE.appendChild(document.createTextNode(Earr[j]));" + ValidationParams.LINE_SEPERATOR);
				bw.write("		tdE.setAttribute(\"align\", \"center\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("		trE.appendChild(tdE);" + ValidationParams.LINE_SEPERATOR);
				bw.write("		table.appendChild(trE);" + ValidationParams.LINE_SEPERATOR);
				bw.write("		if(j !== 0)" + ValidationParams.LINE_SEPERATOR);
				bw.write("			start += EPNum[j-1];" + ValidationParams.LINE_SEPERATOR);
				bw.write("		for(; i < EPNum[j]+start; i++){" + ValidationParams.LINE_SEPERATOR);
				bw.write("			var tr = document.createElement(\"TR\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			var td1 = document.createElement(\"TD\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			td1.setAttribute(\"align\", \"center\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			var td2 = document.createElement(\"TD\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			td2.setAttribute(\"align\", \"center\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			var td3 = document.createElement(\"TD\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			td3.setAttribute(\"align\", \"center\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("			td1.appendChild(document.createTextNode(arr1[i]));" + ValidationParams.LINE_SEPERATOR);
				bw.write("			td2.appendChild(document.createTextNode(arr2[i]));" + ValidationParams.LINE_SEPERATOR);
				bw.write("			td3.appendChild(document.createTextNode(arr3[i]));" + ValidationParams.LINE_SEPERATOR);
				bw.write("			tr.appendChild(td1);" + ValidationParams.LINE_SEPERATOR);
				bw.write("			tr.appendChild(td2);" + ValidationParams.LINE_SEPERATOR);
				bw.write("			tr.appendChild(td3);" + ValidationParams.LINE_SEPERATOR);
				bw.write("			table.appendChild(tr);" + ValidationParams.LINE_SEPERATOR);
				bw.write("		}" + ValidationParams.LINE_SEPERATOR);
				bw.write("	}" + ValidationParams.LINE_SEPERATOR);
				bw.write("	return table;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("function createButton(text, buttonClass, onClickFunction, onClickParams){" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var ButtonElement = document.createElement(\"button\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	ButtonElement.setAttribute(\"class\", buttonClass);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	ButtonElement.setAttribute(\"onclick\", onClickFunction+\"('\"+onClickParams+\"')\");" + ValidationParams.LINE_SEPERATOR);
				bw.write("	ButtonElement.appendChild(document.createTextNode(text));" + ValidationParams.LINE_SEPERATOR);
				bw.write("	return ButtonElement;" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);

				bw.write("function deleteTable(tableName){" + ValidationParams.LINE_SEPERATOR);
				bw.write("	var table = document.getElementById(tableName);" + ValidationParams.LINE_SEPERATOR);
				bw.write("	table.parentNode.removeChild(table);" + ValidationParams.LINE_SEPERATOR);
				bw.write("}" + ValidationParams.LINE_SEPERATOR);
			}

			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void writeIntervalsFromTo(int staleCount,String title, ArrayList<LogRecord> intervals) {
		try {
			bufferWriter.write("<div class=\"mainBlock\"><center>");
			String[] str = new String[intervals.size()];
			String[] colors = new String[intervals.size()];
			for (int i = 0; i < intervals.size(); i++) {
				String className = "donnotIntersectWithStale";
				colors[i] = donnotIntersectWithStaleColor;
				if (intervals.get(i).getId().equals(currentRead.getId())) {
					className = "staleReadLogRecord";
					colors[i] = staleReadLogRecordColor;
				} else if (intervals.get(i).intersect(currentRead)) {
					className = "intersectWithStale";
					switch (isReadOrWriteLogRecord(intervals.get(i))) {
					case 'R':
						colors[i] = intersectReadWithStaleColor;
						className = "intersectReadWithStaleColor";
						break;
					case 'W':
						colors[i] = intersectWriteWithStaleColor;
						className = "intersectWriteWithStaleColor";
						break;
					}
				}
				String entitiesTableString = "document.getElementById('ImageResult').appendChild(TR(" + getRecordTableParametersString(intervals.get(i), className) + "))";
				// schedule += "<a class=\"" + className + "\" onclick=\"showRecord('result" + divCounter + "','" + entitiesTableString + "')\" >&nbsp;" + r.getId() + "&nbsp;</a>";
				str[i] = "<a class=\"" + className + "\" onclick=\"" + entitiesTableString + "\">&nbsp;" + getNameString(intervals.get(i)) + "&nbsp;</a>";
			}

			bufferWriter.write("<div class=\"header\"><button style=\"width: 24px;\" onclick=\"showHide('I" + imageCounter + "', this)\">-</button> " + title + "</div>");
			bufferWriter.write("<div id=\"I" + imageCounter + "\" style=\"display: block;\">");
			bufferWriter.write("<table><tr><td>");
			String image = writeIntervalsFile(staleCount,intervals, colors);
			bufferWriter.write("<img src=\"" + image + "\" />");
			bufferWriter.write("</td><td><table>");
			for (int i = 0; i < intervals.size(); i++) {
				bufferWriter.write("<tr><td height=\"20px\" bgcolor=\""+colors[i]+"\">" + str[i] + "</td></tr><tr><td height=\"3px\"></td></tr>");
			}
			bufferWriter.write("</table></td><td style=\"width: 200px;\"><div id=\"ImageResult\"></div>");
			bufferWriter.write("</td></tr></table>");
			bufferWriter.write("</div></center></div>");
		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}

	private static String getNameString(LogRecord r) {
		char type = isReadOrWriteLogRecord(r);
		String Name = type + " [" + r.getId() + "]";
		return Name;
	}

	private static char isReadOrWriteLogRecord(LogRecord r) {
		char type = 'Z';
		for (Entity e : r.getEntities()) {
			if (e == null)
				continue;
			for (Property p : e.getProperties()) {
				if (p == null)
					continue;
				switch (p.getType()) {
				case ValidationParams.NEW_VALUE_UPDATE:
				case ValidationParams.VALUE_DELETED:
				case ValidationParams.INCREMENT_UPDATE:
					type = 'W';
					break;
				case ValidationParams.READ_RECORD:
					type = 'R';
					break;
				}
			}
		}
		return type;
	}

	private static String writeIntervalsFile(int staleCounter,List<LogRecord> intervals, String[] colors) {
		try {
			String textFile = ImagesDirForJava + "round_" + staleCounter + "_" + imageCounter + ".txt";
			File ufile = new File(textFile);
			String image = ImagesDirForHTML + "round_" + staleCounter + "_" + imageCounter + ".txt.png";
			imageCounter++;
			FileWriter ufstream = new FileWriter(ufile);
			BufferedWriter br = new BufferedWriter(ufstream);
			for (int i = 0; i < intervals.size(); i++) {
				LogRecord r = intervals.get(i);
				br.write(getNameString(r) + "," + r.getStartTime() + "," + r.getEndTime() + "," + colors[i]);
				br.newLine();
			}
			br.flush();
			br.close();
			String cmd="";
			if (ValidationParams.WINDOWS)
				cmd = UtilConstants.python+" ./py/drawIntervals_stale.py " + textFile;
			else
				cmd = " python ./py/drawIntervals_stale.py " + textFile;


			System.out.println("Running: " + cmd);
			executeRuntime(cmd, true);

			return image;
		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
		return null;
	}

	public static void executeRuntime(String cmd, boolean wait) {
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			if (wait) {
				InputStream stdout = p.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
				String line = "";
				while ((line = reader.readLine()) != null) {
					System.out.println("Commandout: " + line);
				}
				p.waitFor();
			} else
				Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}

	private static ArrayList<LogRecord> getAllIntervals(String logDir,LogRecord record, ScheduleList validationSS) {
		ArrayList<LogRecord> intervals = new ArrayList<LogRecord>();
		HashMap<String, Boolean> SearchedEntities = new HashMap<String, Boolean>();
		intervals.add(record);
		if (validationSS.schedules.size() > 0) {
			Schedule s = validationSS.schedules.get(0);
			int length = s.getRecords().size();

			for (Schedule ss : validationSS.schedules) {
				if (ss.getRecords().size() > length) {
					s = ss;
					length = ss.getRecords().size();
				}
			}

			for (LogRecord r : s.getRecords()) {
				intervals.add(r);
				for (Entity e : r.getEntities()) {
					String searchStr = e.getName() + ";" + e.getKey() + ";";
					if (!SearchedEntities.containsKey(searchStr))
						SearchedEntities.put(searchStr, false);
				}

			}
		}
		boolean NotSearchedEntities = true;
		while (NotSearchedEntities) {
			ArrayList<LogRecord> intervals2 = getAllIntervalsFromLogFiles(logDir,record, intervals, SearchedEntities);
			if (intervals2.isEmpty())
				break;
			Collections.sort(intervals2);
			// remove duplicates
			for (int i = intervals2.size() - 1; i > 0; i--) {
				if (intervals2.get(i).getId().equals(intervals2.get(i - 1).getId())) {
					intervals2.remove(i);
				}
			}
			for (int i = intervals.size() - 1; i >= 0; i--) {
				for (int j = intervals2.size() - 1; j >= 0; j--) {
					if (intervals.get(i).getId().equals(intervals2.get(j).getId())) {
						intervals2.remove(j);
						break;
					}
				}
			}
			if(intervals2.size() == 0)
				NotSearchedEntities = false;
			for (LogRecord r : intervals2) {
				intervals.add(r);
				for (Entity e : r.getEntities()) {
					String searchStr = e.getName() + ";" + e.getKey() + ";";
					if (!SearchedEntities.containsKey(searchStr)) {
						SearchedEntities.put(searchStr, false);
					}
				}
			}
			Collections.sort(intervals);
			// remove duplicates
			for (int i = intervals.size() - 1; i > 0; i--) {
				if (intervals.get(i).getId().equals(intervals.get(i - 1).getId())) {
					intervals.remove(i);
				}
			}
		}
		for (int i = 0; i < intervals.size(); i++) {
			if (intervals.get(i).getId().equals(record.getId())) {
				for (int j = i + 1; j < intervals.size(); j++) {
					if (isReadOrWriteLogRecord(intervals.get(j)) == 'R') {
						intervals.remove(j);
						j--;
					}
				}
				break;
			}
		}
		return intervals;
	}

	private static ArrayList<LogRecord> getAllIntervalsFromLogFiles(String logDir ,LogRecord record, ArrayList<LogRecord> intervals, HashMap<String, Boolean> SearchedEntities) {
		// String path = "";
		// try {
		// path = new File(".").getCanonicalPath();
		// path = " /cygdrive/c" + path.substring(path.indexOf("\\Users")) + tempTraces.substring(1);
		// path = path.replaceAll("[\\\\]", "/");
		//
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		String orgFile = tempTraces + "org.txt";
		String sortedFile = tempTraces + "sorted.txt";
		String traceFolder = logDir;
		if(ValidationParams.WINDOWS){
		traceFolder = " /cygdrive/c" + traceFolder.substring(traceFolder.indexOf("\\Users"));
		traceFolder = traceFolder.replaceAll("[\\\\]", "/");
		}
		String createOrAppend = " > ";
		Set<Entry<String, Boolean>> entities = SearchedEntities.entrySet();
		for (Entry<String, Boolean> r : entities) {
			// SearchedEntities.put(r.getKey(), true);
			String searchStr = r.getKey();
			String cmd=null;
			if(ValidationParams.WINDOWS)
			cmd = "C:/cygwin64/bin/grep -rFh \"" + searchStr + "\" " + traceFolder + createOrAppend + orgFile;
			else
			cmd = "grep -rFh \"" + searchStr + "\" " + traceFolder + createOrAppend + orgFile;

			createOrAppend = " >> ";
			Utilities.executeRuntime(cmd, true, orgFile);

		}

		sort(orgFile, sortedFile);

		return getWantedIntervalsFromFile(sortedFile, intervals);
	}

	private static ArrayList<LogRecord> getWantedIntervalsFromFile(String sortedFile, ArrayList<LogRecord> intervals) {
		ArrayList<LogRecord> newIntervals = new ArrayList<LogRecord>();
		try {
			FileInputStream fstreams = new FileInputStream(sortedFile);
			DataInputStream dataInStreams = new DataInputStream(fstreams);
			BufferedReader bReaders = new BufferedReader(new InputStreamReader(dataInStreams));
			String line = null, pLine = "";

			LogRecord records;
			while ((line = bReaders.readLine()) != null) {
				if (line.equals(pLine))
					continue;
			//	System.out.println(line);
				pLine = line;
				records = LogRecord.createLogRecord(line,true,false);
				for (LogRecord r : intervals) {
					if (r.intersect(records)/* && r.overlap(records)*/)
						newIntervals.add(records);
				}
			}

			if (dataInStreams != null)
				dataInStreams.close();
			if (bReaders != null)
				bReaders.close();
		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
		return newIntervals;
	}

	

	public static void sort(String src, String dist) {
		ArrayList<MyObject> objects = new ArrayList<MyObject>();
		FileInputStream fstreams;
		try {
			fstreams = new FileInputStream(src);
			DataInputStream dataInStreams = new DataInputStream(fstreams);
			BufferedReader bReaders = new BufferedReader(new InputStreamReader(dataInStreams));

			String line;
			while ((line = bReaders.readLine()) != null) {
				String[] tokens = line.split("[,]");
				long startTime = Long.parseLong(tokens[startTimeIndex]);
				objects.add(new MyObject(startTime, line));
			}
			bReaders.close();
			dataInStreams.close();
			fstreams.close();

		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.out.println("Log file not found " + e.getMessage());
		}

		// System.out.println("Completed Reading the file.");

		Collections.sort(objects);
		// System.out.println("Completed Sorting the file.");

		File distFile = new File(dist);
		FileWriter ufstream;
		try {
			ufstream = new FileWriter(distFile);
			BufferedWriter bWriter = new BufferedWriter(ufstream);

			for (int i = 0; i < objects.size(); i++) {
				bWriter.write(objects.get(i).line + "\n");
			}
			bWriter.close();
			ufstream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Completed Writing sorted file.");
	}
}
