package de.siteof.progressmonitor;

public interface IProgressCallback {
	
	boolean canCancel();
	
	void cancel();

}
