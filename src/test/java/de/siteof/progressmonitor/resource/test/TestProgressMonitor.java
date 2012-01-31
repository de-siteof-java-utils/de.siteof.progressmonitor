package de.siteof.progressmonitor.resource.test;

import de.siteof.progressmonitor.IProgressMonitor;

public class TestProgressMonitor implements IProgressMonitor {

	private boolean cancelled;

	@Override
	public void beginTask(String name, int totalWork) {
	}

	@Override
	public void done() {
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public boolean isCanceled() {
		return cancelled;
	}

	@Override
	public void setCanceled(boolean value) {
		this.cancelled = value;
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
	}

}
