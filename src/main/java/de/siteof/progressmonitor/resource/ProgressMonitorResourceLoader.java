package de.siteof.progressmonitor.resource;

import java.io.IOException;

import de.siteof.progressmonitor.IProgressMonitorFactory;
import de.siteof.resource.AbstractResourceLoader;
import de.siteof.resource.IResource;
import de.siteof.resource.IResourceLoader;

public class ProgressMonitorResourceLoader extends AbstractResourceLoader {

	private final IResourceLoader resourceLoader;
	private IProgressMonitorFactory progressMonitorFactory;

	public ProgressMonitorResourceLoader(IResourceLoader resourceLoader, IProgressMonitorFactory progressMonitorFactory) {
		super(resourceLoader);
		this.resourceLoader				= resourceLoader;
		this.progressMonitorFactory		= progressMonitorFactory;
	}

	public boolean isProgressMonitor(String name) {
		return true;
	}

	public IResource getResource(String name) throws IOException {
		IResource resource	= resourceLoader.getResource(name);
		if ((resource != null) && (this.isProgressMonitor(name))) {
			resource	= new ProgressMonitorResource(resource, progressMonitorFactory);
		}
		return resource;
	}

}
