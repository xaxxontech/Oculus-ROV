package oculus;

public interface Docker {

	public abstract void cancel();

	/** 
	 * 
	 * 
	 * 
	 * @param str
	 */
	public abstract void autoDock(String str);

	public abstract void dock(String str);

}