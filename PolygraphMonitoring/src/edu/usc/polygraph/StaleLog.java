package edu.usc.polygraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class StaleLog {
	public char type;
	public String tid;
	public long staleOffset;
	public int readPartition;
	public int writePartition;
	public long lastReadOffset;
	public long lastWriteOffset;
	public String actionName;
	public HashSet<String> entities;
	public HashSet<String> properties;
	public HashMap<String, HashSet<String>> expected;
	public String line;

	public StaleLog(String line) {
		this.line = line;//R,32-9,706,0,1664,1,1233,GetProfile,MEMBER-8-FRIEND_CNT:99.00
//		System.out.println(line);
		
		//Z,1-2,4,0,-1,1,535,Withdraw,
		//Account-Y-Balance:99999600#;
		//Account-Y-Balance:99999500#;
		//Account-Y-Balance:99999600#;
		//Account-Y-Balance:99999500#;
		//Account-Y-Balance:99999600#;
		//Account-Y-Balance:99999600
		System.out.println("YAZ:"+line);
		String[] tokens = line.split(",");
		type = tokens[0].charAt(0);
		tid = tokens[1];
		staleOffset = Long.parseLong(tokens[2]);
		readPartition = Integer.parseInt(tokens[3]);
		lastReadOffset = Long.parseLong(tokens[4]);
		writePartition = Integer.parseInt(tokens[5]);
		lastWriteOffset = Long.parseLong(tokens[6]);
		actionName = tokens[7];
		entities = new HashSet<String>();//new ArrayList<String>();
		properties = new HashSet<String>();//new ArrayList<String>();
		expected = new HashMap<String, HashSet<String>>();
		String[] entitiesTokens = tokens[8].split("#;");
		for(int i = 0; i < entitiesTokens.length; i++){
			String[] pTokens = entitiesTokens[i].split(":");
			String eName = pTokens[0].substring(0, pTokens[0].lastIndexOf('-'));
			String pName = pTokens[0].substring(pTokens[0].lastIndexOf('-')+1);
			entities.add(eName);
			properties.add(pTokens[0]);			
			HashSet<String> al = expected.get(pTokens[0]);
			if(al == null){
				al = new HashSet<String>();
				al.add(pTokens[1]);
				expected.put(pTokens[0], al);
			} else {
				al.add(pTokens[1]);				
			}
		}
	}

	public String toString(){
		return line;
	}
}
