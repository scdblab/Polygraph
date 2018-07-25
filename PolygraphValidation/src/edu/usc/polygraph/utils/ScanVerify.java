package edu.usc.polygraph.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.usc.polygraph.LogRecord;
import edu.usc.polygraph.Utilities;

public class ScanVerify {

	public static void main(String[] args) {
		//readFile(args[0], args[1]);
		readFile("/home/mr1/out2.txt", "/home/mr1/Downloads/CADS_YCSB/logs3");

	}

	private static void readFile(String file, String log) {
		FileInputStream fstreams;
		try {
			fstreams = new FileInputStream(file);
			DataInputStream dataInStreams = new DataInputStream(fstreams);
			BufferedReader bReaders = new BufferedReader(new InputStreamReader(dataInStreams));

			String line;
			while ((line = bReaders.readLine()) != null) {
				if (line.contains("Stale Data")){
					boolean verfied=false;
					String id= line.split(":")[1].trim();
					String tid= id.split("-")[0];
					String cmd="grep -rh \","+id+",\" "+log+"/read0-"+tid+"*" ;//grep -r ",9-367," r*

					String res=Utilities.executeRuntime(cmd,true);
					LogRecord staleScan = LogRecord.createLogRecord(res, true,false);
					line=bReaders.readLine();
					String expected= line.split("Expected")[1];
					expected=expected.substring(expected.indexOf('(')+1,expected.indexOf(':'));
					//grep -r "2:144445(" u*
					cmd="grep -rh \"2:"+expected+"(\" "+log+"/u*";
					res=Utilities.executeRuntime(cmd,true);
					String updates[]=res.split(Utilities.newline);
					LogRecord urec=null;
					for (String u:updates){
						urec=LogRecord.createLogRecord(u, false,false);
						if (urec.overlap(staleScan))
							break;
						
						urec=null;
						
					}
					if (urec==null){
						System.out.println("Verfication failed for:"+staleScan.getId());
						continue;
					}
					//grep -rh "USR;144;FIELD2:144445(" r*
					cmd="grep -rh \"USR;"+urec.getEntities()[0].getKey()+";FIELD2:"+expected+"(\" "+log+"/r*";
					res=Utilities.executeRuntime(cmd,true);
					LogRecord srec=null;
					String scans[]=res.split(Utilities.newline);
					for (String s:scans){
						srec=LogRecord.createLogRecord(s, false,false);
						if (urec.overlap(srec)){
							if (staleScan.getStartTime()>srec.getEndTime()){
								verfied=true;
								break;
							}
						}
						
						
						
					}
					if (verfied)
					System.out.println(staleScan.getId()+ " has been verfied correctly");
					else
						System.out.println(staleScan.getId()+ " has NOT been verfied");

				}
			}
			bReaders.close();
			dataInStreams.close();
			fstreams.close();

		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.out.println("Log file not found " + e.getMessage());
		}

		
	}

}
