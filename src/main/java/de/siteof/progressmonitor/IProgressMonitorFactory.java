package de.siteof.progressmonitor;

public interface IProgressMonitorFactory {

	IProgressMonitor createProgressMonitor();

	IProgressMonitor createProgressMonitor(IProgressCallback callback);
	
}
