package explorer;

import java.awt.Color;

/**
 * Explores the world always choosing actions that maximise the expected value for the next state transition.
 * @author mathewpaul1
 *
 */
public class OptimalExplorer implements Explorer {

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

	/** Our table for precomputing good actions */
	private Action[][] opt;

	private boolean useFast = true;

	public OptimalExplorer(ExplorerWorld world) {
		world.addExplorer(this);
		this.world = world; // have to promise not to cheat
		beliefs = new double[world.getMap().length][world.getMap()[0].length];
		opt = new Action[sensorActions.length][observations.length];
		// right know we have no idea at all
		for (int i = 0; i < beliefs.length; i++) {
			for (int j = 0; j < beliefs[0].length; j++) {
				beliefs[i][j] = 1.0/(beliefs.length * beliefs[0].length);
			}
		}
	}

	/** choose whether it is fast or slow, mainly to test that they behave the same */
	public OptimalExplorer(ExplorerWorld world, boolean fast) {
		this(world);
		useFast = fast;
	}

	@Override
	public int getSensorAction() {
		if (useFast)
			return getSensorActionQuicker();
		return getSensorActionSlowly();
	}

	/** precomputes a table then figures out the best way to look */
	private int getSensorActionQuicker() {
		precompBStar(); // now we should have a table
		// still need to look at each possible sensor action
		double astarval = Double.NEGATIVE_INFINITY;
		int astar = -1;
		for (int a : sensorActions) {
			// and sum over the possible observations
			double ysum = 0;
			for(int y : observations) {
				// ...and the possible states of the world
				double xsum = 0;
				for (int ax = 0; ax < beliefs.length; ax++) {
					for (int ay = 0; ay < beliefs[0].length; ay++) {
						xsum += world.observationProbability(y, a, ax, ay) * beliefs[ax][ay] * opt[a][y].value;
					}
				}
				ysum += xsum;
			}
			if (ysum > astarval) {
				astarval = ysum;
				astar = a;
			}
		}
		System.out.println("(OptimalExplorer) choosing sensor action: "+world.actionToString(astar) + " (value: " + astarval + ")");
		lastSensorAction = astar;
		return astar;
	}

	/** does the whole messy calculation. Impractical on anything bigger than 15 or so. Could be pruned somewhat, but still. */
	private int getSensorActionSlowly() {
		System.out.print("(OptimalExplorer) Choosing sensor action: ");
		// this is a bit naive, could (should) only look at possible state transitions
		int astar = -1;
		double astarval = Double.NEGATIVE_INFINITY;

		for (int a : actions) {
			// sum over observations
			double ysum = 0;
			for (int y : observations) {
				// sum over possible states
				double xsum = 0;
				for (int agentX = 0; agentX < beliefs.length; agentX++) {
					for (int agentY = 0; agentY < beliefs[0].length; agentY++) {
						// of the expected value of the best action
						double b = getBestActionValue(a,y);
						if (b != Double.NEGATIVE_INFINITY)
							xsum += world.observationProbability(y, a, agentX, agentY) *
									beliefs[agentX][agentY] *
									b;
					}
				}
				ysum += xsum;
			}
			if (ysum > astarval) {
				astarval = ysum;
				astar = a;
			}
		}
		System.out.println(world.actionToString(astar) + " (value: " + astarval + ")");
		lastSensorAction = astar;
		return astar;
	}

	/** for now just does the naive calculation */
	private double getBestActionValue(int a, int obs) {
		// what would our beliefs be in these circumstances?
		double[][] newBel = updateBeliefs(a, obs);
		if (newBel[0][0] == Double.NEGATIVE_INFINITY)
			return Double.NEGATIVE_INFINITY;

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
							stateSum += newBel[x][y] * // belief in CURRENT state if we were to make the observation, times
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
		return bstarval;
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

	@Override
	public int getAction() { // uses current beliefs -- ie. assumes they have been updated already
		int bstar = (useFast)? getActionFaster() : getActionSlowly();
		advanceBeliefs(bstar);
		System.out.println("(OptimalExplorer) Choosing action: " + world.actionToString(bstar));
		lastAction = bstar;
		return bstar;
	}
	/** slightly cleverer */
	private int getActionFaster() {
		return opt[lastSensorAction][lastObservation].action;
	}

	/** the naive way */
	private int getActionSlowly() {
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
							stateSum += beliefs[x][y] * // belief in CURRENT state if we were to make the observation, times
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


		return bstar;
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

	/** Precomputes a table of actions and observations given a set of beliefs */
	private Action[][] precompBStar() {

		for (int a : sensorActions) {
			for (int y : observations) {
				double[][] bel = updateBeliefs(a,y);
				int bstar = -1;
				double bstarval = Double.NEGATIVE_INFINITY;
				for (int b : actions) {
					double sumx = 0;
					for (int ax = 0; ax < beliefs.length; ax++) {
						for (int ay = 0; ay < beliefs[0].length; ay++) {
							double sumxp = 0;
							for (int axp = 0; axp < beliefs.length; axp++) {
								for (int ayp = 0; ayp < beliefs[0].length; ayp++) {
									sumxp += world.transitionProbability(axp, ayp, ax, ay, b) * world.getReward(new int[]{axp,ayp});
								}
							}
							sumx += bel[ax][ay]*sumxp;
						}
					}
					if (sumx > bstarval) {
						bstarval = sumx;
						bstar = b;
					}
				}
				if (opt[a][y] == null)
					opt[a][y] = new Action();
				opt[a][y].action = bstar;
				opt[a][y].value = bstarval;
			}
		}

		return opt;
	}

	@Override
	public void observe(int y) {
		System.out.println("(OptimalExplorer) Observing: " + y);
		beliefs = updateBeliefs(lastSensorAction, y);
		lastObservation = y;
	}

	@Override
	public int getLastSensorAction() {
		return lastSensorAction;
	}

	/** For seeing what this fellow is thinking */
	public double[][] getBeliefs() {
		return beliefs;
	}

	@Override
	public String toString() {
		return "OptimalExplorer";
	}

	@Override
	public Color getColor() {
		return Color.blue;
	}

	private static class Action {
		public int action;
		public double value;
	}
}
