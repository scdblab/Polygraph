package edu.usc.polygraph.treemap;

import java.util.NavigableMap;
import java.util.TreeMap;


import edu.usc.polygraph.DBState;
import edu.usc.polygraph.ValidationParams;

public class Tree<V extends TreeType> {
	TreeMap<V,DBState> tree;
	public Tree() {
		tree= new TreeMap<>();
	}




	public void insert(String key, DBState st,TreeType c) {

		tree.put(createObject(key,c),st);


	}
	@SuppressWarnings("unchecked")
	public void insert(TreeType key, DBState st) {

		tree.put((V)key,st);


	}
	@SuppressWarnings("unchecked")
	public V createObject (String key, TreeType c) {

		V v;
		try {
			v= (V)c.getClass().newInstance();


			return (V)v.convert(key);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public void remove(String key,TreeType c) {

		tree.remove(createObject(key,c));

	}
	public NavigableMap<V, DBState> searchAll(String lower, boolean l,String upper,boolean u,TreeType c) {
		NavigableMap<V, DBState> res=null;
		if (upper==null)
		{
			res = tree.tailMap(createObject(lower,c), l);

		}
		else
			res = tree.subMap(createObject(lower,c), l, createObject(upper,c), u);
		return res;
	}

	public void clear() {
		tree.clear();

	}

	@SuppressWarnings("unchecked")
	public boolean contains(TreeType key) {
		return tree.containsKey((V)key);
	}
	
	
	public TreeMap<V, DBState> getTree() {
		return tree;
	}
	public static void main (String args[]) {
		Tree <IntegerTreeType>t = new Tree<>();
		DBState s= new DBState(0, "s");
		TreeType k;
		IntegerTreeType s1= new IntegerTreeType();
		s1.convert("2");
		IntegerTreeType s2= new IntegerTreeType();
		s2.convert("1");
		t.insert("1",s,s1);
		System.out.println(t.contains(s2));
	}
}
