package explorer;

import java.awt.Color;

/** The actual explorer. Or at least, what an actual explorer must be able to do. */
public interface Explorer {
	/** Needs to return a sensor action, one of the four directions defined in ExplorerWorld */
	public int getSensorAction();
	/** Get a real action that actually modifies the state */
	public int getAction();
	/** Receive an observation and do any necessary belief updates etc */
	public void observe(int y);

	/** gets the last sensor action taken */
	public int getLastSensorAction();

	/** returns the beliefs of the agent*/
	public double[][] getBeliefs();

	/** Each type of explorer should have a unique colour */
	public Color getColor();
}
