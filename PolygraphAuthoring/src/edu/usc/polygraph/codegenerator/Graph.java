package edu.usc.polygraph.codegenerator;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A directed graph data structure.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class Graph<T> {
	public static void main(String[] args) {
		String ER = CodeGeneratorFunctions.readFile("/home/mr1/JSON_Tests/test.txt");
		String T1 = CodeGeneratorFunctions.readFile("/home/mr1/JSON_Tests/test_T1.txt");
		List<Graph<JSONObject>> gs = Graph.buildTreeGraph(ER, T1);
		// Graph<String> g = new Graph<String>();
		// Vertex<String> a = new Vertex<String>("a");
		// Vertex<String> b = new Vertex<String>("b");
		// Vertex<String> c = new Vertex<String>("c");
		// Vertex<String> d = new Vertex<String>("d");
		// // Edge <String> ba= new Edge<String>(b, a,"ba");
		// // Edge <String> db= new Edge<String>(d, b,"db");
		// // Edge <String> ad= new Edge<String>(a, d,"ad");
		// // Edge <String> ca= new Edge<String>(c, a,"ca");
		// g.addVertex(a);
		// g.addVertex(b);
		// g.addVertex(c);
		// g.addVertex(d);
		// g.addEdge(b, a, "ba1");
		// g.addEdge(b, a, "ba2");
		// g.addEdge(d, b, "db");
		// g.addEdge(d, c, "dc");
		// g.addEdge(a, d, "ad");
		// g.addEdge(c, a, "ca");
		if (gs != null) {
			int i = 0;
			for (Graph<JSONObject> g : gs) {
				boolean result = g.validateTree();
				if (result) {
					if (g.isOneOccurance(g.getRoot())) {

//						System.out.println("gs[" + i + "] is a tree, with root " + g.getRoot().name);
					} else {
//						System.out.println("gs[" + i + "] is a tree, but its root (" + g.getRoot().name + ") was refrenced multiple times.");
					}
				} else {
//					System.out.println("g[" + i + "] is not a tree");
				}
//				System.out.println("----------------");
				i++;
			}
		} else {
//			System.out.println("No trees were constructed.");
		}
		// HashSet<String> allowed = new HashSet<String>();
		// allowed.add("ba1");
		// allowed.add("ba2")

		// VisitedNodes<String> visitor= new VisitedNodes<String>();
		// HashSet<String> referencedEnities = new HashSet<String>();
		// for (Vertex<String> v : g.getVerticies()) {
		//
		// referencedEnities.add(v.name);
		// }
		// for (Edge<String> edge : g.getEdges()) {
		// if (!edge.name.equals("12"))
		// allowed.add(edge.name);
		// }
		// HashSet<String> res = g.getPartitioningKeys(referencedEnities, allowed);
		// System.out.println(res);

	}

	public boolean isOneOccurance(Vertex<JSONObject> node) {
		JSONArray elements = node.getData().getJSONArray("sets");
		if (elements.length() > 1 || elements.length() < 1)
			return false;
		try {
			boolean single = ((JSONObject) elements.get(0)).getBoolean("single");
			return single;
		} catch (JSONException e) {
			System.out.println("Old files, no single variable in sets");
		}
		return true;
	}

	/** Color used to mark unvisited nodes */
	public static final int VISIT_COLOR_WHITE = 1;

	/** Color used to mark nodes as they are first visited in DFS order */
	public static final int VISIT_COLOR_GREY = 2;

	/** Color used to mark nodes after descendants are completely visited */
	public static final int VISIT_COLOR_BLACK = 3;

	/** Vector<Vertex> of graph verticies */
	private List<Vertex<T>> verticies;

	/** Vector<Edge> of edges in the graph */
	private List<Edge<T>> edges;
	private HashSet<String> edgesNames;
	private HashMap<String, Vertex<T>> vertsMap;

	/** The vertex identified as the root of the graph */
	private Vertex<T> rootVertex;

	/**
	 * Construct a new graph without any vertices or edges
	 */
	public Graph() {
		verticies = new ArrayList<Vertex<T>>();
		edges = new ArrayList<Edge<T>>();
		edgesNames = new HashSet<String>();
		vertsMap = new HashMap<String, Vertex<T>>();
	}

	public Graph(Graph<T> g) {
		verticies = new ArrayList<Vertex<T>>();
		edges = new ArrayList<Edge<T>>();
		edgesNames = new HashSet<String>();
		vertsMap = new HashMap<String, Vertex<T>>();
		for (Vertex<T> v : g.verticies) {
			verticies.add(v.getCopy());
		}
		for (Edge<T> e : g.edges) {
			Vertex<T> from = null, to = null;
			for (Vertex<T> v : verticies) {
				if (e.getFrom().id == v.id) {
					from = v;
				} else if (e.getTo().id == v.id) {
					to = v;
				}
			}
			addEdge(from, to, e.name);
		}
	}

	/**
	 * Are there any verticies in the graph
	 * 
	 * @return true if there are no verticies in the graph
	 */
	public boolean isEmpty() {
		return verticies.size() == 0;
	}

	/**
	 * Add a vertex to the graph
	 * 
	 * @param v
	 *            the Vertex to add
	 * @return true if the vertex was added, false if it was already in the graph.
	 */
	public boolean addVertex(Vertex<T> v) {
		boolean added = false;
		if (verticies.contains(v) == false && !vertsMap.containsKey(v.name)) {
			added = verticies.add(v);
			if (added)
				vertsMap.put(v.name, v);
		}
		return added;
	}

	/**
	 * Get the vertex count.
	 * 
	 * @return the number of verticies in the graph.
	 */
	public int size() {
		return verticies.size();
	}

	/**
	 * Get the root vertex
	 * 
	 * @return the root vertex if one is set, null if no vertex has been set as the root.
	 */
	public Vertex<T> getRootVertex() {
		return rootVertex;
	}

	/**
	 * Set a root vertex. If root does no exist in the graph it is added.
	 * 
	 * @param root
	 *            - the vertex to set as the root and optionally add if it does not exist in the graph.
	 */
	public void setRootVertex(Vertex<T> root) {
		this.rootVertex = root;
		if (verticies.contains(root) == false)
			this.addVertex(root);
	}

	/**
	 * Get the given Vertex.
	 * 
	 * @param n
	 *            the index [0, size()-1] of the Vertex to access
	 * @return the nth Vertex
	 */
	public Vertex<T> getVertex(int n) {
		return verticies.get(n);
	}

	/**
	 * Get the graph verticies
	 * 
	 * @return the graph verticies
	 */
	public List<Vertex<T>> getVerticies() {
		return this.verticies;
	}

	static Graph<String> buildGraph(String erFile) {
		Graph<String> g = new Graph<String>();

		HashMap<String, Vertex<String>> verts = new HashMap<String, Vertex<String>>();

		JSONObject obj = new JSONObject(erFile);// CodeGeneratorFunctions.readFile(erFile)

		JSONArray elements = obj.getJSONArray("Entities");

		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("type").equals("Entity")) {

				String name = String.valueOf(i);
				Vertex<String> v = new Vertex<String>(-1, name);
				// System.out.println("creating vertix:"+name);
				g.addVertex(v);
				verts.put(name, v);
			}
		}
		// Relationship
		elements = obj.getJSONArray("Relationships");

		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("type").equals("Relationship")) {

				String name = String.valueOf(i);
				String sCard = element.getString("sCardinality");
				String dCard = element.getString("dCardinality");
				if (sCard.equals("one") || dCard.equals("one")) {

					int source = element.getInt("source");
					int dest = element.getInt("destination");

					Vertex<String> vTo = verts.get(dest + "");
					Vertex<String> vfrom = verts.get(source + "");
					if (sCard.equals("one")) {
						g.addEdge(vfrom, vTo, name);
						// System.out.println("creating edge: name:"+name +" from:"+vfrom.name + " to:"+vTo.name);

					}
					if (dCard.equals("one")) {
						g.addEdge(vTo, vfrom, name);
						// System.out.println("creating edge: name:"+name +" from:"+vTo.name + " to:"+vfrom.name);

					}

				}
				// JSONArray slots = element.getJSONArray("slots");
				// for (int j = 0; j < slots.length(); j++) {
				// JSONObject slotTox = (JSONObject) slots.get(j);
				// if (!slotTox.getString("cardinality").equals("one"))
				// continue;
				// for (int k = 0; k < slots.length(); k++) {
				// if (k==j)
				// continue;
				// JSONObject slotFrom = (JSONObject) slots.get(k);

			}
			// g.addEdge(from, to, name);

			// }

		}

		return g;

	}

	static List<Graph<JSONObject>> buildTreeGraph(String erFile, String tiFile) {
		HashMap<Integer, Vertex<JSONObject>> vertices = new HashMap<Integer, Vertex<JSONObject>>();
		HashMap<Integer, EdgeStruct<JSONObject>> edges = new HashMap<Integer, EdgeStruct<JSONObject>>();

		JSONObject erObj = new JSONObject(erFile);
		JSONObject tiObj = new JSONObject(tiFile);

		JSONArray elements = erObj.getJSONArray("Entities");
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("type").equals("Entity")) {
				String name = element.getString("name");
				Vertex<JSONObject> v = new Vertex<JSONObject>(i, name, element);
				// g.addVertex(v);
				vertices.put(i, v);
			}
		}

		elements = erObj.getJSONArray("Relationships");
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("type").equals("Relationship")) {
				String name = element.getString("name");
				String sCard = element.getString("sCardinality");
				String dCard = element.getString("dCardinality");
				int source = element.getInt("source");
				int dest = element.getInt("destination");
				EdgeStruct<JSONObject> e = new EdgeStruct<JSONObject>(name, source, dest, sCard, dCard, element);
				edges.put(i, e);
			}
		}

		Graph<JSONObject> g = new Graph<JSONObject>();
		List<EdgeStruct<JSONObject>> one_to_one_edges = new ArrayList<EdgeStruct<JSONObject>>();
		elements = tiObj.getJSONArray("Elements");
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("elementType").equals("Entity")) {
				int index = element.getInt("eid");
				Vertex<JSONObject> v = vertices.get(index);
				v.setData(element);
				g.addVertex(v);
			}
		}
		for (int i = 0; i < elements.length(); i++) {
			JSONObject element = (JSONObject) elements.get(i);
			if (element.getString("elementType").equals("Relationship")) {
				int index = element.getInt("eid");
				EdgeStruct<JSONObject> e = edges.get(index);
				if (e.cSource.equals("many") && e.cDestenation.equals("many"))
					return null;
				else {
					Vertex<JSONObject> vTo = vertices.get(e.destenation);
					Vertex<JSONObject> vfrom = vertices.get(e.source);
					if (e.cSource.equals("one") && e.cDestenation.equals("many")) {
						g.addEdge(vfrom, vTo, e.name);
					} else if (e.cSource.equals("many") && e.cDestenation.equals("one")) {
						g.addEdge(vTo, vfrom, e.name);
					} else if (e.cSource.equals("one") && e.cDestenation.equals("one")) {
						one_to_one_edges.add(e);
					}
				}
			}
		}
		List<Graph<JSONObject>> result = new ArrayList<Graph<JSONObject>>();
		result.add(g);
		List<Graph<JSONObject>> temp = new ArrayList<Graph<JSONObject>>();
		List<Graph<JSONObject>> temp2;
		for (EdgeStruct<JSONObject> e : one_to_one_edges) {
			while (!result.isEmpty()) {
				g = result.get(0);
				result.remove(0);
				Graph<JSONObject> g1 = new Graph<JSONObject>(g);
				Graph<JSONObject> g2 = new Graph<JSONObject>(g);
				Vertex<JSONObject> vTo = g1.getVertexById(e.destenation);
				Vertex<JSONObject> vfrom = g1.getVertexById(e.source);
				g1.addEdge(vfrom, vTo, e.name);
				vTo = g2.getVertexById(e.destenation);
				vfrom = g2.getVertexById(e.source);
				g2.addEdge(vTo, vfrom, e.name);
				temp.add(g1);
				temp.add(g2);
			}
			temp2 = result;
			result = temp;
			temp = temp2;
		}

		return result;
	}

	private Vertex<T> getVertexById(int id) {
		for(Vertex<T> v : verticies){
			if(v.id == id)
				return v;
		}
		return null;
	}

	public HashSet<String> getPartitioningKeys(HashSet<String> referencedEnities, HashSet<String> allowedEdges) {
		HashSet<String> enities = new HashSet<String>();
		for (String e1 : referencedEnities) {

			VisitedNodes<T> visitor = new VisitedNodes<T>();
			for (Vertex<T> vertix : this.verticies) {
				vertix.clearMark();
			}
			this.dfsSpanningTree(vertsMap.get(e1), visitor, allowedEdges);

			boolean allExist = true;
			for (String e2 : referencedEnities) {
				if (!visitor.nodes.contains(e2)) {
					allExist = false;
					break;

				}
			}
			if (allExist)
				enities.add("E-" + e1);

		}
		return enities;
	}

	/**
	 * Insert a directed, weighted Edge<T> into the graph.
	 * 
	 * @param from
	 *            - the Edge<T> starting vertex
	 * @param to
	 *            - the Edge<T> ending vertex
	 * @param cost
	 *            - the Edge<T> weight/cost
	 * @return true if the Edge<T> was added, false if from already has this Edge<T>
	 * @throws IllegalArgumentException
	 *             if from/to are not verticies in the graph
	 */
	public boolean addEdge(Vertex<T> from, Vertex<T> to, String name) throws IllegalArgumentException {
		if (verticies.contains(from) == false)
			throw new IllegalArgumentException("from is not in graph");
		if (verticies.contains(to) == false)
			throw new IllegalArgumentException("to is not in graph");

		Edge<T> e = new Edge<T>(from, to, name);
		// if (edgesNames.contains(e.name))
		// throw new IllegalArgumentException("Edge with the same name exist!");
		// if (from.findEdge(to) != null)
		// return false;
		// else {
		from.addEdge(e);
		to.addEdge(e);
		edges.add(e);
		edgesNames.add(e.name);
		return true;
		// }
	}

	/**
	 * Insert a bidirectional Edge<T> in the graph
	 * 
	 * @param from
	 *            - the Edge<T> starting vertex
	 * @param to
	 *            - the Edge<T> ending vertex
	 * @param cost
	 *            - the Edge<T> weight/cost
	 * @return true if edges between both nodes were added, false otherwise
	 * @throws IllegalArgumentException
	 *             if from/to are not verticies in the graph
	 */
	public boolean insertBiEdge(Vertex<T> from, Vertex<T> to, String name) throws IllegalArgumentException {
		return addEdge(from, to, name) && addEdge(to, from, name);
	}

	/**
	 * Get the graph edges
	 * 
	 * @return the graph edges
	 */
	public List<Edge<T>> getEdges() {
		return this.edges;
	}

	/**
	 * Remove a vertex from the graph
	 * 
	 * @param v
	 *            the Vertex to remove
	 * @return true if the Vertex was removed
	 */
	public boolean removeVertex(Vertex<T> v) {
		if (!verticies.contains(v))
			return false;

		verticies.remove(v);
		if (v == rootVertex)
			rootVertex = null;

		// Remove the edges associated with v
		for (int n = 0; n < v.getOutgoingEdgeCount(); n++) {
			Edge<T> e = v.getOutgoingEdge(n);
			v.remove(e);
			Vertex<T> to = e.getTo();
			to.remove(e);
			edges.remove(e);
		}
		for (int n = 0; n < v.getIncomingEdgeCount(); n++) {
			Edge<T> e = v.getIncomingEdge(n);
			v.remove(e);
			Vertex<T> predecessor = e.getFrom();
			predecessor.remove(e);
		}
		return true;
	}

	/**
	 * Remove an Edge<T> from the graph
	 * 
	 * @param from
	 *            - the Edge<T> starting vertex
	 * @param to
	 *            - the Edge<T> ending vertex
	 * @return true if the Edge<T> exists, false otherwise
	 */
	public boolean removeEdge(Vertex<T> from, Vertex<T> to) {
		Edge<T> e = from.findEdge(to);
		if (e == null)
			return false;
		else {
			from.remove(e);
			to.remove(e);
			edges.remove(e);
			return true;
		}
	}

	/**
	 * Clear the mark state of all verticies in the graph by calling clearMark() on all verticies.
	 * 
	 * @see Vertex#clearMark()
	 */
	public void clearMark() {
		for (Vertex<T> w : verticies)
			w.clearMark();
	}

	/**
	 * Clear the mark state of all edges in the graph by calling clearMark() on all edges.
	 */
	public void clearEdges() {
		for (Edge<T> e : edges)
			e.clearMark();
	}

	/**
	 * Perform a depth first serach using recursion.
	 * 
	 * @param v
	 *            - the Vertex to start the search from
	 * @param visitor
	 *            - the vistor to inform prior to
	 * @see Visitor#visit(Graph, Vertex)
	 */
	public void depthFirstSearch(Vertex<T> v, final Visitor<T> visitor) {
		VisitorEX<T, RuntimeException> wrapper = new VisitorEX<T, RuntimeException>() {
			public void visit(Graph<T> g, Vertex<T> v) throws RuntimeException {
				if (visitor != null)
					visitor.visit(g, v);
			}
		};
		this.depthFirstSearch(v, wrapper);
	}

	/**
	 * Perform a depth first serach using recursion. The search may be cut short if the visitor throws an exception.
	 * 
	 * @param <E>
	 * 
	 * @param v
	 *            - the Vertex to start the search from
	 * @param visitor
	 *            - the vistor to inform prior to
	 * @see Visitor#visit(Graph, Vertex)
	 * @throws E
	 *             if visitor.visit throws an exception
	 */
	public <E extends Exception> void depthFirstSearch(Vertex<T> v, VisitorEX<T, E> visitor) throws E {
		if (visitor != null)
			visitor.visit(this, v);
		v.visit();
		for (int i = 0; i < v.getOutgoingEdgeCount(); i++) {
			Edge<T> e = v.getOutgoingEdge(i);
			if (!e.getTo().visited()) {
				depthFirstSearch(e.getTo(), visitor);
			}
		}
	}

	/**
	 * Perform a breadth first search of this graph, starting at v.
	 * 
	 * @param v
	 *            - the search starting point
	 * @param visitor
	 *            - the vistor whose vist method is called prior to visting a vertex.
	 */
	public void breadthFirstSearch(Vertex<T> v, final Visitor<T> visitor) {
		VisitorEX<T, RuntimeException> wrapper = new VisitorEX<T, RuntimeException>() {
			public void visit(Graph<T> g, Vertex<T> v) throws RuntimeException {
				if (visitor != null)
					visitor.visit(g, v);
			}
		};
		this.breadthFirstSearch(v, wrapper);
	}

	/**
	 * Perform a breadth first search of this graph, starting at v. The vist may be cut short if visitor throws an exception during a vist callback.
	 * 
	 * @param <E>
	 * 
	 * @param v
	 *            - the search starting point
	 * @param visitor
	 *            - the vistor whose vist method is called prior to visting a vertex.
	 * @throws E
	 *             if vistor.visit throws an exception
	 */
	public <E extends Exception> void breadthFirstSearch(Vertex<T> v, VisitorEX<T, E> visitor) throws E {
		LinkedList<Vertex<T>> q = new LinkedList<Vertex<T>>();

		q.add(v);
		if (visitor != null)
			visitor.visit(this, v);
		v.visit();
		while (q.isEmpty() == false) {
			v = q.removeFirst();
			for (int i = 0; i < v.getOutgoingEdgeCount(); i++) {
				Edge<T> e = v.getOutgoingEdge(i);
				Vertex<T> to = e.getTo();
				if (!to.visited()) {
					q.add(to);
					if (visitor != null)
						visitor.visit(this, to);
					to.visit();
				}
			}
		}
	}

	/**
	 * Find the spanning tree using a DFS starting from v.
	 * 
	 * @param v
	 *            - the vertex to start the search from
	 * @param visitor
	 *            - visitor invoked after each vertex is visited and an edge is added to the tree.
	 */
	public void dfsSpanningTree(Vertex<T> v, DFSVisitor<T> visitor, HashSet<String> allowedEdges) {
		v.visit();
		if (visitor != null)
			visitor.visit(this, v);

		for (int i = 0; i < v.getOutgoingEdgeCount(); i++) {
			Edge<T> e = v.getOutgoingEdge(i);
			if (!e.getTo().visited() && allowedEdges.contains(e.name)) {
				if (visitor != null)
					visitor.visit(this, v, e, allowedEdges);
				e.mark();
				dfsSpanningTree(e.getTo(), visitor, allowedEdges);
			}
		}
	}

	public boolean validateTree() {
		if (isExistNodeWithMultiParents()) {
			return false;
		}
		LinkedList<Vertex<T>> path = null;
		if ((path = findCycle()) != null) {
			System.out.println("Found cycle: ");
			printPath(path);
			return false;
		}
		if (getRoot() == null) {
			System.out.println("No root was found");
			return false;
		}
		return true;
	}

	private void printPath(LinkedList<Vertex<T>> path) {
		for (Vertex<T> v : path) {
			System.out.print(v.name + ", ");
		}
		System.out.println();
	}

	public Vertex<T> getRoot() {
		for (Vertex<T> v : this.verticies) {
			clearMark();
			if (v.getIncomingEdgeCount() == 0) {
				if (isRoot(v))
					return v;
			}
		}
		return null;
	}

	private boolean isRoot(Vertex<T> root) {
		Queue<Vertex<T>> q = new LinkedList<Vertex<T>>();
		q.add(root);
		while (!q.isEmpty()) {
			Vertex<T> v = q.poll();
			if (!v.visited()) {
				v.mark();
				for (int i = 0; i < v.getOutgoingEdgeCount(); i++) {
					q.add(v.getOutgoingEdge(i).getTo());
				}
			}
		}
		for (Vertex<T> v : this.verticies) {
			if (!v.visited())
				return false;
		}
		return true;
	}

	private boolean isExistNodeWithMultiParents() {
		for (Vertex<T> v : this.verticies) {
			if (v.getIncomingEdgeCount() > 1) {
				Vertex<T> v1 = v.getIncomingEdge(0).getFrom();
				for (int i = 1; i < v.getIncomingEdgeCount(); i++) {
					if (v1 != v.getIncomingEdge(i).getFrom()) {
						System.out.println("Vertex \"" + v.name + "\" has multiple parents.");
						return true;
					}
				}
			}
		}
		return false;
	}

	public LinkedList<Vertex<T>> findCycle() {
		for (Vertex<T> v : this.verticies) {
			for (int i = 0; i < v.getOutgoingEdgeCount(); i++) {
				LinkedList<Vertex<T>> list = findCycle_rec(v, v.getOutgoingEdge(i).getTo());
				if (list != null) {
					list.addFirst(v);
					return list;
				}
			}
		}
		return null;
	}

	private LinkedList<Vertex<T>> findCycle_rec(Vertex<T> startNode, Vertex<T> currentNode) {
		if (currentNode.getOutgoingEdgeCount() == 0)
			return null;
		if (currentNode == startNode) {
			LinkedList<Vertex<T>> list = new LinkedList<Vertex<T>>();
			list.add(currentNode);
			return list;
		}
		for (int i = 0; i < currentNode.getOutgoingEdgeCount(); i++) {
			LinkedList<Vertex<T>> list = findCycle_rec(startNode, currentNode.getOutgoingEdge(i).getTo());
			if (list != null) {
				list.addFirst(currentNode);
				return list;
			}
		}
		return null;
	}

	/**
	 * Search the verticies for one with name.
	 * 
	 * @param name
	 *            - the vertex name
	 * @return the first vertex with a matching name, null if no matches are found
	 */
	public Vertex<T> findVertexByName(String name) {
		Vertex<T> match = null;
		for (Vertex<T> v : verticies) {
			if (name.equals(v.getName())) {
				match = v;
				break;
			}
		}
		return match;
	}

	/**
	 * Search the verticies for one with data.
	 * 
	 * @param data
	 *            - the vertex data to match
	 * @param compare
	 *            - the comparator to perform the match
	 * @return the first vertex with a matching data, null if no matches are found
	 */
	public Vertex<T> findVertexByData(T data, Comparator<T> compare) {
		Vertex<T> match = null;
		for (Vertex<T> v : verticies) {
			if (compare.compare(data, v.getData()) == 0) {
				match = v;
				break;
			}
		}
		return match;
	}

	/**
	 * Search the graph for cycles. In order to detect cycles, we use a modified depth first search called a colored DFS. All nodes are initially marked white. When a node is encountered, it is marked
	 * grey, and when its descendants are completely visited, it is marked black. If a grey node is ever encountered, then there is a cycle.
	 * 
	 * @return the edges that form cycles in the graph. The array will be empty if there are no cycles.
	 */
	public Edge<T>[] findCycles() {
		ArrayList<Edge<T>> cycleEdges = new ArrayList<Edge<T>>();
		// Mark all verticies as white
		for (int n = 0; n < verticies.size(); n++) {
			Vertex<T> v = getVertex(n);
			v.setMarkState(VISIT_COLOR_WHITE);
		}
		for (int n = 0; n < verticies.size(); n++) {
			Vertex<T> v = getVertex(n);
			visit(v, cycleEdges);
		}

		Edge<T>[] cycles = new Edge[cycleEdges.size()];
		cycleEdges.toArray(cycles);
		return cycles;
	}

	private void visit(Vertex<T> v, ArrayList<Edge<T>> cycleEdges) {
		v.setMarkState(VISIT_COLOR_GREY);
		int count = v.getOutgoingEdgeCount();
		for (int n = 0; n < count; n++) {
			Edge<T> e = v.getOutgoingEdge(n);
			Vertex<T> u = e.getTo();
			if (u.getMarkState() == VISIT_COLOR_GREY) {
				// A cycle Edge<T>
				cycleEdges.add(e);
			} else if (u.getMarkState() == VISIT_COLOR_WHITE) {
				visit(u, cycleEdges);
			}
		}
		v.setMarkState(VISIT_COLOR_BLACK);
	}

	public String toString() {
		StringBuffer tmp = new StringBuffer("Graph[");
		for (Vertex<T> v : verticies)
			tmp.append(v);
		tmp.append(']');
		return tmp.toString();
	}

}

/*
 * JBoss, Home of Professional Open Source Copyright 2006, Red Hat Middleware LLC, and individual contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A directed, weighted edge in a graph
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 * @param <T>
 */
class Edge<T> {
	private Vertex<T> from;

	private Vertex<T> to;

	private int cost;
	public String name;
	private boolean mark;

	/**
	 * Create a zero cost edge between from and to
	 * 
	 * @param from
	 *            the starting vertex
	 * @param to
	 *            the ending vertex
	 */
	public Edge(Vertex<T> from, Vertex<T> to, String name) {

		this(from, to, 0);
		this.name = name;
	}

	/**
	 * Create an edge between from and to with the given cost.
	 * 
	 * @param from
	 *            the starting vertex
	 * @param to
	 *            the ending vertex
	 * @param cost
	 *            the cost of the edge
	 */
	public Edge(Vertex<T> from, Vertex<T> to, int cost) {
		this.from = from;
		this.to = to;
		this.cost = cost;
		mark = false;
	}

	/**
	 * Get the ending vertex
	 * 
	 * @return ending vertex
	 */
	public Vertex<T> getTo() {
		return to;
	}

	/**
	 * Get the starting vertex
	 * 
	 * @return starting vertex
	 */
	public Vertex<T> getFrom() {
		return from;
	}

	/**
	 * Get the cost of the edge
	 * 
	 * @return cost of the edge
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Set the mark flag of the edge
	 * 
	 */
	public void mark() {
		mark = true;
	}

	/**
	 * Clear the edge mark flag
	 * 
	 */
	public void clearMark() {
		mark = false;
	}

	/**
	 * Get the edge mark flag
	 * 
	 * @return edge mark flag
	 */
	public boolean isMarked() {
		return mark;
	}

	/**
	 * String rep of edge
	 * 
	 * @return string rep with from/to vertex names and cost
	 */
	public String toString() {
		StringBuffer tmp = new StringBuffer("Edge[from: ");
		tmp.append(from.getName());
		tmp.append(",to: ");
		tmp.append(to.getName());
		tmp.append(", cost: ");
		tmp.append(cost);
		tmp.append("]");
		return tmp.toString();
	}
}

/*
 * JBoss, Home of Professional Open Source Copyright 2006, Red Hat Middleware LLC, and individual contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A named graph vertex with optional data.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 * @param <T>
 */
@SuppressWarnings("unchecked")
class Vertex<T> {
	private List<Edge<T>> incomingEdges;

	private List<Edge<T>> outgoingEdges;

	public String name;
	public int id;

	private boolean mark;

	private int markState;

	private T data;

	/**
	 * Calls this(null, null).
	 */
	public Vertex() {
		this(-1, null, null);
	}

	public Vertex<T> getCopy() {
		Vertex<T> v = new Vertex<T>(id, name, data);
		// v.incomingEdges.addAll(incomingEdges);
		// v.outgoingEdges.addAll(outgoingEdges);
		v.mark = mark;
		v.markState = markState;
		return v;
	}

	/**
	 * Create a vertex with the given name and no data
	 * 
	 * @param n
	 */
	public Vertex(int id, String n) {
		this(id, n, null);
	}

	/**
	 * Create a Vertex with name n and given data
	 * 
	 * @param n
	 *            - name of vertex
	 * @param data
	 *            - data associated with vertex
	 */
	public Vertex(int id, String n, T data) {
		this.id = id;
		incomingEdges = new ArrayList<Edge<T>>();
		outgoingEdges = new ArrayList<Edge<T>>();
		name = n;
		mark = false;
		this.data = data;
	}

	/**
	 * @return the possibly null name of the vertex
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the possibly null data of the vertex
	 */
	public T getData() {
		return this.data;
	}

	/**
	 * @param data
	 *            The data to set.
	 */
	public void setData(T data) {
		this.data = data;
	}

	/**
	 * Add an edge to the vertex. If edge.from is this vertex, its an outgoing edge. If edge.to is this vertex, its an incoming edge. If neither from or to is this vertex, the edge is not added.
	 * 
	 * @param e
	 *            - the edge to add
	 * @return true if the edge was added, false otherwise
	 */
	public boolean addEdge(Edge<T> e) {
		if (e.getFrom() == this)
			outgoingEdges.add(e);
		else if (e.getTo() == this)
			incomingEdges.add(e);
		else
			return false;
		return true;
	}

	/**
	 * Add an outgoing edge ending at to.
	 * 
	 * @param to
	 *            - the destination vertex
	 * @param cost
	 *            the edge cost
	 */
	public void addOutgoingEdge(Vertex<T> to, int cost) {
		Edge<T> out = new Edge<T>(this, to, cost);
		outgoingEdges.add(out);
	}

	/**
	 * Add an incoming edge starting at from
	 * 
	 * @param from
	 *            - the starting vertex
	 * @param cost
	 *            the edge cost
	 */
	public void addIncomingEdge(Vertex<T> from, int cost) {
		Edge<T> out = new Edge<T>(this, from, cost);
		incomingEdges.add(out);
	}

	/**
	 * Check the vertex for either an incoming or outgoing edge mathcing e.
	 * 
	 * @param e
	 *            the edge to check
	 * @return true it has an edge
	 */
	public boolean hasEdge(Edge<T> e) {
		if (e.getFrom() == this)
			return incomingEdges.contains(e);
		else if (e.getTo() == this)
			return outgoingEdges.contains(e);
		else
			return false;
	}

	/**
	 * Remove an edge from this vertex
	 * 
	 * @param e
	 *            - the edge to remove
	 * @return true if the edge was removed, false if the edge was not connected to this vertex
	 */
	public boolean remove(Edge<T> e) {
		if (e.getFrom() == this)
			incomingEdges.remove(e);
		else if (e.getTo() == this)
			outgoingEdges.remove(e);
		else
			return false;
		return true;
	}

	/**
	 * 
	 * @return the count of incoming edges
	 */
	public int getIncomingEdgeCount() {
		return incomingEdges.size();
	}

	/**
	 * Get the ith incoming edge
	 * 
	 * @param i
	 *            the index into incoming edges
	 * @return ith incoming edge
	 */
	public Edge<T> getIncomingEdge(int i) {
		return incomingEdges.get(i);
	}

	/**
	 * Get the incoming edges
	 * 
	 * @return incoming edge list
	 */
	public List getIncomingEdges() {
		return this.incomingEdges;
	}

	/**
	 * 
	 * @return the count of incoming edges
	 */
	public int getOutgoingEdgeCount() {
		return outgoingEdges.size();
	}

	/**
	 * Get the ith outgoing edge
	 * 
	 * @param i
	 *            the index into outgoing edges
	 * @return ith outgoing edge
	 */
	public Edge<T> getOutgoingEdge(int i) {
		return outgoingEdges.get(i);
	}

	/**
	 * Get the outgoing edges
	 * 
	 * @return outgoing edge list
	 */
	public List getOutgoingEdges() {
		return this.outgoingEdges;
	}

	/**
	 * Search the outgoing edges looking for an edge whose's edge.to == dest.
	 * 
	 * @param dest
	 *            the destination
	 * @return the outgoing edge going to dest if one exists, null otherwise.
	 */
	public Edge<T> findEdge(Vertex<T> dest) {
		for (Edge<T> e : outgoingEdges) {
			if (e.getTo() == dest)
				return e;
		}
		return null;
	}

	/**
	 * Search the outgoing edges for a match to e.
	 * 
	 * @param e
	 *            - the edge to check
	 * @return e if its a member of the outgoing edges, null otherwise.
	 */
	public Edge<T> findEdge(Edge<T> e) {
		if (outgoingEdges.contains(e))
			return e;
		else
			return null;
	}

	/**
	 * What is the cost from this vertext to the dest vertex.
	 * 
	 * @param dest
	 *            - the destination vertex.
	 * @return Return Integer.MAX_VALUE if we have no edge to dest, 0 if dest is this vertex, the cost of the outgoing edge otherwise.
	 */
	public int cost(Vertex<T> dest) {
		if (dest == this)
			return 0;

		Edge<T> e = findEdge(dest);
		int cost = Integer.MAX_VALUE;
		if (e != null)
			cost = e.getCost();
		return cost;
	}

	/**
	 * Is there an outgoing edge ending at dest.
	 * 
	 * @param dest
	 *            - the vertex to check
	 * @return true if there is an outgoing edge ending at vertex, false otherwise.
	 */
	public boolean hasEdge(Vertex<T> dest) {
		return (findEdge(dest) != null);
	}

	/**
	 * Has this vertex been marked during a visit
	 * 
	 * @return true is visit has been called
	 */
	public boolean visited() {
		return mark;
	}

	/**
	 * Set the vertex mark flag.
	 * 
	 */
	public void mark() {
		mark = true;
	}

	/**
	 * Set the mark state to state.
	 * 
	 * @param state
	 *            the state
	 */
	public void setMarkState(int state) {
		markState = state;
	}

	/**
	 * Get the mark state value.
	 * 
	 * @return the mark state
	 */
	public int getMarkState() {
		return markState;
	}

	/**
	 * Visit the vertex and set the mark flag to true.
	 * 
	 */
	public void visit() {
		mark();
	}

	/**
	 * Clear the visited mark flag.
	 * 
	 */
	public void clearMark() {
		mark = false;
	}

	/**
	 * @return a string form of the vertex with in and out edges.
	 */
	public String toString() {
		StringBuffer tmp = new StringBuffer("Vertex(");
		tmp.append(name);
		tmp.append(", data=");
		tmp.append(data);
		tmp.append("), in:[");
		for (int i = 0; i < incomingEdges.size(); i++) {
			Edge<T> e = incomingEdges.get(i);
			if (i > 0)
				tmp.append(',');
			tmp.append('{');
			tmp.append(e.getFrom().name);
			tmp.append(',');
			tmp.append(e.getCost());
			tmp.append('}');
		}
		tmp.append("], out:[");
		for (int i = 0; i < outgoingEdges.size(); i++) {
			Edge<T> e = outgoingEdges.get(i);
			if (i > 0)
				tmp.append(',');
			tmp.append('{');
			tmp.append(e.getTo().name);
			tmp.append(',');
			tmp.append(e.getCost());
			tmp.append('}');
		}
		tmp.append(']');
		return tmp.toString();
	}
}

/*
 * JBoss, Home of Professional Open Source Copyright 2006, Red Hat Middleware LLC, and individual contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A graph visitor interface.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 * @param <T>
 */
interface Visitor<T> {
	/**
	 * Called by the graph traversal methods when a vertex is first visited.
	 * 
	 * @param g
	 *            - the graph
	 * @param v
	 *            - the vertex being visited.
	 */
	public void visit(Graph<T> g, Vertex<T> v);
}

/*
 * JBoss, Home of Professional Open Source Copyright 2006, Red Hat Middleware LLC, and individual contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A graph visitor interface that can throw an exception during a visit callback.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 * @param <T>
 * @param <E>
 */
interface VisitorEX<T, E extends Exception> {
	/**
	 * Called by the graph traversal methods when a vertex is first visited.
	 * 
	 * @param g
	 *            - the graph
	 * @param v
	 *            - the vertex being visited.
	 * @throws E
	 *             exception for any error
	 */
	public void visit(Graph<T> g, Vertex<T> v) throws E;
}

/*
 * JBoss, Home of Professional Open Source Copyright 2006, Red Hat Middleware LLC, and individual contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A spanning tree visitor callback interface
 * 
 * @see Graph#dfsSpanningTree(Vertex, DFSVisitor)
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 * @param <T>
 */
interface DFSVisitor<T> {
	/**
	 * Called by the graph traversal methods when a vertex is first visited.
	 * 
	 * @param g
	 *            - the graph
	 * @param v
	 *            - the vertex being visited.
	 */
	public void visit(Graph<T> g, Vertex<T> v);

	/**
	 * Used dfsSpanningTree to notify the visitor of each outgoing edge to an unvisited vertex.
	 * 
	 * @param g
	 *            - the graph
	 * @param v
	 *            - the vertex being visited
	 * @param e
	 *            - the outgoing edge from v
	 * @param allowedEdges
	 */
	public void visit(Graph<T> g, Vertex<T> v, Edge<T> e, HashSet<String> allowedEdges);
}

class EdgeStruct<T> {
	public EdgeStruct(String name, int source, int destenation, String cSource, String cDestenation, T data) {
		this.name = name;
		this.source = source;
		this.destenation = destenation;
		this.cSource = cSource;
		this.cDestenation = cDestenation;
		this.data = data;
	}

	public String name;
	public int source;
	public int destenation;
	public String cSource;
	public String cDestenation;
	public T data;
}