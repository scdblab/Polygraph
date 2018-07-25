package edu.usc.polygraph.treemap;

public class IntegerTreeType implements TreeType{

	int value=-1;
	public int getValue() {
		return value;
	}
	@Override
	public int compareTo(TreeType o) {
		IntegerTreeType myType = (IntegerTreeType) o;
		if (value==-1 || myType.getValue()==-1 ) {
			System.out.println("Error: TreeMap value is not set");
			System.exit(0);
		}
		return Integer.compare(value, myType.getValue());
	}

	@Override
	public TreeType convert(String s) {
		value=Integer.parseInt(s);
		return this;
	}
	@Override
	public String getStringValue() {
		return String.valueOf(value);
	}

}
