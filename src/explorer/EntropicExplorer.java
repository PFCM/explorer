package explorer;

import java.awt.Color;


/**
 * An agent that always chooses the sensor action which reduces the entropy of its beliefs.
 * This agent (currently) then acts optimally to maximise the expected reward of its manipulatory
 * action given its newly less entropic beliefs.
 *
 * More specifically it will always choose the ensor action which has the greatest expected reduction
 * in entropy. That is to say itgoes through each possible sensor action and averages the reduction
 * in entropy given by each possible outcome, weighted by the probability of that outcome (according to
 * current beliefs). It then simply grabs the best one.
 * @author mathewpaul1
 *
 */
public class EntropicExplorer implements Explorer {

	/** Entropy of the current set of beliefs */
	private double currentEntropy;
	private double beliefs[][];

	private ExplorerWorld world;

	private int lastAction;
	private int lastSensorAction;
	private int[] actions = new int[]{ExplorerWorld.NORTH, ExplorerWorld.SOUTH, ExplorerWorld.WEST, ExplorerWorld.EAST};
	private int[] sensorActions = actions;
	private int[] observations = new int[]{ExplorerWorld.EMPTY, ExplorerWorld.FULL, ExplorerWorld.TARGET};

	public EntropicExplorer(ExplorerWorld w) {
		world = w;
		world.addExplorer(this);

		// set up initial beliefs
		beliefs = new double[world.getMap().length][world.getMap()[0].length];
		double belief = 1.0/(world.getMap().length * world.getMap()[0].length);
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				beliefs[x][y] = belief;
			}
		}

		currentEntropy = entropy(beliefs);
	}

	@Override
	public int getSensorAction() {
		// we are going to have to find the argmax across sensor actions
		int astar = -1;
		double astarval = Double.NEGATIVE_INFINITY;
		for (int a : sensorActions) {
			// sum across possible observations
			double ysum = 0;
			for (int y : observations) {
				// of the reduction in entropy by having made this observation
				double newEntropy = entropy(updateBeliefs(a,y));
				if (Double.isNaN(newEntropy))
					newEntropy = currentEntropy;
				double entropyReduction = currentEntropy - newEntropy;
				// weighted by the probability
				// which is the sum across all possible states of our belief in that state times the probability of getting the observation in that state
				double xsum = 0;
				for (int ax = 0; ax < beliefs.length; ax++)
					for (int ay = 0; ay < beliefs.length; ay++) {
						xsum += beliefs[ax][ay] * world.observationProbability(y, a, ax, ay);
					}
				ysum += xsum * entropyReduction;
			}
			System.out.println("(EntropicExplorer)    " + world.actionToString(a) + " : " + ysum);
			if (ysum > astarval) {
				astarval = ysum;
				astar = a;
			}
		}
		lastSensorAction = astar;
		System.out.println("(EntropicExplorer) Choosing sensor action: " + world.actionToString(astar));
		return astar;
	}

	/** this is just the same as everybody else */
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

		System.out.println("(EntropiExplorer) Choosing action: " + world.actionToString(bstar) + " (value = " + bstarval + ")");
		lastAction = bstar;
		advanceBeliefs(bstar);
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

	@Override
	public void observe(int y) {
		System.out.print("(EntropicExplorer) Observing: " + y);
		beliefs = updateBeliefs(lastSensorAction, y);
		currentEntropy = entropy(beliefs);
		System.out.println(" (entropy now " + currentEntropy + ")");
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
	public int getLastSensorAction() {
		return lastSensorAction;
	}

	@Override
	public double[][] getBeliefs() {
		return beliefs;
	}

	@Override
	public Color getColor() {
		return new Color(1.0f,0.0f,1.0f,0.5f);
	}

	/** takes a set of beliefs and returns the shannon entropy (log base 2) */
	private double entropy(double[][] beliefs) {
		double sum = 0;
		double log2 = Math.log(2);
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				if (beliefs[x][y] > 0) // because we can't take log of 0 (but we can skit around it because we would be multiplying it by 0 anyway so even if it existed it wouldn't contribute to the sum)
					sum += beliefs[x][y] * (Math.log(beliefs[x][y])/log2); // because logb(n) = log(n)/log(b)
			}
		}

		return -sum;
	}

	@Override
	public String toString() {
		return "EntropicExplorer";
	}

}
