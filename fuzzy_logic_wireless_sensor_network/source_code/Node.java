
public class Node {
	int x; // x coordinate
	int y; // y coordinate
	int batteryLevel;
	int nodeDegree;
	int communicationRange;
	
	public Node(int x, int y, int communicationRange) {
		this.batteryLevel = 1000; // initial battery is full, 1000
		this.x = x;
		this.y = y;
		this.communicationRange = communicationRange;
	}
	
	public Node(Node n) {
		this.x = n.x;
		this.y = n.y;
		this.batteryLevel = n.batteryLevel;
		this.nodeDegree = n.nodeDegree;
		this.communicationRange = n.communicationRange;
	}
	
	// Battery consumption of transmission of one packets is 
	// proportional to square of communication range
	public void transmitPackets(int numOfPacket) {
		batteryLevel -= numOfPacket * communicationRange * communicationRange / 10;
	}
	
	public String toString() {
		return String.format("[x = %-5dy = %-5dcr = %-5dnd = %-2d]", 
				x, y, communicationRange, nodeDegree);
//		return "[x = " + x + ",\ty = " +  y + 
//				",\tcr = " + communicationRange + ",\tnd = " + nodeDegree + "]";
	}
}
