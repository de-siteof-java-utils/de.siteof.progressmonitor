package de.siteof.progressmonitor.resource.test;

import java.io.IOException;

import org.junit.Test;

import de.siteof.progressmonitor.IProgressCallback;
import de.siteof.progressmonitor.IProgressMonitor;
import de.siteof.progressmonitor.IProgressMonitorFactory;
import de.siteof.progressmonitor.resource.ProgressMonitorResourceLoader;
import de.siteof.resource.IResourceLoader;
import de.siteof.resource.util.test.ResourceLoaderTester;

public class ProgressMonitorResourceLoaderTest {

	private final ResourceLoaderTester tester =  new ResourceLoaderTester();

	private final IProgressMonitorFactory progressMonitorFactory;

	private final TestProgressMonitor progressMonitor = new TestProgressMonitor();

	public ProgressMonitorResourceLoaderTest() {
		progressMonitorFactory = new IProgressMonitorFactory() {

			@Override
			public IProgressMonitor createProgressMonitor() {
				return progressMonitor;
			}

			@Override
			public IProgressMonitor createProgressMonitor(
					IProgressCallback callback) {
				return progressMonitor;
			}
		};
	}

	protected void doTest() throws IOException {
		this.doTest("dummy");
	}

	protected void doTest(String name) throws IOException {
		tester.setResourceLoader(this.createResourceLoader(tester.getParent()));
		tester.test(name);
	}

	protected IResourceLoader createResourceLoader(IResourceLoader parent) {
		return new ProgressMonitorResourceLoader(parent, progressMonitorFactory);
	}

	@Test
	public void test() throws IOException {
		this.doTest("http://dummy/");
	}

}
