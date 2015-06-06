import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

public class TermProject {
	public static final int CRMIN = 10;
	public static final int CRMAX = 30;
	public static final int AREASIZE = 100;
	public static final int NUMOFNODES = 31;
	
	public static final int CRITICAL_E = 100; // 10% of initial E 1000
	public static final int CRITICAL_ND = 4;
	public static final int ND_VARIATION_RATE = 3;
	public static final int CR_VARIATION_RATE = 2;
	
	public static final int NUMOFCYCLES = 31;
	static Scanner input = new Scanner(System.in);
	
	public static void main(String[] args) throws Exception {
		/*
		 * 1. use a list to store generated nodes
		 * 2. base station is the first in the list and in the center
		 * 3. sensor node has random position (100 * 100 area) and 
		 *    communication range (between 10 and 30)
		 * 4. use the node list to build a directed weighted graph
		 * 5. use fuzzy logic to update node CR
		 * 6. find MST with single drain at base station
		 * 7. find children of the non-BS nodes and calculate their battery consumption
		 */

		ArrayList<Node> list;
		// check how to get input
		System.out.print("Import from a file? (yes/no): ");

		// input from file 
		if(input.nextLine().equalsIgnoreCase("yes")) {
			// get the file name
			System.out.print("Enter the file name: ");
			String fileName = input.nextLine();
			list = inputFromFile(fileName);
		}
		// random generate input
		else {
			list = generateSensors(NUMOFNODES);
			saveToFile(list); // save input to a file
		}
		
		// initial WSN graph
		printNodeList(list);
		Graph graph = new Graph(list);
		printNodeList(list);
		drawGraph(graph, 0);
		
		// a copy of node list for non-fuzzy logic
		ArrayList<Node> nonFuzzylist = new ArrayList<Node>();
		for(Node n: list) // copy node list
			nonFuzzylist.add(new Node(n));
		
		// apply fuzzy logic or not
		System.out.print("Use fuzzy logic or not? (yes or no): ");
		String selection = input.nextLine();
		
		int cycle = 1;
		int sumOfPackets = 0;
		
		// using fuzzy logic to control the WSN connections
		// update in every cycle based on battery and node degree
		if(selection.equalsIgnoreCase("yes")) {
			System.out.print("Choose fuzzy logic configuration (1 or 2): ");
			String str = input.nextLine();
			int selection_FL = Integer.parseInt(str);
			ArrayList<Integer> energy = new ArrayList<Integer>(); // store total energy of each cycle
			energy.add((list.size() - 1) * list.get(1).batteryLevel); // add initial total energy
			while(cycle < NUMOFCYCLES) {
				updateCR(list, selection_FL); // update communication range
				//printNodeList(list);
				Graph curGraph = new Graph(list); // construct new WSN network
				drawGraph(curGraph, cycle); // display WSN in current cycle
				Graph mst = findMST(curGraph); // find the minimum spanning tree leading with base station as single drain
				//System.out.print(mst);
				drawGraph(mst, cycle);
				int numOfPackets = transmit(mst);
				System.out.print(numOfPackets + ", ");
				if(numOfPackets == 0) break;
				sumOfPackets += numOfPackets; // transmit packets to update battery in every node
				energy.add(getEnergy(list)); // save total energy of current cycle
				cycle++;
			}
			new Chart(energy); // draw energy vs cycle
		}
		
		else {
			// result without fuzzy logic
			ArrayList<Integer> energyNonFuzzy = new ArrayList<Integer>(); // store total energy of each cycle
			energyNonFuzzy.add((nonFuzzylist.size() - 1) * nonFuzzylist.get(1).batteryLevel); // add initial total energy		
			
			while(cycle < NUMOFCYCLES) {
				Graph curGraph = new Graph(nonFuzzylist); // construct new WSN network
				drawGraph(curGraph, cycle); // display WSN in current cycle
				Graph mst = findMST(curGraph); // find the minimum spanning tree leading with base station as single drain
				drawGraph(mst, cycle);
				int numOfPackets = transmit(mst);
				System.out.print(numOfPackets + ", ");
				if(numOfPackets == 0) break;
				sumOfPackets += numOfPackets; // transmit packets to update battery in every node
				energyNonFuzzy.add(getEnergy(nonFuzzylist)); // save total energy of current cycle
				cycle++;
			}
			new Chart(energyNonFuzzy); // draw energy vs cycle
		}
		System.out.println();
		System.out.println("Total number of packets received at base station is: " + sumOfPackets);
		input.close();
	}
	
	// read a list of nodes from file
	public static ArrayList<Node> inputFromFile(String fileName) throws Exception {
		ArrayList<Node> list = new ArrayList<Node>();
		// create input stream
		File file = new File(fileName);
		
		// use scanner to read file
		Scanner input = new Scanner(file);
		while(input.hasNextLine()) {
			String oneNode = input.nextLine();
			if(oneNode.length() != 0) {
				Node n = createNode(oneNode);
				list.add(n);
			}
		}
		input.close();
		return list;
	}
	
	// generate a list of random nodes
	public static ArrayList<Node> generateSensors(int num) {
		ArrayList<Node> list = new ArrayList<Node>(num);
		
		// Put base station at index = 0
		list.add(new Node(AREASIZE / 2, AREASIZE / 2, 0));
		
		// Add num - 1 random nodes
		for(int i = 0; i < num - 1; i++) {
			boolean isOverlap = false; // marker of overlapped node
			// generate a new node
			int x = (int)(Math.random() * AREASIZE);
			int y = (int)(Math.random() * AREASIZE);
			int cr = (int)(Math.random() * (CRMAX - CRMIN) + CRMIN); // initial communication range
			Node n = new Node(x, y, cr);
			
			// check for overlapped nodes
			// if so, re-generate
			for(Node e: list) {
				if(e.x == n.x && e.y == n.y) {
					i--;
					isOverlap = true;
					break;
				}
			}
			
			if(!isOverlap)
				list.add(n);
		}
		
		return list;
	}
	
	// generate a node from a string
	public static Node createNode(String str) {
		String[] arr = str.split("\\,\\s");
		int x = Integer.parseInt(arr[0]);
		int y = Integer.parseInt(arr[1]);
		int cr = Integer.parseInt(arr[2]);
		return new Node(x, y, cr);
	}
	
	// save list of nodes to a file
	public static void saveToFile(ArrayList<Node> list) throws Exception {
		// get the current time to name the file
		SimpleDateFormat dateFormat = new SimpleDateFormat("ssSSS");
		String timestamp = dateFormat.format(new Date());
		String filename = timestamp + ".txt";
		
		// output the list of node to file
		PrintWriter out = new PrintWriter(filename);
		for(Node n: list)
			out.println(n.x + ", " + n.y + ", " + n.communicationRange);
		out.close();
	}
	
	// Draw graph
	public static void drawGraph(Graph graph, int cycle) throws Exception {
		JFrame frame = new JFrame();
		GraphView panel = new GraphView(graph);
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.add(panel, BorderLayout.CENTER);
		
		frame.pack();
		frame.setTitle("WSN network");
		frame.setLocationRelativeTo(null ); // Center the frame
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		// save to images
		BufferedImage image = new BufferedImage(
		frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics gr = image.getGraphics();
		frame.printAll(gr);
		gr.dispose();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("ssSSS");
		String timestamp = dateFormat.format(new Date());
		String filename = cycle + "_" + timestamp + ".png";
		ImageIO.write(image, "PNG", new File(filename));
	}

	// print list of nodes
	public static void printNodeList(ArrayList<Node> list) {
		for(Node n: list) {
			System.out.println(n);
		}
		System.out.println();
	}
	
	// use current battery level and node degree to update node communication range by fuzzy logic
	public static void updateCR(ArrayList<Node> list, int selection) {
		for(int i = 1; i < NUMOFNODES; i++) {
			Node node = list.get(i);
			int ND_desired = getDesiredND(node, selection); // secondary loop of fuzzy logic to update desired node degree
			updateCR(node, ND_desired, selection); // primary loop to update communication range
		}
	}
	
	public static int getDesiredND(Node node, int selection) {
		double e = -(CRITICAL_E - node.batteryLevel) * 1.0 / CRITICAL_E;
		double u = getFuzzyOutput(e, selection);
		int ND_variation = (int)(u * ND_VARIATION_RATE);
		return CRITICAL_ND + ND_variation;
	}
	
	public static void updateCR(Node node, int ND_desired, int selection) {
		double e = (ND_desired - node.nodeDegree) * 1.0 / CRITICAL_ND;
		double u = getFuzzyOutput(e, selection);
		int CR_variation = (int)(u * CR_VARIATION_RATE);
		int newCR = CR_variation + node.communicationRange;
		
		// new CR is saturated between CRmin and CRmax
		if(newCR < CRMIN)
			node.communicationRange = CRMIN;
		else if(newCR > CRMAX)
			node.communicationRange = CRMAX;
		else
			node.communicationRange = newCR;
	}
	
	public static double getFuzzyOutput(double e, int selection) {
		if(selection == 1)
			return fuzzyConfigOne(e);
		else if(selection == 2)
			return fuzzyConfigTwo(e);
		else {
			System.out.println("Error");
			System.exit(0);
			return 0;
		}
	}
	
	public static double fuzzyConfigOne(double e) {
		if(e <= -0.5)
			return -1;
		if(e > -0.5 && e <= 0)
			return 2 * e;
		if(e > 0 && e <= 0.5)
			return 2 * e;
		return 1;
	}
	
	public static double fuzzyConfigTwo(double e) {
		if(e < -1) return -1;
		if(e > 1) return 1;
		return e;
	}
	
	// calculate single drain spanning tree rooted at base station
	public static Graph findMST(Graph graph) {
		Map<Vertex, Integer> distanceMap = new HashMap<Vertex, Integer>();
		Map<Vertex, List<Edge>> incomingMap = new HashMap<Vertex, List<Edge>>(); // incoming edge of a vertex in the tree
		Set<Vertex> visited = new HashSet<Vertex>();
		ArrayList<Vertex> vertices = graph.vertices;
		
		// initialize distance from other vertices to BS vertices
		distanceMap.put(vertices.get(0), 0);
		for(int i = 1; i < vertices.size(); i++)
			distanceMap.put(vertices.get(i), Integer.MAX_VALUE);
		
		// initialize incomingMap for all vertices
		for(Vertex v: vertices)
			incomingMap.put(v, new ArrayList<Edge>());
		
		// put BS in visited list
		visited.add(vertices.get(0));
		
		// find children of all vertices in the shortest path tree
		int minDist;
		Edge minEdge;
		while(true) {
			minDist = Integer.MAX_VALUE;
			minEdge = null;
			
			for(Vertex v: visited) {
				for(Edge fromEdge: v.incomingEdges) {
					if(!visited.contains(fromEdge.from)) {
						int curCost = fromEdge.cost + distanceMap.get(v);
						if(curCost < distanceMap.get(fromEdge.from)) // update neighbor distance
							distanceMap.put(fromEdge.from, curCost);
						if(curCost < minDist) { // find minimum distance neighbor and the edge
							minDist = curCost;
							minEdge = fromEdge;
						}
					}
				}
			}
			if(minEdge == null) break;
			incomingMap.get(minEdge.to).add(minEdge);
			visited.add(minEdge.from);
		}
		
		// construct the graph for the shortest path tree
		ArrayList<Vertex> newVertices = new ArrayList<Vertex>();
		for(Vertex v: vertices) {
			if(!incomingMap.get(v).isEmpty()) {
				v.incomingEdges = incomingMap.get(v);
				v.outgoingEdges = new ArrayList<Edge>();
			}
			else {
				v.incomingEdges = new ArrayList<Edge>();
				v.outgoingEdges = new ArrayList<Edge>();
			}
			newVertices.add(v);
		}
		
		return new Graph(newVertices);
	}
	
	// update each node of its battery
	// calculate total packets transmitted in one cycle using mst
	public static int transmit(Graph mst) {
		int sumOfPackets = 0;
		ArrayList<Vertex> list = mst.vertices;
		for(int i = 0; i < list.size(); i++) {
			Vertex v = list.get(i);
			if(!v.incomingEdges.isEmpty()) {
				for(Edge e: v.incomingEdges) {
					Vertex from = e.from;
					from.data.transmitPackets(getTreeSize(from));
				}
				 // battery consumption includes all incoming packets plus own
				sumOfPackets += v.incomingEdges.size();
			}
		}
		return sumOfPackets;
	}
	
	// calculate number of tree size of a vertex
	public static int getTreeSize(Vertex root) {
		if(root.incomingEdges.isEmpty()) return 1;
		int sum = 1;
		for(Edge e: root.incomingEdges) {
			sum += getTreeSize(e.from);
		}
		return sum;
	}
	
	// calculate total energy of WSN
	public static int getEnergy(ArrayList<Node> list) {
		int sum = 0;
		for(int i = 1; i < list.size(); i++) {
			sum += list.get(i).batteryLevel;
		}
		return sum;
	}
}
