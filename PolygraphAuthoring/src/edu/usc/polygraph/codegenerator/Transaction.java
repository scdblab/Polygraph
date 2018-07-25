package edu.usc.polygraph.codegenerator;

import java.util.*;

import edu.usc.polygraph.Entity;
import edu.usc.polygraph.PolygraphUISettings;
import edu.usc.polygraph.Property;

public class Transaction {
	public Transaction(String name) {
		this.name=name;
		this.topic=null;
	}
	String topic;
	String name;
	List<Integer> key;
	String JSON;
	char type=' ';
	ArrayList<Entity> entities;
	ArrayList<ConflictedTransaction>conflicted;
	ArrayList<String> relationships;
	HashMap<String,String> propsVars;
	public HashMap<String, PObject> mEntities;


	@Override
	public String toString(){
		StringBuilder sb= new StringBuilder();
		sb.append("TransName="+name+"\n");
		sb.append("TransTopic="+topic+"\n");
		for (Entity e:entities){
			sb.append("Entity:"+e.getName()+"\n");
			sb.append("numProps:"+e.getProperties().length+"\n");


		}
		sb.append("Conflicts:");
		for (ConflictedTransaction conflict: conflicted){
			sb.append(conflict.key+",");
		}
		sb.append("\npartitioning key:"+key);

		return sb.toString();

	}
	public void setType(){
		for (Entity e: entities){
			if (e.getProperties()==null)
				continue;
			for (Property p: e.getProperties()){
				if(type==' '){
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
				}
				else if (type==PolygraphUISettings.READ_RECORD){
					if (p.getType()==PolygraphUISettings.NEW_VALUE_UPDATE || p.getType()==PolygraphUISettings.VALUE_DELETED ||p.getType()==PolygraphUISettings.INCREMENT_UPDATE ||p.getType()==PolygraphUISettings.DECREMENT_UPDATE_INTERFACE)
						type=PolygraphUISettings.READ_WRITE_RECORD;
				}
				else if (type==PolygraphUISettings.UPDATE_RECORD){
					if (p.getType()==PolygraphUISettings.VALUE_READ ||p.getType()==PolygraphUISettings.VALUE_NA)
						type=PolygraphUISettings.READ_WRITE_RECORD;
				}
				if (type==PolygraphUISettings.READ_WRITE_RECORD){
					break;

				}
			} // prop loop
			if (type==PolygraphUISettings.READ_WRITE_RECORD)
				break;
		
	} // entity loop
}
}
class ConflictedTransaction{
	Transaction transaction;
	String key;
	public ConflictedTransaction(Transaction t, String k){
		transaction=t;
		key=k;
	}
}