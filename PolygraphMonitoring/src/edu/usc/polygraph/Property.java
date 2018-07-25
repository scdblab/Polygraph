package edu.usc.polygraph;

import edu.usc.polygraph.website.PolygraphUISettings;

public class Property {
	private String name;
	private String value;
	private char type = 'X';
	private boolean pk = false;
	private String variable_type;
	private String variable_name;

	public boolean isPk() {
		return pk;
	}

	public void setPk(boolean pk) {
		this.pk = pk;
	}

	public String getVariable_type() {
		return variable_type;
	}

	public void setVariable_type(String variable_type) {
		this.variable_type = variable_type;
	}

	public String getVariable_name() {
		return variable_name;
	}

	public void setVariable_name(String variable_name) {
		this.variable_name = variable_name;
	}

	public Property(String name, String value, char type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public Property getCopy() {
		Property p = new Property(name, value, type);
		return p;
	}

	public static String getProprtyKey(Entity e, Property p) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.getName());
		sb.append(PolygraphUISettings.KEY_SEPERATOR);
		sb.append(e.getKey());
		sb.append(PolygraphUISettings.KEY_SEPERATOR);
		sb.append(p.getName());
		return sb.toString();
	}
	
	public String getProprtyKey(Entity e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.getName());
		sb.append(PolygraphUISettings.KEY_SEPERATOR);
		sb.append(e.getKey());
		sb.append(PolygraphUISettings.KEY_SEPERATOR);
		sb.append(getName());
		return sb.toString();
	}
	
	public String toString(){
		return "["+name+":"+type+":"+pk+":"+variable_name+":"+variable_type+"]";		
	}

	public String toPrint() {
		String result = String.format("%s:%s:%c", name, value, type);
		return result;
	}
}