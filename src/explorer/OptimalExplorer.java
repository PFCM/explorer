package explorer;

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

	public OptimalExplorer(ExplorerWorld world) {
		this.world = world; // have to promise not to cheat
		beliefs = new double[world.getMap().length][world.getMap()[0].length];
		// right know we have no idea at all
		for (int i = 0; i < beliefs.length; i++) {
			for (int j = 0; j < beliefs[0].length; j++) {
				beliefs[i][j] = 1.0/(beliefs.length * beliefs[0].length);
			}
		}
	}

	@Override
	public int getSensorAction() {
		System.out.print("(OptimalExplorer) Choosing sensor action: ");
		// this is a bit naive, can do some pre-computation in future
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
						xsum += world.observationProbability(y, a, agentX, agentY) *
								beliefs[agentX][agentY] *
								getBestActionValue(a,y);
					}
				}
				ysum += xsum;
			}
			if (ysum > astarval) {
				astarval = ysum;
				astar = a;
			}
		}
		System.out.println(""+astar + "(value: " + astarval + ")");
		return astar;
	}

	/** for now just does the naive calculation */
	private double getBestActionValue(int a, int obs) {
		// what would our beliefs be in these circumstances?
		double[][] newBel = advanceBeliefs(a, obs);

		int bstar = -1;
		double bstarval = Double.NEGATIVE_INFINITY;
		for (int b : actions) {
			// now we need to look at each possible state transition
			double stateSum = 0;
			double newStateSum = 0;
			// which means for every possible next state
			for (int newX = 0; newX < beliefs.length; newX++) {
				for (int newY = 0; newY < beliefs[0].length; newY++) {
					int[] newState = new int[]{newX,newY};
					// we need to look at every possible previous state
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

	/** Returns the advanced beliefs given a sensor action and an observation. Does not ovewrite existing beliefs
	 * as we have to use this for figuring out the best options
	 * @param a - the sensor action
	 * @param obs - the observation
	 * @return
	 */
	private double[][] advanceBeliefs(int a, int obs) {
		double[][] newBeliefs = new double[beliefs.length][beliefs[0].length];
		double sum = 0; // for the normalisation
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs.length; y++) {
				newBeliefs[x][y] = beliefs[x][y] * world.observationProbability(obs, a, x, y);
				sum += newBeliefs[x][y];
			}
		}
		// now normalis
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs.length; y++) {
				newBeliefs[x][y] /= sum;
			}
		}
		return newBeliefs;
	}

	@Override
	public int getAction() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void observe(int y) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLastSensorAction() {
		// TODO Auto-generated method stub
		return 0;
	}

}
