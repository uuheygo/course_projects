import java.awt.*;

import javax.swing.*;

public class GraphView extends JPanel{
	Graph graph;
	final int SIZE = 600;
	final int R = 10;
	final int SF = SIZE / TermProject.AREASIZE; // scale factor
	
	public GraphView(Graph graph) {
		this.graph = graph; // graph is in 100 * 100 
		this.setPreferredSize(new Dimension(SIZE, SIZE));
		this.setBorder(BorderFactory.createLineBorder(Color.BLUE));
		this.setOpaque(false);
		this.setBackground(Color.RED);
	}
	
	protected void paintComponent(Graphics g) {
		// draw vertices: green-alive, red-dead
		for(Vertex e: graph.vertices) {
			if(e.data.x == TermProject.AREASIZE / 2 && e.data.y ==  TermProject.AREASIZE / 2) { // base station
				g.setColor(Color.BLACK);
				g.fillOval(e.data.x * SF - R / 2, e.data.y * SF - R / 2, R, R);
			}
			else {
				if(e.data.batteryLevel > 0) { // live sensors
					g.setColor(Color.GREEN);
					g.fillOval(e.data.x * SF - R / 2, e.data.y * SF - R /2, R, R);
				}
				else { // dead sensors
					g.setColor(Color.RED);
					g.fillOval(e.data.x * SF - R / 2, e.data.y * SF - R /2, R, R);
				}
			}
		}
		
		// draw edges
		for(Edge e: graph.edges) {
			if(e.from.data.batteryLevel > 0 && e.to.data.batteryLevel > 0) {
				g.setColor(Color.BLACK);
				drawArrow(g, e.from.data.x * SF, e.from.data.y * SF, 
						e.to.data.x * SF, e.to.data.y * SF);
			}
		}
	}
	
	private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
		int d = 6; // width of arrow
		int h = 3; // length of arrow
		int dx = x2 - x1, dy = y2 - y1;
		double D = Math.sqrt(dx*dx + dy*dy);
		double xm = D - d, xn = xm, ym = h, yn = -h, x;
		double sin = dy/D, cos = dx/D;

		x = xm*cos - ym*sin + x1;
		ym = xm*sin + ym*cos + y1;
		xm = x;

		x = xn*cos - yn*sin + x1;
		yn = xn*sin + yn*cos + y1;
		xn = x;

		int[] xpoints = {x2, (int) xm, (int) xn};
		int[] ypoints = {y2, (int) ym, (int) yn};

		g.drawLine(x1, y1, x2, y2);
		g.fillPolygon(xpoints, ypoints, 3);
	}
}
