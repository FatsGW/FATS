package com.android.fatsgw.fats.exchangers;

public interface Exchanger
{
	/** Starts the exchanger.
	 * @return Returns true if start succeeds.
	 */
	boolean start();
	
	
	/** Stops the exchanger.
	 * @return Returns true if stop succeeds.
	 */
	boolean stop();
	
	
	/** Restarts the exchanger.
	 * @return Returns true if restart succeeds.
	 */
	boolean restart();
}
