import java.util.*;

import javax.swing.*;

public class Graph extends JPanel{
	ArrayList<Vertex> vertices; // List of vertices
	ArrayList<Edge> edges; // List of edges

	public Graph(List<Node> list) {
		vertices = new ArrayList<Vertex>();
		edges = new ArrayList<Edge>();
		generateGraph(list);
	}
	
	public Graph(ArrayList<Vertex> vertices) {
		this.vertices = vertices;
		edges = new ArrayList<Edge>();
		for(Vertex v: vertices) {
			edges.addAll(v.incomingEdges);
		}
	}
	
	private void generateGraph(List<Node> list) {
		// initialize vertices
		for(Node n : list)
			vertices.add(new Vertex(n));
		
		// add edges to every vertex and edges list
		for(Vertex from : vertices) {
			for(Vertex to : vertices) {
				if(from.data.batteryLevel > 0 && to.data.batteryLevel > 0 && 
						from != to && canReach(from, to)) {
					Edge edge = new Edge(from, to, from.data.communicationRange);
					from.addEdge(edge);
					to.addEdge(edge);
					edges.add(edge);
				}
			}
		}
		
		// initialize node degree
		for(Vertex v: vertices) {
			v.data.nodeDegree = v.outgoingEdges.size();
		}
	}
	
	// check if to is in the communication range of from
	private boolean canReach(Vertex from, Vertex to) {
		int distanceSq = (int) (Math.pow(from.data.x - to.data.x, 2) 
				+ Math.pow(from.data.y - to.data.y, 2));
		return (int) (Math.pow(from.data.communicationRange, 2)) >= distanceSq;
	}
	
	public String toString() {
		String str = "";
		for(Vertex v: vertices) {
			for(Edge e: v.incomingEdges)
				str += e.toString() + "\n";
		}
		return str;
	}
}
