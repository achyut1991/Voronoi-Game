package delaunay;

/*
 * Copyright (c) 2005, 2007 by L. Paul Chew.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/**
 * The Delauany applet.
 * 
 * Creates and displays a Delaunay Triangulation (DT) or a Voronoi Diagram
 * (VoD). Has a main program so it is an application as well as an applet.
 * 
 * @author Paul Chew
 * 
 *         Created July 2005. Derived from an earlier, messier version.
 * 
 *         Modified December 2007. Updated some of the Triangulation methods.
 *         Added the "Colorful" checkbox. Reorganized the interface between
 *         DelaunayAp and DelaunayPanel. Added code to find a Voronoi cell.
 * 
 */
@SuppressWarnings("serial")
public class DelaunayAp extends javax.swing.JApplet implements Runnable,
		ActionListener, MouseListener {

	private boolean debug = true; // Used for debugging
	private Component currentSwitch = null; // Entry-switch that mouse is in

	private static String windowTitle = "Voronoi/Delaunay Window";
	private JRadioButton voronoiButton = new JRadioButton("Voronoi Diagram");
	private JRadioButton delaunayButton = new JRadioButton(
			"Delaunay Triangulation");
	private JButton clearButton = new JButton("Clear");
	private JCheckBox colorfulBox = new JCheckBox("More Colorful");
	private DelaunayPanel delaunayPanel = new DelaunayPanel(this);
	private JLabel circleSwitch = new JLabel("Show Empty Circles");
	private JLabel delaunaySwitch = new JLabel("Show Delaunay Edges");
	private JLabel voronoiSwitch = new JLabel("Show Voronoi Edges");
	private int playerNo;

	/**
	 * Main program (used when run as application instead of applet).
	 */
	public static void main(String[] args) {
		DelaunayAp applet = new DelaunayAp(); // Create applet
		applet.init(); // Applet initialization
		JFrame dWindow = new JFrame(); // Create window
		dWindow.setSize(800, 500); // Set window size
		dWindow.setTitle(windowTitle); // Set window title
		dWindow.setLayout(new BorderLayout()); // Specify layout manager
		dWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Specify closing behavior
		dWindow.add(applet, "Center"); // Place applet into window
		dWindow.setResizable(false); // Set Window non resizable
		dWindow.setVisible(true); // Show the window
	}

	/**
	 * Initialize the applet. As recommended, the actual use of Swing components
	 * takes place in the event-dispatching thread.
	 */
	public void init() {
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (Exception e) {
			System.err.println("Initialization failure");
		}
	}

	/**
	 * Set up the applet's GUI. As recommended, the init method executes this in
	 * the event-dispatching thread.
	 */
	public void run() {
		setLayout(new BorderLayout());

		// Add the button controls
		ButtonGroup group = new ButtonGroup();
		group.add(voronoiButton);
		group.add(delaunayButton);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(voronoiButton);
		buttonPanel.add(delaunayButton);
		buttonPanel.add(clearButton);
		buttonPanel.add(new JLabel("          ")); // Spacing
		buttonPanel.add(colorfulBox);
		this.add(buttonPanel, "North");

		// Add the mouse-entry switches
		JPanel switchPanel = new JPanel();
		switchPanel.add(circleSwitch);
		switchPanel.add(new Label("     ")); // Spacing
		switchPanel.add(delaunaySwitch);
		switchPanel.add(new Label("     ")); // Spacing
		switchPanel.add(voronoiSwitch);
		this.add(switchPanel, "South");

		// Build the delaunay panel
		delaunayPanel.setBackground(Color.gray);
		this.add(delaunayPanel, "Center");

		// Register the listeners
		voronoiButton.addActionListener(this);
		delaunayButton.addActionListener(this);
		clearButton.addActionListener(this);
		colorfulBox.addActionListener(this);
		delaunayPanel.addMouseListener(this);
		circleSwitch.addMouseListener(this);
		delaunaySwitch.addMouseListener(this);
		voronoiSwitch.addMouseListener(this);

		// Set initial playerTurn
		playerNo = 1;

		// Initialize the radio buttons
		voronoiButton.doClick();
	}

	/**
	 * A button has been pressed; redraw the picture.
	 */
	public void actionPerformed(ActionEvent e) {
		if (debug)
			System.out.println(((AbstractButton) e.getSource()).getText());
		if (e.getSource() == clearButton)
			delaunayPanel.clear();
		playerNo = 1;
		delaunayPanel.repaint();
	}

	/**
	 * If entering a mouse-entry switch then redraw the picture.
	 */
	public void mouseEntered(MouseEvent e) {
		currentSwitch = e.getComponent();
		if (currentSwitch instanceof JLabel)
			delaunayPanel.repaint();
		else
			currentSwitch = null;
	}

	/**
	 * If exiting a mouse-entry switch then redraw the picture.
	 */
	public void mouseExited(MouseEvent e) {
		currentSwitch = null;
		if (e.getComponent() instanceof JLabel)
			delaunayPanel.repaint();
	}

	/**
	 * If mouse has been pressed inside the delaunayPanel then add a new site.
	 */
	public void mousePressed(MouseEvent e) {
		if (e.getSource() != delaunayPanel)
			return;
		Pnt point = new Pnt(e.getX(), e.getY());
		Polygon islandPoly = delaunayPanel.getIslandPoly();
		if(islandPoly.contains(e.getX(), e.getY())){
			return;
		}
		point.setPlayerNo(playerNo);
		playerNo = (playerNo == 1) ? 2 : 1;
		if (debug)
			System.out.println("Click " + point);
		delaunayPanel.addSite(point);
		delaunayPanel.repaint();
	}

	/**
	 * Not used, but needed for MouseListener.
	 */
	public void mouseReleased(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	/**
	 * @return true iff the "colorful" box is selected
	 */
	public boolean isColorful() {
		return colorfulBox.isSelected();
	}

	/**
	 * @return true iff doing Voronoi diagram.
	 */
	public boolean isVoronoi() {
		return voronoiButton.isSelected();
	}

	/**
	 * @return true iff within circle switch
	 */
	public boolean showingCircles() {
		return currentSwitch == circleSwitch;
	}

	/**
	 * @return true iff within delaunay switch
	 */
	public boolean showingDelaunay() {
		return currentSwitch == delaunaySwitch;
	}

	/**
	 * @return true iff within voronoi switch
	 */
	public boolean showingVoronoi() {
		return currentSwitch == voronoiSwitch;
	}

}

/**
 * Graphics Panel for DelaunayAp.
 */
@SuppressWarnings("serial")
class DelaunayPanel extends JPanel {

	public static Color voronoiColor = Color.cyan;
	public static Color delaunayColor = Color.green;
	public static int pointRadius = 3;

	private DelaunayAp controller; // Controller for DT
	private Triangulation dt; // Delaunay triangulation
	private Map<Object, Color> colorTable; // Remembers colors for display
	private Triangle initialTriangle; // Initial triangle
	private static int initialSize = 10000; // Size of initial triangle
	private Graphics g; // Stored graphics context
	private Random random = new Random(); // Source of random numbers
	private int xp[] = { 0, 800, 800, 0 };
	private int yp[] = { 0, 0, 600, 600 };
	private Pnt[] fullPolygon = { new Pnt(0, 0), new Pnt(800, 0),
			new Pnt(800, 600), new Pnt(0, 600) };
	private Pnt[] islandPolygon = { new Pnt(351, 148), new Pnt(348, 160),
			new Pnt(346, 170), new Pnt(345, 185), new Pnt(352, 201),
			new Pnt(367, 214), new Pnt(392, 227), new Pnt(422, 235),
			new Pnt(454, 242), new Pnt(484, 231), new Pnt(494, 220),
			new Pnt(506, 198), new Pnt(521, 188), new Pnt(542, 175),
			new Pnt(553, 162), new Pnt(550, 139), new Pnt(540, 117),
			new Pnt(519, 102), new Pnt(498, 93), new Pnt(467, 94),
			new Pnt(456, 87), new Pnt(439, 70), new Pnt(415, 75),
			new Pnt(409, 87), new Pnt(403, 102), new Pnt(398, 106),
			new Pnt(372, 104), new Pnt(363, 114), new Pnt(355, 128) };
	private Color greenTransp = new Color(0.0f, 1.0f, 0.0f, 0.1f);
    private Color blueTransp = new Color(0.0f, 0.0f, 1.0f, 0.1f);
    private Color redTransp = new Color(1.0f, 0.0f, 0.0f, 0.4f);
    private Player player1,player2;

	/**
	 * Create and initialize the DT.
	 */
	public DelaunayPanel(DelaunayAp controller) {
		this.controller = controller;
		player1 = new Player("Player 1");
		player2 = new Player("Player 2");
		initialTriangle = new Triangle(new Pnt(-initialSize, -initialSize),
				new Pnt(initialSize, -initialSize), new Pnt(0, initialSize));
		dt = new Triangulation(initialTriangle);
		colorTable = new HashMap<Object, Color>();
	}

	/**
	 * Add a new site to the DT.
	 * 
	 * @param point
	 *            the site to add
	 */
	public void addSite(Pnt point) {
		dt.delaunayPlace(point);
	}

	/**
	 * Re-initialize the DT.
	 */
	public void clear() {
		dt = new Triangulation(initialTriangle);
	}

	/**
	 * Get the color for the spcified item; generate a new color if necessary.
	 * 
	 * @param item
	 *            we want the color for this item
	 * @return item's color
	 */
	private Color getColor(Object item) {
		if (colorTable.containsKey(item))
			return colorTable.get(item);
		Color color = new Color(Color.HSBtoRGB(random.nextFloat(), 1.0f, 1.0f));
		colorTable.put(item, color);
		return color;
	}

	/* Basic Drawing Methods */

	/**
	 * Draw a point.
	 * 
	 * @param point
	 *            the Pnt to draw
	 */
	public void draw(Pnt point) {
		int r = pointRadius;
		int x = (int) point.coord(0);
		int y = (int) point.coord(1);
		Image img1 = Toolkit.getDefaultToolkit().getImage("oldfish.png");
		Image img2 = Toolkit.getDefaultToolkit().getImage("fishingRed.png");
		g.fillOval(x - r, y - r, r + r, r + r);
		if (point.getPlayerNo() == 1)
			g.drawImage(img1, x - 25, y - 12, null);
		else
			g.drawImage(img2, x - 14, y - 7, null);

	}

	/**
	 * Draw a circle.
	 * 
	 * @param center
	 *            the center of the circle
	 * @param radius
	 *            the circle's radius
	 * @param fillColor
	 *            null implies no fill
	 */
	public void draw(Pnt center, double radius, Color fillColor) {
		int x = (int) center.coord(0);
		int y = (int) center.coord(1);
		int r = (int) radius;
		if (fillColor != null) {
			Color temp = g.getColor();
			g.setColor(fillColor);
			g.fillOval(x - r, y - r, r + r, r + r);
			g.setColor(temp);
		}
		g.drawOval(x - r, y - r, r + r, r + r);
	}

	/**
	 * Draw a polygon.
	 * 
	 * @param polygon
	 *            an array of polygon vertices
	 * @param fillColor
	 *            null implies no fill
	 */
	public double draw(Pnt[] polygon, Color fillColor) {
		double area = 0;
		int[] x = new int[polygon.length];
		int[] y = new int[polygon.length];
		for (int i = 0; i < polygon.length; i++) {
			x[i] = (int) polygon[i].coord(0);
			y[i] = (int) polygon[i].coord(1);
		}
		if (fillColor != null) {
			Color temp = g.getColor();
			g.setColor(fillColor);
			g.fillPolygon(x, y, polygon.length);
			g.setColor(temp);
		}
		area += polygonArea(getintersection(polygon, fullPolygon));
		area -= polygonArea(getintersection(polygon, islandPolygon));
		g.drawPolygon(x, y, polygon.length);
		return area;
	}

	/* Higher Level Drawing Methods */

	/**
	 * Handles painting entire contents of DelaunayPanel. Called automatically;
	 * requested via call to repaint().
	 * 
	 * @param g
	 *            the Graphics context
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		this.g = g;

		// Flood the drawing area with a "background" color
		Color temp = g.getColor();
		/*
		 * if (!controller.isVoronoi()) g.setColor(delaunayColor); else if
		 * (dt.contains(initialTriangle)) g.setColor(this.getBackground()); else
		 * g.setColor(voronoiColor); g.fillRect(0, 0, this.getWidth(),
		 * this.getHeight()); g.setColor(temp);
		 */

		// Set background image
		Image backgroundImage = Toolkit.getDefaultToolkit()
				.getImage("map1.png");
		g.drawImage(backgroundImage, 0, 0, null);
		int[] x = new int[islandPolygon.length];
		int[] y = new int[islandPolygon.length];
		for (int i = 0; i < islandPolygon.length; i++) {
			x[i] = (int) islandPolygon[i].coord(0);
			y[i] = (int) islandPolygon[i].coord(1);
		}
		g.setColor(redTransp);
		g.fillPolygon(x,y,islandPolygon.length);
		g.setColor(temp);
		g.drawPolygon(x, y, islandPolygon.length);

		// If no colors then we can clear the color table
		if (!controller.isColorful())
			colorTable.clear();

		// Draw the appropriate picture
		if (controller.isVoronoi())
			drawAllVoronoi(controller.isColorful(), true);
		else
			drawAllDelaunay(controller.isColorful());

		// Draw any extra info due to the mouse-entry switches
		temp = g.getColor();
		g.setColor(Color.white);
		if (controller.showingCircles())
			drawAllCircles();
		if (controller.showingDelaunay())
			drawAllDelaunay(false);
		if (controller.showingVoronoi())
			drawAllVoronoi(false, false);
		g.setColor(temp);
	}

	/**
	 * Draw all the Delaunay triangles.
	 * 
	 * @param withFill
	 *            true iff drawing Delaunay triangles with fill colors
	 */
	public void drawAllDelaunay(boolean withFill) {
		for (Triangle triangle : dt) {
			Pnt[] vertices = triangle.toArray(new Pnt[0]);
			draw(vertices, withFill ? getColor(triangle) : null);
		}
	}

	/**
	 * Draw all the Voronoi cells.
	 * 
	 * @param withFill
	 *            true iff drawing Voronoi cells with fill colors
	 * @param withSites
	 *            true iff drawing the site for each Voronoi cell
	 */
	public void drawAllVoronoi(boolean withFill, boolean withSites) {
		// Keep track of sites done; no drawing for initial triangles sites
		HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
		player1.resetScore();
		player2.resetScore();
		for (Triangle triangle : dt)
			for (Pnt site : triangle) {
				if (done.contains(site))
					continue;
				done.add(site);
				List<Triangle> list = dt.surroundingTriangles(site, triangle);
				Pnt[] vertices = new Pnt[list.size()];
				int i = 0;
				for (Triangle tri : list)
					vertices[i++] = tri.getCircumcenter();
				Color polyCol;
				if(site.getPlayerNo()==1){
					polyCol = greenTransp;
				}
				else{
					polyCol = blueTransp;
				}
				double area = draw(vertices, polyCol);
				if(site.getPlayerNo()==1){
					player1.updateScore(area);
				}
				else{
					player2.updateScore(area);
				}
				if (withSites)
					draw(site);
				System.out.println("Point (" + site.coord(0) + ","
						+ site.coord(1) + ") with voronoi area = " + area);
			}
		System.out.println("P1Score = " + player1.getScore());
		System.out.println("P2Score = " + player2.getScore());
	}

	/**
	 * Draw all the empty circles (one for each triangle) of the DT.
	 */
	public void drawAllCircles() {
		// Loop through all triangles of the DT
		for (Triangle triangle : dt) {
			// Skip circles involving the initial-triangle vertices
			if (triangle.containsAny(initialTriangle))
				continue;
			Pnt c = triangle.getCircumcenter();
			double radius = c.subtract(triangle.get(0)).magnitude();
			draw(c, radius, null);
		}
	}

	public double polygonArea(Pnt[] polygon) {
		double area = 0;
		if (polygon.length != 0) {
			for (int i = 0; i < polygon.length - 1; i++) {
				area += polygon[i].coord(0) * polygon[i + 1].coord(1)
						- polygon[i + 1].coord(0) * polygon[i].coord(1);
			}
			area += polygon[polygon.length - 1].coord(0) * polygon[0].coord(1)
					- polygon[0].coord(0)
					* polygon[polygon.length - 1].coord(1);
			area /= 2;
		}
		return Math.abs(area);
	}

	public Pnt[] getintersection(Pnt[] polygon1, Pnt[] polygon2) {
		Pnt[] intersectionPolygon = null;
		int[] x1 = new int[polygon1.length];
		int[] y1 = new int[polygon1.length];
		int[] x2 = new int[polygon2.length];
		int[] y2 = new int[polygon2.length];
		for (int i = 0; i < polygon1.length; i++) {
			x1[i] = (int) polygon1[i].coord(0);
			y1[i] = (int) polygon1[i].coord(1);
		}
		for (int i = 0; i < polygon2.length; i++) {
			x2[i] = (int) polygon2[i].coord(0);
			y2[i] = (int) polygon2[i].coord(1);
		}
		Polygon poly1 = new Polygon(x1, y1, polygon1.length);
		Polygon poly2 = new Polygon(x2, y2, polygon2.length);
		Area a1 = new Area(poly1);
		Area a2 = new Area(poly2);
		a1.intersect(a2);
		intersectionPolygon = traversePath(a1);
		// System.out.println("Interpoly : " + intersectionPolygon.length);
		return intersectionPolygon;
	}

	private static Pnt[] traversePath(Shape s) {
		PathIterator pit = s.getPathIterator(null);
		List<Pnt> points = new ArrayList<Pnt>();
		double[] coords = new double[6];
		Pnt temp;
		while (!pit.isDone()) {
			int type = pit.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_MOVETO:
				double x = coords[0];
				double y = coords[1];
				temp = new Pnt(x, y);
				points.add(temp);
				// System.out.printf("MOVETO:  x = %.2f  y = %.2f%n", x, y);
				break;
			case PathIterator.SEG_LINETO:
				x = coords[0];
				y = coords[1];
				temp = new Pnt(x, y);
				points.add(temp);
				// System.out.printf("LINETO: x = %.2f  y = %.2f%n", x, y);
				break;
			case PathIterator.SEG_QUADTO:
				double ctrlx = coords[0];
				double ctrly = coords[1];
				x = coords[2];
				y = coords[3];
				temp = new Pnt(x, y);
				points.add(temp);
				/*
				 * System.out.printf("QUADTO: ctrlx = %5.1f  ctrly = %.1f%n" +
				 * "        x = %5.1f      y = %.1f%n", ctrlx, ctrly, x, y);
				 */
				break;
			case PathIterator.SEG_CUBICTO:
				double ctrlx1 = coords[0];
				double ctrly1 = coords[1];
				double ctrlx2 = coords[2];
				double ctrly2 = coords[3];
				x = coords[4];
				y = coords[5];
				temp = new Pnt(x, y);
				points.add(temp);
				/*
				 * System.out.printf("CUBICTO: ctrlx1 = %5.1f  ctrly1 = %.1f%n"
				 * + "         ctrlx2 = %5.1f  ctrly2 = %.1f%n" +
				 * "         x = %5.1f       y = %.1f%n", ctrlx1, ctrly1,
				 * ctrlx2, ctrly2, x, y);
				 */
				break;
			case PathIterator.SEG_CLOSE:
				// System.out.println("CLOSE");
			}
			pit.next();
		}
		Pnt[] retPoint = new Pnt[points.size()];
		for (int i = 0; i < points.size(); i++) {
			retPoint[i] = points.get(i);
		}
		return retPoint;
	}
	
	public Polygon getIslandPoly() {
		int[] x = new int[islandPolygon.length];
		int[] y = new int[islandPolygon.length];
		for (int i = 0; i < islandPolygon.length; i++) {
			x[i] = (int) islandPolygon[i].coord(0);
			y[i] = (int) islandPolygon[i].coord(1);
		}
		Polygon poly = new Polygon(x,y,islandPolygon.length);
		return poly;
	}

}
