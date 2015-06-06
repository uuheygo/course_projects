class Edge {
	Vertex from;
	Vertex to;
	int cost;
	
	public Edge() {
		from = null;
		to = null;
		cost = 0;
	}
	
	public Edge(Vertex from, Vertex to, int cost) {
		this.from = from;
		this.to = to;
		this.cost = cost;
	}
	
	public String toString() {
		return String.format("[%-3d, %-3d] <--- [%-3d, %-3d]  %d", to.data.x, to.data.y, from.data.x, from.data.y, cost);
	}
}