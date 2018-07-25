package edu.usc.polygraph.treemap;

public class StringTreeType implements TreeType{

	String value=null;
	public String getValue() {
		return value;
	}
	@Override
	public int compareTo(TreeType o) {
		StringTreeType myType = (StringTreeType) o;
		if (value==null || myType.getValue()==null ) {
			System.out.println("Error: TreeMap value is not set");
			System.exit(0);
		}
		return value.compareTo( myType.getValue());
	}

	@Override
	public TreeType convert(String s) {
		value=s;
		return this;
	}
	@Override
	public String getStringValue() {
		return value;
	}

}
