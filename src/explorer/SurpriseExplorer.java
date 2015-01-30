package explorer;

import java.awt.Color;

/**
 * An explorer which attempts to use Itti et al's notion of Bayesian Surprise to determine where to look.
 * The argument is that the above author's experiments seem to show that people will look at surprising things, where
 * they define surprise as something that will change our beliefs the most. This is very slightly different from the
 * entropic agent as they use the KL divergence between the posterior and the prior (after having made the observation)
 * to find a measure of the surprisingness of an observation. The maths of this comes out curiously close to the difference in
 * the entropy of the two distributions, but it is not identical.
 * @author mathewpaul1
 *
 */
public class SurpriseExplorer implements Explorer {

	/** current beliefs */
	private double beliefs[][];

	private ExplorerWorld world;

	private int lastAction;
	private int lastSensorAction;
	private int[] actions = new int[]{ExplorerWorld.NORTH, ExplorerWorld.SOUTH, ExplorerWorld.WEST, ExplorerWorld.EAST};
	private int[] sensorActions = actions;
	private int[] observations = new int[]{ExplorerWorld.EMPTY, ExplorerWorld.FULL, ExplorerWorld.TARGET};

	public SurpriseExplorer(ExplorerWorld w) {
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

	/** Returns the Kullback-Leibler divergence of distributions a and b (in that order) */
	private double klDivergence(double[][] a, double[][] b) {
		double sum = 0;
		// sum over all possible states
		for (int x = 0; x < a.length; x++) {
			for (int y = 0; y < a[0].length; y++) {
				if (a[x][y] != 0 && b[x][y] != 0)
					sum += a[x][y] * Math.log(a[x][y]/b[x][y]);
			}
		}
		return sum;
	}

	@Override
	public int getSensorAction() {
		// find the action with the greatest expected surprise
		int astar = -1;
		double astarval = Double.NEGATIVE_INFINITY;
		for (int a : actions) {
			// have to find the expected surprise across the observations space
			double ysum = 0;
			for (int y : observations) {
				// get our beliefs as they would be in this circumstance
				double[][] newbel = updateBeliefs(a,y);
				double surprise = klDivergence(newbel, beliefs); // in this order, according to Itti et al
				// average out by multiplying with P(y)
				// so we have to sum out over beliefs in x as we only know P(y|x)Bel(x)
				double xsum = 0;
				for (int ax = 0; ax < beliefs.length; ax++)
					for (int ay = 0; ay < beliefs[0].length; ay++) {
						xsum += world.observationProbability(y, a, ax, ay) * beliefs[ax][ay];
					}
				ysum += surprise * xsum; // surprise for this observation * prob of this observation

			}
			if (ysum > astarval) {
				astarval = ysum;
				astar = a;
			}
		}

		System.out.println("(SurpriseExplorer) Choosing action: " + world.actionToString(astar) + " (expected surprise " + astarval + ")");
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

		System.out.println("(SurpriseExplorer) Choosing action: " + world.actionToString(bstar) + " (value = " + bstarval + ")");
		lastAction = bstar;
		advanceBeliefs(bstar);
		return bstar;
	}

	@Override
	public void observe(int y) {
		System.out.println("(SurpriseExplorer) Observing: " + y);
		beliefs = updateBeliefs(lastSensorAction, y);
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
		return new Color(0.0f,0.5f,1.0f,0.5f);
	}

}
