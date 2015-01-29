package explorer;

import java.util.HashMap;
import java.util.Map;
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
	private double observationProbability = 0.99;

	/** Directions: these are the possible actions for both camera and agent */
	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;

	/** The real position of the agent, which is the state of the world. 2 elements: x then y. */
	private Map<Explorer,int[]> state;
	/** Target position -- keep track of this for calculating rewards */
	private int[] target;
	/** starting position -- want it to be the same for all agents so that they can be compared */
	private int[] start;

	/** Random number generator. Can keep the seed the same to test different on the same random maps */
	private Random rand;

	/** Generates a (currently random) world with a randomly placed agent. Needs to know how big */
	public ExplorerWorld(int w, int h) {
		// get the random going
		rand = new Random(0xfadeface/*System.nanoTime()*/);

		target = new int[2];
		map = noiseMap(w,h,0.1, target);


		state = new HashMap<>();

		start = new int[]{rand.nextInt(map.length), rand.nextInt(map[0].length)};
	}

	/** Starts tracking state for an explorer, initially assigning it a random location */
	public void addExplorer(Explorer e) {
		state.put(e, start.clone());
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
			obsX -= 1;
			break;
		case EAST:
			obsX += 1;
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
			// what does it take to be a valid north transition?
			if (newX == oldX && // the X must be the same
			    (oldY-newY == 1 || // and they must either be one apart (in the right direction)
			     (oldY == newY && oldY == 0))) // or be bashing one's head against the wall
				return 1;
			return 0;

		case SOUTH:
			// what about south?
			if (newX == oldX && // the X must be the same
				(newY-oldY == 1 || // and they must either be one apart (in the right direction)
				 (oldY == newY && oldY == map[0].length-1))) // or be bashing one's head against the wall
				return 1;
			return 0;

		case EAST:
			// east (right)
			if (newY == oldY && // can't have movd vertically
				(newX-oldX == 1 || // horizontally one to the right (old should be 1 smaller than new)
				 (oldX == newX && oldX == map.length-1))) // or be bashing one's head against the wall
				return 1;
			return 0;

		case WEST:
			// going left
			if (newY == oldY && // not vertical movement
				(oldX-newX == 1 || // the new one should be a step smaller than the old one
				 (oldX == newX && oldX == 0))) // or be bashing one's head against the wall
				return 1;
			return 0;

		default:
			return Double.NaN; // just in case
		}
	}

	/** generates an observation */
	public int getObservation(int a, Explorer e) {
		int ox = state.get(e)[0];
		int oy = state.get(e)[1]; // coordinates of the square to be observed
		switch(a) {
		case NORTH:
			oy -= 1;
			break;
		case SOUTH:
			oy += 1;
			break;
		case EAST:
			ox += 1;
			break;
		case WEST:
			ox -= 1;
			break;
		default:
			break;
		}


		double r = rand.nextDouble();
		if (ox >= map.length || ox < 0 || oy >= map[0].length || oy < 0) { // the edge is all full
			if (r < observationProbability)
				return FULL;
			if (r < observationProbability + (1-observationProbability)/2)
				return EMPTY;
			return TARGET;
		}
		// otherwise
		if (r < observationProbability)
			return map[ox][oy];
		if (r < observationProbability + (1-observationProbability)/2)
			return wrapIntoRange(map[ox][oy]+1, EMPTY, TARGET);
		return wrapIntoRange(map[ox][oy]+2, EMPTY, TARGET);
	}

	/** Moves the state on given a manipulatory action */
	public void advanceState(int b, Explorer e) { // TODO: stochastic
		switch(b) {
		case NORTH:
			state.get(e)[1] = Math.max(0, state.get(e)[1]-1);
			break;
		case SOUTH:
			state.get(e)[1] = Math.min(state.get(e)[1]+1, map[0].length-1);
			break;
		case EAST:
			state.get(e)[0] = Math.min(state.get(e)[0]+1, map.length-1);
			break;
		case WEST:
			state.get(e)[0] = Math.max(0, state.get(e)[0]-1);
			break;
		default:
			break;
		}
	}

	/** wraps into the range, inclusive of the min and max */
	private int wrapIntoRange(int num, int min, int max) {
		if (num > max)
			return num-min;
		if (num < min)
			return max - (min-num);
		return num;
	}


	/** Returns the map, for the purposes of drawing it */
	public int[][] getMap() {
		return map;
	}

	/** Returns the true state in order to draw it */
	public int[] getState(Explorer e) {
		return state.get(e);
	}

	/** returns NORTH, SOUTH, EAST or WEST as appropriate */
	public String actionToString(int a) {
		switch(a) {
		case WEST:
			return "WEST";
		case EAST:
			return "EAST";
		case NORTH:
			return "NORTH";
		case SOUTH:
			return "SOUTH";
		default:
			return "UNKNOWN ACTION";
		}
	}

	/** has the agent reached the target? */
	public boolean targetReached(Explorer e) {
		return state.get(e)[0] == target[0] && state.get(e)[1] == target[1];
	}
}
