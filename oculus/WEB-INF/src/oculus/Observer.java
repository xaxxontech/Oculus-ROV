package oculus;

public interface Observer {
	
	public void updated(final String key);
	
	public void removed(final String key);

}
