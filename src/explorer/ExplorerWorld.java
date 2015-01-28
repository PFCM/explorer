package explorer;

import java.util.Random;

/** Represents the world. Generates observations advances the world state,
 *  responds to queries regarding probability of observations etc.
 * @author mathewpaul1
 *
 */
public class ExplorerWorld {

	/** the map of the world. Three possible values, defined below */
	private int[][] map;
	/** Empty square, should show up as white on the visualisation */
	public static final int EMPTY = 0;
	/** Filled square, black on the visualisation */
	public static final int FULL = 1;
	/** Goal! */
	public static final int TARGET = 2;
	/** the probability of making a correct observation. Each possible incorrect observation has a (1-observationProb)/2 chance. */
	private double observationProbability = 1;

	/** Directions: these are the possible actions for both camera and agent */
	public static final int NORTH = 1;
	public static final int SOUTH = 2;
	public static final int EAST = 3;
	public static final int WEST = 4;

	/** The real position of the agent, which is the state of the world. 2 elements: x then y. */
	private int[] state;
	/** Target position -- keep track of this for calculating rewards */
	private int[] target;

	/** Random number generator. Can keep the seed the same to test different on the same random maps */
	private Random rand;

	/** Generates a (currently random) world with a randomly placed agent. Needs to know how big */
	public ExplorerWorld(int w, int h) {
		// get the random going
		rand = new Random(0xface);

		target = new int[2];
		map = noiseMap(w,h,0.3, target);

		// random agent position
		state = new int[2];
		state[0] = rand.nextInt(w);
		state[1] = rand.nextInt(h);
	}

	/**
	 * Makes a new map with randomly filled squares and a random target location.
	 *
	 * @param w - width
	 * @param h - height
	 * @param prob - the probability of any given square being black (except the target, that is chosen at random after having filled all the rest of the squares.
	 * @return
	 */
	private int[][] noiseMap(int w, int h, double prob, int[] target) {
		int[][] map = new int[w][h];

		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++) {
				if (rand.nextDouble() < prob)
					map[i][j] = FULL;
			}

		// finally the target
		target[0] = rand.nextInt(w);
		target[1] = rand.nextInt(h);
		map[target[0]][target[1]] = TARGET;

		return map;
	}

	/** Gets the reward for the given state. Currently this is the negative Manhattan distance in order to pretend that we've learnt a value function */
	public double getReward(int[] state) {
		return -(Math.abs(state[0] - target[0]) + Math.abs(state[1]-target[1]));
	}

	/** returns the probability of a given state + sensor action producing a given observation.
	 * If an observation would look off the edge of the map, it will always be full. */
	public double observationProbability(int y, int a, int agentX, int agentY) {
		int obsX = agentX,obsY = agentY; // coordinates of the square being observed
		switch(a) {
		case NORTH:
			obsY -= 1;
			break;
		case SOUTH:
			obsY += 1;
			break;
		case WEST:
			obsX += 1;
			break;
		case EAST:
			obsX -= 1;
			break;
		default:
			System.err.println("(World) unknown sensor action: " + a);
			return -1;
		}

		if (obsX >= map.length || obsX < 0 || obsY >= map[0].length || obsY < 0) {
			if (y == FULL)
				return observationProbability;
			return (1-observationProbability)/2.0;
		}
		// otherwise we need to check
		if (y == map[obsX][obsY])
			return observationProbability;
		return (1-observationProbability)/2.0;
	}

	/**
	 * Returns the probability of a transition from (oldX,oldY) to (newX,newY) given action was taken.
	 * @param oldX
	 * @param oldY
	 * @param newX
	 * @param newY
	 * @param action
	 * @return
	 */
	public double transitionProbability(int newX, int newY, int oldX, int oldY, int action) {
		switch(action) {
		case NORTH:
			if (newX == oldX && oldY-newY == 1)
				return 1;
			return 0;

		case SOUTH:
			if (newX == oldX && oldY-newY == -1)
				return 1;
			return 0;

		case EAST:
			if (newY == oldY && oldX-newX == -1)
				return 1;
			return 0;

		case WEST:
			if (newY == oldY && oldX-newX == 1)
				return 1;
			return 0;
		default:
			return -1;
		}
	}


	/** Returns the map, for the purposes of drawing it */
	public int[][] getMap() {
		return map;
	}

	/** Returns the true state in order to draw it */
	public int[] getState() {
		return state;
	}
}
