package de.siteof.progressmonitor.resource.test;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import de.siteof.progressmonitor.IProgressCallback;
import de.siteof.progressmonitor.IProgressMonitor;
import de.siteof.progressmonitor.IProgressMonitorFactory;
import de.siteof.progressmonitor.resource.ProgressMonitorResourceLoader;
import de.siteof.resource.IResourceLoader;
import de.siteof.resource.util.test.ResourceLoaderTestParameter;
import de.siteof.resource.util.test.ResourceLoaderTester;
import de.siteof.test.LabelledParameterized;

@RunWith(LabelledParameterized.class)
public class ProgressMonitorResourceLoaderTest {

	private static final TestProgressMonitor progressMonitor = new TestProgressMonitor();

	private static final IProgressMonitorFactory progressMonitorFactory = new IProgressMonitorFactory() {

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


	private static ResourceLoaderTester tester = new ResourceLoaderTester() {
		@Override
		protected IResourceLoader createResourceLoader(IResourceLoader parent) {
			return new ProgressMonitorResourceLoader(parent, progressMonitorFactory);
		}
	};

	private final ResourceLoaderTestParameter test;

	public ProgressMonitorResourceLoaderTest(ResourceLoaderTestParameter test) {
		this.test = test;
	}

	@Parameters
    public static Collection<Object[]> getTests() {
    	return tester.allTestsArrays();
    }

	@Test
	public void test() throws IOException {
		tester.test(test, "http://dummy/");
	}

}
