package fishnoi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class DelaunayAp extends javax.swing.JApplet implements Runnable,
		ActionListener, MouseListener {

	private boolean debug = false; 
	private Component currentFocus = null;

	private static String windowTitle = "Fishnoi";
	private JRadioButton voronoiButton = new JRadioButton("Voronoi Diagram");
	private JRadioButton delaunayButton = new JRadioButton(
			"Delaunay Triangulation");
	private JButton map1Button = new JButton("Map 1");
	private JButton map2Button = new JButton("Map 2");
	private JCheckBox colorfulBox = new JCheckBox("More Colorful");
	private DelaunayPanel delaunayPanel;
	private JLabel p1Label = new JLabel("Player1 Score: ");
	private JLabel p2Label = new JLabel("Player2 Score: ");
	public static JLabel p1Score = new JLabel("0");
	public static JLabel p2Score = new JLabel("0");
	private int playerNo;
	private int mapChoice;
	private int turn;

	public static void main(String[] args) {
		DelaunayAp applet = new DelaunayAp(); 
		applet.init(); 
		JFrame dWindow = new JFrame(); 
		dWindow.setSize(800, 500); 
		dWindow.setTitle(windowTitle); 
		dWindow.setLayout(new BorderLayout()); 
		dWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		dWindow.add(applet, "Center"); 
		dWindow.setResizable(false); 
		dWindow.setVisible(true); 
	}

	public void init() {
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (Exception e) {
			System.err.println("Initialization failure");
		}
	}

	public void run() {
		setLayout(new BorderLayout());

		
		ButtonGroup group = new ButtonGroup();
		group.add(voronoiButton);
		group.add(delaunayButton);
		JPanel buttonPanel = new JPanel();

		buttonPanel.add(map1Button);
		buttonPanel.add(new JLabel("          "));
		buttonPanel.add(map2Button);
		this.add(buttonPanel, "North");


		JPanel switchPanel = new JPanel();
		switchPanel.add(p1Label);
		switchPanel.add(p1Score);
		switchPanel.add(new JLabel("             "));
		switchPanel.add(p2Label);
		switchPanel.add(p2Score); 
		this.add(switchPanel, "South");

		mapChoice = 1;
		delaunayPanel = new DelaunayPanel(mapChoice);

		delaunayPanel.setBackground(Color.gray);
		this.add(delaunayPanel, "Center");

		voronoiButton.addActionListener(this);
		delaunayButton.addActionListener(this);
		map1Button.addActionListener(this);
		map2Button.addActionListener(this);
		colorfulBox.addActionListener(this);
		delaunayPanel.addMouseListener(this);

		playerNo = 1;
		turn=0;

		voronoiButton.doClick();
	}

	public void actionPerformed(ActionEvent e) {
		if (debug)
			System.out.println(((AbstractButton) e.getSource()).getText());
		if (e.getSource() == map1Button) {
			delaunayPanel.clear(1);
			mapChoice = 1;
		} else if (e.getSource() == map2Button) {
			delaunayPanel.clear(2);
			mapChoice = 2;
		}
		playerNo = 1;
		turn = 0;
		delaunayPanel.repaint();
	}

	public void mouseEntered(MouseEvent e) {
		currentFocus = e.getComponent();
		if (currentFocus instanceof JLabel)
			delaunayPanel.repaint();
		else
			currentFocus = null;
	}

	public void mouseExited(MouseEvent e) {
		currentFocus = null;
		if (e.getComponent() instanceof JLabel)
			delaunayPanel.repaint();
	}

	public void mousePressed(MouseEvent e) {
		if (e.getSource() != delaunayPanel)
			return;
		Pnt point = new Pnt(e.getX(), e.getY());
		Polygon islandPoly1 = delaunayPanel.getIslandPoly1();
		Polygon islandPoly2 = delaunayPanel.getIslandPoly2();
		if (islandPoly1.contains(e.getX(), e.getY())) {
			return;
		}
		if (islandPoly2.contains(e.getX(), e.getY())) {
			return;
		}
		point.setPlayerNo(playerNo);
		playerNo = (playerNo == 1) ? 2 : 1;
		turn++;
		//System.out.println(turn);
		if(turn==31){
			if(delaunayPanel.player1.getScore() > delaunayPanel.player2.getScore()){
				JOptionPane.showMessageDialog(delaunayPanel, "Oh o! All out of boats mate. \nPlayer 1 caught the most fish!");
			}
			else if(delaunayPanel.player1.getScore() < delaunayPanel.player2.getScore()) {
				JOptionPane.showMessageDialog(delaunayPanel, "Oh o! All out of boats mate. \nPlayer 2 caught the most fish!");
			}
			else{
				JOptionPane.showMessageDialog(delaunayPanel, "Oh o! All out of boats mate. \nDRAW!");
			}
			if(mapChoice==1){
				map1Button.doClick();
				return;
			}
			else {
				map2Button.doClick();
				return;
			}
		}
		if (debug)
			System.out.println("Click " + point);
		delaunayPanel.addSite(point);
		delaunayPanel.repaint();
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	/**
	 * @return true iff doing Voronoi diagram.
	 */
	public boolean isVoronoi() {
		return voronoiButton.isSelected();
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

	private Triangulation dt; // Delaunay triangulation
	private Triangle initialTriangle; // Initial triangle
	private static int initialSize = 10000; // Size of initial triangle
	private Graphics g; // Stored graphics context
	private Pnt[] fullPolygon = { new Pnt(0, 0), new Pnt(800, 0),
			new Pnt(800, 600), new Pnt(0, 600) };

	private Pnt[] islandPolygon1 = { new Pnt(351, 148), new Pnt(348, 160),
			new Pnt(346, 170), new Pnt(345, 185), new Pnt(352, 201),
			new Pnt(367, 214), new Pnt(392, 227), new Pnt(422, 235),
			new Pnt(454, 242), new Pnt(484, 231), new Pnt(494, 220),
			new Pnt(506, 198), new Pnt(521, 188), new Pnt(542, 175),
			new Pnt(553, 162), new Pnt(550, 139), new Pnt(540, 117),
			new Pnt(519, 102), new Pnt(498, 93), new Pnt(467, 94),
			new Pnt(456, 87), new Pnt(439, 70), new Pnt(415, 75),
			new Pnt(409, 87), new Pnt(403, 102), new Pnt(398, 106),
			new Pnt(372, 104), new Pnt(363, 114), new Pnt(355, 128) };

	private Pnt[] islandPolygon2 = { new Pnt(695, 265), new Pnt(704, 269),
			new Pnt(722, 269), new Pnt(737, 265), new Pnt(751, 261),
			new Pnt(766, 253), new Pnt(775, 244), new Pnt(771, 230),
			new Pnt(763, 220), new Pnt(756, 209), new Pnt(757, 192),
			new Pnt(763, 179), new Pnt(759, 172), new Pnt(742, 185),
			new Pnt(731, 191), new Pnt(717, 199), new Pnt(701, 210),
			new Pnt(687, 219), new Pnt(688, 236), new Pnt(691, 251),
			new Pnt(699, 260) };

	private Pnt[] islandPolygon3 = { new Pnt(0, 138), new Pnt(3, 138),
			new Pnt(23, 136), new Pnt(41, 136), new Pnt(59, 136),
			new Pnt(76, 136), new Pnt(89, 133), new Pnt(109, 134),
			new Pnt(124, 133), new Pnt(144, 134), new Pnt(171, 132),
			new Pnt(192, 127), new Pnt(215, 124), new Pnt(237, 124),
			new Pnt(254, 122), new Pnt(285, 119), new Pnt(300, 117),
			new Pnt(317, 114), new Pnt(342, 110), new Pnt(355, 106),
			new Pnt(380, 104), new Pnt(395, 101), new Pnt(422, 96),
			new Pnt(444, 92), new Pnt(463, 84), new Pnt(481, 77),
			new Pnt(503, 69), new Pnt(518, 63), new Pnt(525, 51),
			new Pnt(545, 44), new Pnt(567, 48), new Pnt(593, 53),
			new Pnt(623, 55), new Pnt(638, 50), new Pnt(660, 43),
			new Pnt(672, 31), new Pnt(704, 23), new Pnt(733, 18),
			new Pnt(750, 15), new Pnt(775, 10), new Pnt(793, 1),
			new Pnt(800, 0), new Pnt(0, 0) };

	private Pnt[] islandPolygon4 = { new Pnt(622, 410), new Pnt(622, 400),
			new Pnt(610, 387), new Pnt(604, 377), new Pnt(598, 365),
			new Pnt(596, 349), new Pnt(597, 328), new Pnt(601, 319),
			new Pnt(611, 304), new Pnt(620, 294), new Pnt(635, 280),
			new Pnt(651, 270), new Pnt(667, 262), new Pnt(678, 258),
			new Pnt(693, 249), new Pnt(709, 245), new Pnt(722, 234),
			new Pnt(738, 226), new Pnt(757, 220), new Pnt(773, 217),
			new Pnt(791, 216), new Pnt(800, 216), new Pnt(800, 410) };

	private Color greenTransp = new Color(0.0f, 1.0f, 0.0f, 0.1f);
	private Color blueTransp = new Color(0.0f, 0.0f, 1.0f, 0.1f);
	public Player player1, player2;
	private int mapChoice;

	/**
	 * Create and initialize the DT.
	 */
	public DelaunayPanel(int map) {
		mapChoice = map;
		player1 = new Player("Player 1");
		player2 = new Player("Player 2");
		initialTriangle = new Triangle(new Pnt(-initialSize, -initialSize),
				new Pnt(initialSize, -initialSize), new Pnt(0, initialSize));
		dt = new Triangulation(initialTriangle);
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
	public void clear(int map) {
		dt = new Triangulation(initialTriangle);
		mapChoice = map;
	}


	/**
	 * Draw the boat.
	 * 
	 * @param point
	 *            location of boat
	 */
	public void draw(Pnt point) {
		int r = pointRadius;
		int x = (int) point.coord(0);
		int y = (int) point.coord(1);
		Image img1 = Toolkit.getDefaultToolkit().getImage("oldfish.png");
		Image img2 = Toolkit.getDefaultToolkit().getImage("newfish.png");
		g.fillOval(x - r, y - r, r + r, r + r);
		if (point.getPlayerNo() == 1)
			g.drawImage(img1, x - 25, y - 12, null);
		else
			g.drawImage(img2, x - 25, y - 28, null);

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
		Area intersectionPoly = null;
		
		area += polygonArea(getintersection(polygon, fullPolygon));
		if (mapChoice == 1) {
			area -= polygonArea(getintersection(polygon, islandPolygon1));
			area -= polygonArea(getintersection(polygon, islandPolygon2));
			intersectionPoly = getSubtracts1(polygon, islandPolygon1);
			intersectionPoly = getSubtracts1(intersectionPoly, islandPolygon2);
			
		} else if (mapChoice == 2) {
			area -= polygonArea(getintersection(polygon, islandPolygon3));
			area -= polygonArea(getintersection(polygon, islandPolygon4));
			intersectionPoly = getSubtracts1(polygon, islandPolygon3);
			intersectionPoly = getSubtracts1(intersectionPoly, islandPolygon4);
		}
		

		Graphics2D g2d = (Graphics2D)g;
		
		
		if (fillColor != null) {
			Color temp = g.getColor();
			g.setColor(fillColor);
			g2d.fill(intersectionPoly);
			g.setColor(temp);
		}
		
		g2d.draw(intersectionPoly);
		//g.drawPolygon(x1, y1, intersectionPoly.length);
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
		g.setColor(Color.white);

		if (mapChoice == 1) {
			Image backgroundImage = Toolkit.getDefaultToolkit().getImage(
					"map1.png");
			g.drawImage(backgroundImage, 0, 0, null);
		} else if (mapChoice == 2) {
			Image backgroundImage = Toolkit.getDefaultToolkit().getImage(
					"map2.png");
			g.drawImage(backgroundImage, 0, 0, null);
		}

		drawAllVoronoi(true);

	}



	public void drawAllVoronoi(boolean withSites) {
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
				if (site.getPlayerNo() == 1) {
					polyCol = greenTransp;
				} else {
					polyCol = blueTransp;
				}
				double area = draw(vertices, polyCol);
				if (site.getPlayerNo() == 1) {
					player1.updateScore(area);
				} else {
					player2.updateScore(area);
				}
				if (withSites)
					draw(site);
				/*
				 * System.out.println("Point (" + site.coord(0) + "," +
				 * site.coord(1) + ") with voronoi area = " + area);
				 */
			}
		// System.out.println("P1Score = " + player1.getScore());
		// System.out.println("P2Score = " + player2.getScore());
		DecimalFormat formatter = new DecimalFormat("#0.00");
		DelaunayAp.p1Score.setText("" + formatter.format(player1.getScore()));
		DelaunayAp.p2Score.setText("" + formatter.format(player2.getScore()));

	}

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
	
	public Area getSubtracts1(Pnt[] polygon1, Pnt[] polygon2) {
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
		a1.subtract(a2);
		return a1;
	}
	
	public Area getSubtracts1(Area a1, Pnt[] polygon2) {
		int[] x2 = new int[polygon2.length];
		int[] y2 = new int[polygon2.length];
		for (int i = 0; i < polygon2.length; i++) {
			x2[i] = (int) polygon2[i].coord(0);
			y2[i] = (int) polygon2[i].coord(1);
		}
		Polygon poly2 = new Polygon(x2, y2, polygon2.length);
		Area a2 = new Area(poly2);
		a1.subtract(a2);
		//intersectionPolygon = traversePath(a1);
		// System.out.println("Interpoly : " + intersectionPolygon.length);
		return a1;
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

	public Polygon getIslandPoly1() {
		Polygon poly = null;
		if (mapChoice == 1) {
			int[] x = new int[islandPolygon1.length];
			int[] y = new int[islandPolygon1.length];
			for (int i = 0; i < islandPolygon1.length; i++) {
				x[i] = (int) islandPolygon1[i].coord(0);
				y[i] = (int) islandPolygon1[i].coord(1);
			}
			poly = new Polygon(x, y, islandPolygon1.length);
		} else if (mapChoice == 2) {
			int[] x = new int[islandPolygon3.length];
			int[] y = new int[islandPolygon3.length];
			for (int i = 0; i < islandPolygon3.length; i++) {
				x[i] = (int) islandPolygon3[i].coord(0);
				y[i] = (int) islandPolygon3[i].coord(1);
			}
			poly = new Polygon(x, y, islandPolygon3.length);
		}
		return poly;
	}

	public Polygon getIslandPoly2() {
		Polygon poly = null;
		if (mapChoice == 1) {
			int[] x = new int[islandPolygon2.length];
			int[] y = new int[islandPolygon2.length];
			for (int i = 0; i < islandPolygon2.length; i++) {
				x[i] = (int) islandPolygon2[i].coord(0);
				y[i] = (int) islandPolygon2[i].coord(1);
			}
			poly = new Polygon(x, y, islandPolygon2.length);
		} else if (mapChoice == 2) {
			int[] x = new int[islandPolygon4.length];
			int[] y = new int[islandPolygon4.length];
			for (int i = 0; i < islandPolygon4.length; i++) {
				x[i] = (int) islandPolygon4[i].coord(0);
				y[i] = (int) islandPolygon4[i].coord(1);
			}
			poly = new Polygon(x, y, islandPolygon4.length);
		}
		return poly;
	}

}
