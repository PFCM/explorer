package explorer;

import java.awt.Color;
import java.util.Random;


public class RandomExplorer implements Explorer {

	private ExplorerWorld world;

	/** Beliefs in one's own position. Kept in a 2D array (x,y) for ease of use */
	private double[][] beliefs;

	/** Possible actions, it is handy to have them in an iterable form */
	private int[] sensorActions = new int[]{ExplorerWorld.NORTH, ExplorerWorld.SOUTH, ExplorerWorld.WEST, ExplorerWorld.EAST};
	private int[] actions = sensorActions;
	/** possible observations (move this list to world? */
	private int[] observations = new int[]{ExplorerWorld.EMPTY, ExplorerWorld.FULL, ExplorerWorld.TARGET};

	/** The last direction we looked */
	private int lastSensorAction;
	/** the last way we went */
	private int lastAction;
	/** the last thing we saw */
	private int lastObservation;

	private Random rand;

	public RandomExplorer(ExplorerWorld world) {
		world.addExplorer(this);
		this.world = world; // have to promise not to cheat
		beliefs = new double[world.getMap().length][world.getMap()[0].length];
		// right know we have no idea at all
		for (int i = 0; i < beliefs.length; i++) {
			for (int j = 0; j < beliefs[0].length; j++) {
				beliefs[i][j] = 1.0/(beliefs.length * beliefs[0].length);
			}
		}

		rand = new Random(0xface);
	}

	@Override
	public int getSensorAction() {
		lastSensorAction = sensorActions[rand.nextInt(sensorActions.length)];
		return lastSensorAction;
	}

	@Override
	public int getAction() {
		int bstar = -1;
		double bstarval = Double.NEGATIVE_INFINITY;
		for (int b : actions) {
			// now we need to look at each possible state transition
			double newStateSum = 0;
			// which means for every possible next state
			for (int newX = 0; newX < beliefs.length; newX++) {
				for (int newY = 0; newY < beliefs[0].length; newY++) {
					int[] newState = new int[]{newX,newY};
					// we need to look at every possible previous state
					double stateSum = 0;
					for (int x = 0; x < beliefs.length; x++) {
						for (int y = 0; y < beliefs[0].length; y++) {
							stateSum += beliefs[x][y] * // belief in (x,y), times
										world.transitionProbability(newX, newY, x, y, b) * // probability of getting into the new state
										world.getReward(newState);
						}
					}
					newStateSum += stateSum;
				}
			}
			if (newStateSum > bstarval) {
				bstar = b;
				bstarval = newStateSum;
			}
		}

		System.out.println("(RandomExplorer) Choosing action: " + world.actionToString(bstar) + " (value = " + bstarval + ")");
		lastAction = bstar;
		advanceBeliefs(bstar);
		return bstar;
	}

	@Override
	public void observe(int y) {
		System.out.println("(RandomExplorer) Observing: " + y);
		beliefs = updateBeliefs(lastSensorAction, y);
	}

	/** Returns the advanced beliefs given a sensor action and an observation. Does not overwrite existing beliefs
	 * as we have to use this for figuring out the best options
	 * @param a - the sensor action
	 * @param obs - the observation
	 * @return
	 */
	private double[][] updateBeliefs(int a, int obs) {
		double[][] newBeliefs = new double[beliefs.length][beliefs[0].length];
		double sum = 0; // for the normalisation
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				newBeliefs[x][y] = beliefs[x][y] * world.observationProbability(obs, a, x, y);
				sum += newBeliefs[x][y];
			}
		}
		if (sum == 0) { // this does happen a few times
			newBeliefs[0][0] = Double.NEGATIVE_INFINITY;
			return newBeliefs;
		}
		// now normalise
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				newBeliefs[x][y] /= sum;
			}
		}
		return newBeliefs;
	}

	/** Advances beliefs according to the specified action (overwrites previous beliefs) */
	private void advanceBeliefs(int b) {
		double sum = 0;
		double[][] newbel = new double[beliefs.length][beliefs[0].length];
		for (int x  =0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				double statesum = 0; // belief in each state is the sum of beliefs in possible previous state times the probability of the transition
				for (int px = 0; px < beliefs.length; px++) {
					for (int py = 0; py < beliefs[0].length; py++) {
						statesum += beliefs[px][py] * world.transitionProbability(x, y, px, py, b);
					}
				}
				newbel[x][y] = statesum;
				sum += newbel[x][y];
			}
		}
		// now normalise them
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				newbel[x][y] /= sum;
			}
		}
		beliefs = newbel;
	}

	@Override
	public int getLastSensorAction() {
		return lastSensorAction;
	}

	public double[][] getBeliefs() {
		return beliefs;
	}

	@Override
	public String toString() {
		return "RandomExplorer";
	}

	@Override
	public Color getColor() {
		return new Color(0.0f,1.0f,0.5f,0.5f);
	}

}
