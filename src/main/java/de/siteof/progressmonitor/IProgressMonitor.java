package de.siteof.progressmonitor;

// copied from org.eclipse.core.runtime
public interface IProgressMonitor {

	int UNKNOWN = -1;

	void beginTask(String name, int totalWork);

	void done();

	void internalWorked(double work);

	boolean isCanceled();

	void setCanceled(boolean value);

	void setTaskName(String name);

	void subTask(String name);

	void worked(int work);

}
