import java.util.*;

class Vertex {
	List<Edge> incomingEdges;
	List<Edge> outgoingEdges;
	Node data;

	public Vertex(Node data) {
		incomingEdges = new ArrayList();
		outgoingEdges = new ArrayList();
		this.data = data;
	}
	
	// Add edge either incoming or outgoing
	public boolean addEdge(Edge e) {
		if (e.from == this)
			outgoingEdges.add(e);
		else if (e.to == this)
			incomingEdges.add(e);
		else
			return false;
		return true;
	}
}