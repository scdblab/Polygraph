package edu.usc.polygraph.treemap;


public interface TreeType extends Comparable<TreeType> {
public TreeType convert(String s);
public String getStringValue();

}
