package explorer;

import java.awt.Color;

/**
 * An explorer that just looks straight ahead, in the direction it last traveled. Otherwise behaves optimally according to its beliefs.
 * @author mathewpaul1
 *
 */
public class DumbExplorer implements Explorer {

	private int lastAction = ExplorerWorld.NORTH;
	private double[][] beliefs; // for explanation see the complicated one

	private ExplorerWorld world;


	private int[] actions = new int[]{ExplorerWorld.NORTH, ExplorerWorld.SOUTH, ExplorerWorld.WEST, ExplorerWorld.EAST};

	public DumbExplorer(ExplorerWorld world) {
		world.addExplorer(this);
		this.world = world;
		beliefs = new double[world.getMap().length][world.getMap()[0].length];
		double belief = 1.0/(world.getMap().length * world.getMap()[0].length);
		for (int x = 0; x < beliefs.length; x++) {
			for (int y = 0; y < beliefs[0].length; y++) {
				beliefs[x][y] = belief;
			}
		}
	}

	@Override
	public int getSensorAction() {
		return lastAction;
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

		System.out.println("(DumbExplorer) Choosing action: " + world.actionToString(bstar) + " (value = " + bstarval + ")");
		lastAction = bstar;
		advanceBeliefs(bstar);
		return bstar;
	}

	@Override
	public void observe(int y) {
		System.out.println("(DumbExplorer) Observing: " + y);
		beliefs = updateBeliefs(lastAction, y);
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
		return lastAction;
	}

	public double[][] getBeliefs() {
		return beliefs;
	}

	@Override
	public String toString() {
		return "DumbExplorer";
	}

	@Override
	public Color getColor() {
		return new Color(1.0f,0.5f,0.5f,0.5f);
	}
}
