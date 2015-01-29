package explorer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Handles the drawing and the timing for the simulation. Also serves as the entry point */
public class ExplorerGui {

	private JFrame frame;
	private JPanel canvas;


	/** simulation stuff */
	private ExplorerWorld world;
	private List<Explorer> explorers;
	private int step = 0; // how far through are we?
	private int pathLength = 0;

	public ExplorerGui() {
		// get the simulation ready
		world = new ExplorerWorld(14,14);
		explorers = new ArrayList<>();
		explorers.add(new DumbExplorer(world));
		explorers.add(new OptimalExplorer(world));
		explorers.add(new EntropicExplorer(world));

		// set up actual GUI stuff
		canvas = new JPanel() { // this is a bit cheeky, especially the size bit
			@Override
			public void paintComponent(Graphics g) {
				draw((Graphics2D)g);
			}
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,500);
			}
		};

		frame = new JFrame();
		frame.add(canvas);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);


		new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				step();
			}
		}).start();
	}

	/** advance the simulation to the next stage (not necessarily a full timestep) */
	private void step() {
		if (step == 0) {
			System.out.println("~~~~~~~~~~~~~~~~");
			System.out.println("Getting sensor actions");
			// TODO: actually deal with multiple
			for (Explorer e : explorers) {
				int a = e.getSensorAction();
			}
		}
		if (step == 1) {
			System.out.println("~~~~~~~~~~~~~~~~");
			System.out.println("Observing");
			for (Explorer e : explorers) {
				e.observe(world.getObservation(e.getLastSensorAction(), e));
			}
		}
		if (step == 2) {
			System.out.println("~~~~~~~~~~~~~~~~");
			System.out.println("Getting real actions & advancing");

			pathLength++;
			List<Explorer> done = new ArrayList<>();
			for (Explorer e: explorers) {
				 world.advanceState(e.getAction(), e);
				 if (world.targetReached(e)) {
					System.out.println("Success for " + e.toString());
					System.out.println("in " + pathLength + " steps.");
					done.add(e);
				}
			}

			explorers.removeAll(done);

			if (explorers.isEmpty())
				step = 5;
			else
				step = -1;
		}
		step++;
		canvas.repaint();
	}


	/** actually draws what's going on to the window */
	private void draw(Graphics2D g) {
		g.setColor(Color.white);
		g.fill(g.getClip());

		int[][] map = world.getMap();

		// first draw the map
		int cellSize = canvas.getWidth()/map.length;

		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[0].length; y++) {
				switch(map[x][y]) {
				case ExplorerWorld.EMPTY:
					g.setColor(Color.white);
					break;
				case ExplorerWorld.FULL:
					g.setColor(Color.black);
					break;
				case ExplorerWorld.TARGET:
					g.setColor(Color.green);
					break;
				default:
					g.setColor(Color.red);
					break;
				}
				g.fillRect(x*cellSize, y*cellSize, cellSize, cellSize);
			}
		}

		// draw all of our little people
		for (Explorer e : explorers) {
			int[] state = world.getState(e);
			g.setColor(e.getColor());
			g.fillOval(state[0]*cellSize, state[1]*cellSize, cellSize, cellSize);
			int sx=state[0]*cellSize + cellSize/2;
			int sy = state[1]*cellSize + cellSize/2;
			switch(e.getLastSensorAction()) {
			case ExplorerWorld.NORTH:
				sy -= cellSize/2;
				break;
			case ExplorerWorld.SOUTH:
				sy += cellSize/2;
				break;
			case ExplorerWorld.EAST:
				sx += cellSize/2;
				break;
			case ExplorerWorld.WEST:
				sx -= cellSize/2;
				break;
			default:
				break;
			}
			g.setColor(Color.black);
			g.drawLine(sx, sy, state[0]*cellSize+cellSize/2, state[1]*cellSize+cellSize/2);
		}

		if (explorers.size() == 1) { // if there is only one, draw his beliefs (if he has any)
			double[][] bel = explorers.get(0).getBeliefs();
				// draw the chap


				for (int x = 0; x < map.length; x++) {
					for (int y = 0; y < map[0].length; y++) {
						g.setColor(new Color(1.0f,0.0f,0.0f,0.5f));
						g.fillRect((int) (x*cellSize + (1.0-bel[x][y])*(cellSize/2)),
								   (int) (y*cellSize + (1.0-bel[x][y])*(cellSize/2)),
								   (int) (cellSize*bel[x][y]),
								   (int) (cellSize*bel[x][y]));
					}

				}


		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ExplorerGui();
			}
		});
	}

}
