package explorer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private Map<Explorer, Integer> finished;
	private Map<Explorer, List<int[]>> paths;
	private int step = 0; // how far through are we?
	private int pathLength = 0;
	private Timer timer; // just set to 0 for repeats

	public ExplorerGui() {


		// set up actual GUI stuff
		canvas = new JPanel() { // this is a bit cheeky, especially the size bit
			@Override
			public void paintComponent(Graphics g) {
				draw((Graphics2D)g);
			}
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(1000,1000);
			}
		};

		frame = new JFrame();
		frame.add(canvas);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);


		// -1 to just watch a single trial
		setupAndRun(-1);
	}

	private void setupAndRun(int trials) {
		List<Result> results = null;

		if (trials == -1) {
			timer = new Timer(1500, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					step();
				}
			});
		} else {
			results = new ArrayList<>();
			results.add(new Result("DumbExplorer"));
			results.add(new Result("OptimalExplorer"));
			results.add(new Result("EntropicExplorer"));
			results.add(new Result("SurpriseExplorer"));
			results.add(new Result("RandomExplorer"));

		}
		int max = (trials > 0)? trials : 1;
		for (int i = 0; i < max; i++) {
			// get the simulation ready
			world = new ExplorerWorld(15,15);
			explorers = new ArrayList<>();
			//explorers.add(new DumbExplorer(world));
			explorers.add(new OptimalExplorer(world));
			//explorers.add(new EntropicExplorer(world));
			//explorers.add(new SurpriseExplorer(world));
			//explorers.add(new RandomExplorer(world));

			finished = new HashMap<>();
			paths = new HashMap<>();
			for (Explorer e : explorers) {
				paths.put(e, new ArrayList<int[]>());
			}
			for (Explorer e : explorers) {
				paths.get(e).add(world.getState(e).clone());
			}

			if (timer != null)
				timer.start();
			else {
				step = 0;
				pathLength = 0;

				System.out.println("~~~~~~~~~~~~~~~~~~\n~~Start trial: "+i+"~~\n~~~~~~~~~~~~~~~~~~");
				int cycles = 0;
				while(step() && cycles++ < 1000);
				// and collect results
				for (Explorer e : finished.keySet()) {
					int index = 0; // this is kind of gross
					if (e instanceof OptimalExplorer)
						index = 1;
					else if (e instanceof EntropicExplorer)
						index = 2;
					else if (e instanceof SurpriseExplorer)
						index = 3;
					else if (e instanceof RandomExplorer)
						index = 4;

					addResult(results.get(index), finished.get(e));
				}
			}

		}
		if (trials > 0) {
			for (Result r : results) {
				System.out.println(r);
			}
		}
	}

	/** Adds the number to the result object */
	private void addResult(Result r, int num) {
		r.numTrials += 1;
		if (Double.isNaN(r.averagePathLength))
			r.averagePathLength = num;
		else
			r.averagePathLength = (r.averagePathLength + num)/2;
		if (num > r.highestPathLength)
			r.highestPathLength = num;
		if (num < r.lowestPathLength)
			r.lowestPathLength = num;
	}

	/** advance the simulation to the next stage (not necessarily a full timestep) */
	private boolean step() {
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
				 paths.get(e).add(world.getState(e).clone());
				 if (world.targetReached(e)) {
					System.out.println("Success for " + e.toString());
					System.out.println("in " + pathLength + " steps.");
					done.add(e);
				}
			}

			explorers.removeAll(done);
			for (Explorer e : done) {
				finished.put(e, pathLength);
			}

			if (explorers.isEmpty()) {
				step = 5;
				System.out.println("~~~~~~~~~~~~~~~~~");
				System.out.println("~~~~~~~~~~~~~~~~~");
				System.out.println("Results: ");
				for (Explorer e : finished.keySet()) {
					System.out.print("\t");
					System.out.print(e.toString());
					System.out.print(" -- ");
					System.out.print(finished.get(e));
					System.out.println(" steps");
				}
			}
			else
				step = -1;
		}
		step++;
		canvas.repaint();
		return step < 6;
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

		// draw paths
		for (Explorer e : paths.keySet()) {
			g.setColor(e.getColor());
			int[] lastp = null;
			for (int[] p : paths.get(e)) {
				if (lastp != null) {
					g.drawLine(p[0]*cellSize+cellSize/2, p[1]*cellSize+cellSize/2,
							   lastp[0]*cellSize+cellSize/2, lastp[1]*cellSize+cellSize/2);
				}
				lastp = p;
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
			g.setColor(Color.white);
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

	/** just to store some data */
	private static class Result {
		public int numTrials;
		public double averagePathLength;
		public int highestPathLength;
		public int lowestPathLength;

		private String name;

		public Result(String n) {
			numTrials = 0;
			averagePathLength = Double.NaN;
			highestPathLength = Integer.MIN_VALUE;
			lowestPathLength = Integer.MAX_VALUE;

			name = n;
		}

		@Override
		public String toString() {
			return    "~~~"+name+"~~~\n"
					+ "\t" + numTrials + " trials\n"
					+ "\t" + averagePathLength + " average path length\n"
					+ "\t" + highestPathLength + " longest path\n"
					+ "\t" + lowestPathLength + " shortest path\n";
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
