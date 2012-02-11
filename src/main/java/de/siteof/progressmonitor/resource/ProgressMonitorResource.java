package de.siteof.progressmonitor.resource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.progressmonitor.IProgressCallback;
import de.siteof.progressmonitor.IProgressMonitor;
import de.siteof.progressmonitor.IProgressMonitorFactory;
import de.siteof.resource.IResource;
import de.siteof.resource.ResourceProxy;
import de.siteof.resource.ResourceRequestParameters;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.ResourceLoaderEvent;
import de.siteof.resource.util.IOUtil;

public class ProgressMonitorResource extends ResourceProxy {

	private static class ProgressMonitorInputStream extends FilterInputStream {

		private final IProgressMonitor progressMonitor;
		private final long resolution;
		private long readCounter;
		private int lastWorked;
		private final long size;

		protected ProgressMonitorInputStream(InputStream in, IProgressMonitor progressMonitor, long size, long resolution) {
			super(in);
			this.progressMonitor	= progressMonitor;
			this.size				= size;
			this.resolution		= resolution;
		}

		protected void doOnBeforeRead(int maxCount) {
		}

		protected void doOnRead(int count) {
			readCounter	+= count;
			int worked	= (int) (readCounter / resolution);
			progressMonitor.worked(worked - lastWorked);
			lastWorked	= worked;
		}

		@Override
		public int read() throws IOException {
			doOnBeforeRead(1);
			int result	= super.read();
			if (result >= 0) {
				doOnRead(1);
			}
			return result;
		}

		@Override
		public int read(byte[] buffer, int offset, int count) throws IOException {
			final int maxRead	= 128;
			if (count > maxRead) {
				int result	= 0;
				while (result < count) {
					int blockSize	= Math.min(maxRead, count - result);
					int blockRead	= this.read(buffer, offset + result, blockSize);
					result += blockRead;
					if (result != blockSize) {
						break;
					}
				}
				return result;
			} else {
				doOnBeforeRead(count);
				int result	= super.read(buffer, offset, count);
				if (result > 0) {
					doOnRead(result);
				}
				return result;
			}
		}

		@Override
		public int read(byte[] buffer) throws IOException {
			return this.read(buffer, 0, buffer.length);
		}

		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#close()
		 */
		@Override
		public void close() throws IOException {
			try {
				super.close();
			} finally {
				if (readCounter < size) {
					progressMonitor.setCanceled(true);
				}
				progressMonitor.done();
			}
		}

	}


	private static class ProgressCallback implements IProgressCallback {

		private final IResource resource;
		private InputStream inputStream;
		private Thread thread;

		public ProgressCallback(IResource resource) {
			this.resource = resource;
		}

		@Override
		public boolean canCancel() {
			return ((inputStream != null) || (thread != null));
		}

		@Override
		public void cancel() {
			if (resource != null) {
				resource.abort();
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					log.warn("failed to close stream - " + e, e);
				}
			} else if (thread != null) {
//				if (thread != Thread.currentThread()) {
//					thread.interrupt();
//				}
			}
		}

		/**
		 * @param inputStream the inputStream to set
		 */
		public void setInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		/**
		 * @param thread the thread to set
		 */
		public void setThread(Thread thread) {
			this.thread = thread;
		}

	}


	private final IProgressMonitorFactory progressMonitorFactory;

	private static final Log log	= LogFactory.getLog(ProgressMonitorResource.class);


	public ProgressMonitorResource(IResource resource,  IProgressMonitorFactory progressMonitorFactory) {
		super(resource);
		this.progressMonitorFactory		= progressMonitorFactory;
	}

	@Override
	public InputStream getResourceAsStream() throws IOException {
		IProgressMonitor progressMonitor	= null;
		ProgressCallback callback = null;
		if (progressMonitorFactory != null) {
			callback = new ProgressCallback(this);
			callback.setThread(Thread.currentThread());
			progressMonitor	= progressMonitorFactory.createProgressMonitor(callback);
		}
		if (progressMonitor != null) {
			progressMonitor.beginTask(this.getName(), IProgressMonitor.UNKNOWN);
		}
		InputStream in	= null;
		try {
			in	= super.getResourceAsStream();
			if (in != null) {
				if (progressMonitor != null) {
					long size		= this.getSize();
					long resolution	= 1;
					progressMonitor.beginTask(this.getName(), (int) (size / resolution));
					in	= new ProgressMonitorInputStream(in, progressMonitor, size, resolution);
					callback.setInputStream(in);
				}
			}
		} finally {
			if ((in == null) && (progressMonitor != null)) {
				progressMonitor.setCanceled(true);
				progressMonitor.done();
			}
		}
		return in;
	}

	@Override
	public byte[] getResourceBytes() throws IOException {
		InputStream in	= getResourceAsStream();
		if (in == null) {
			log.warn("resource not found: " + this.getName());
			return null;
		}
		try {
			log.debug("loading data from resource, name=" + this.getName());
			return IOUtil.readAllFromStream(in);
		} finally {
			in.close();
		}
	}

	/* (non-Javadoc)
	 * @see de.siteof.webpicturebrowser.loader.ResourceProxy#getResourceAsStream(de.siteof.webpicturebrowser.loader.event.IResourceListener)
	 */
	@Override
	public void getResourceAsStream(
			IResourceListener<ResourceLoaderEvent<InputStream>> listener,
			ResourceRequestParameters parameters)
			throws IOException {
		final IResourceListener<ResourceLoaderEvent<InputStream>> finalListener = listener;
		IProgressMonitor progressMonitor	= null;
		ProgressCallback callback = null;
		if (progressMonitorFactory != null) {
			callback = new ProgressCallback(this);
			callback.setThread(Thread.currentThread());
			progressMonitor	= progressMonitorFactory.createProgressMonitor(callback);
		}
		if (progressMonitor != null) {
			progressMonitor.beginTask(this.getName(), IProgressMonitor.UNKNOWN);
		}

		final IProgressMonitor finalProgressMonitor	= progressMonitor;
		final ProgressCallback finalCallback = callback;

		this.getResource().getResourceAsStream(new IResourceListener<ResourceLoaderEvent<InputStream>>() {
			@Override
			public void onResourceEvent(
					ResourceLoaderEvent<InputStream> event) {
				IProgressMonitor progressMonitor	= finalProgressMonitor;
				if (event.isComplete()) {
					InputStream in	= null;
					try {
						in	= event.getResult();
						if (in != null) {
							if (progressMonitor != null) {
								long size;
								try {
									size = in.available();
								} catch (IOException e) {
									log.warn("failed to retrieve the available size from the input stream - " + e, e);
									size = 0;
								}
								long resolution	= 1;
								if (progressMonitor != null) {
									progressMonitor.beginTask(getName(), (int) (size / resolution));
								}
								in	= new ProgressMonitorInputStream(in, progressMonitor, size, resolution);
								finalCallback.setInputStream(in);
							}
						}

						finalListener.onResourceEvent(new ResourceLoaderEvent<InputStream>(
								ProgressMonitorResource.this, in, true));

					} finally {
						if ((in == null) && (progressMonitor != null)) {
							progressMonitor.setCanceled(true);
							progressMonitor.done();
						}
					}
				} else if (event.isFailed()) {
					if (progressMonitor != null) {
						progressMonitor.setCanceled(true);
						progressMonitor.done();
					}
					finalListener.onResourceEvent(new ResourceLoaderEvent<InputStream>(
							ProgressMonitorResource.this, event.getCause()));
				} else if (event.hasStatusMessage()) {
					CharSequence statusMessage = event.getStatusMessage();
					if (progressMonitor != null) {
						progressMonitor.subTask(statusMessage.toString());
					}
					finalListener.onResourceEvent(new ResourceLoaderEvent<InputStream>(
							ProgressMonitorResource.this, statusMessage));
				} else {
					finalListener.onResourceEvent(event.cloneFor(
							ProgressMonitorResource.this, (InputStream) null));
				}
			}}, parameters);
	}

	/* (non-Javadoc)
	 * @see de.siteof.webpicturebrowser.loader.ResourceProxy#getResourceBytes(de.siteof.webpicturebrowser.loader.event.IResourceListener)
	 */
	@Override
	public void getResourceBytes(
			IResourceListener<ResourceLoaderEvent<byte[]>> listener,
			ResourceRequestParameters parameters)
			throws IOException {
		final IResourceListener<ResourceLoaderEvent<byte[]>> finalListener = listener;
		this.getResourceAsStream(new IResourceListener<ResourceLoaderEvent<InputStream>>() {
			@Override
			public void onResourceEvent(ResourceLoaderEvent<InputStream> event) {
				if (event.isComplete()) {
					try {
						InputStream in = event.getResult();
						if (in != null) {
							boolean chunked = true;
							if (chunked) {
								try {
									byte[] buffer = new byte[4096];
									while (true) {
										int bytesRead = in.read(buffer);
										if (bytesRead < 0) {
											break;
										}
										if (bytesRead > 0) {
											// chunk
											byte[] data = Arrays.copyOf(buffer, bytesRead);
											finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
													ProgressMonitorResource.this, data, false));
										}
									}
									// complete event
									finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
											ProgressMonitorResource.this, new byte[0], true));
								} finally {
									in.close();
								}
							} else {
								byte[] data = getResourceBytesFromEvent(event);
								finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
										ProgressMonitorResource.this, data, true));
							}
						} else {
							finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
									ProgressMonitorResource.this, new byte[0], true));
						}
					} catch (IOException e) {
						finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
								ProgressMonitorResource.this,
								new IOException("failed to read bytes from input steam - " + e, e)));
					}
				} else if (event.isFailed()) {
					finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
							ProgressMonitorResource.this, event.getCause()));
				} else if (event.hasStatusMessage()) {
					CharSequence statusMessage = event.getStatusMessage();
					finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
							ProgressMonitorResource.this, statusMessage));
				} else {
					finalListener.onResourceEvent(event.cloneFor(ProgressMonitorResource.this, (byte[]) null));
				}
			}}, parameters);
		/*
		IProgressMonitor progressMonitor	= null;
		ProgressCallback callback = null;
		if (progressMonitorFactory != null) {
			callback = new ProgressCallback(this);
			callback.setThread(Thread.currentThread());
			progressMonitor	= progressMonitorFactory.createProgressMonitor(callback);
		}
		if (progressMonitor != null) {
			progressMonitor.beginTask(this.getName(), IProgressMonitor.UNKNOWN);
		}

		final IProgressMonitor finalProgressMonitor	= progressMonitor;

		this.getResource().getResourceBytes(new IResourceListener<ResourceLoaderEvent<byte[]>>() {
			public void onResourceEvent(
					ResourceLoaderEvent<byte[]> event) {
				IProgressMonitor progressMonitor	= finalProgressMonitor;
				if (event.isComplete()) {
					byte[] data	= event.getResult();
					progressMonitor.setCanceled(false);
					progressMonitor.done();
					finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
							ProgressMonitorResource.this, data, true));
				} else if (event.isFailed()) {
					progressMonitor.setCanceled(true);
					progressMonitor.done();
					finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
							ProgressMonitorResource.this, event.getCause()));
				} else if (event.hasStatusMessage()) {
					CharSequence statusMessage = event.getStatusMessage();
					progressMonitor.subTask(statusMessage.toString());
					finalListener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
							ProgressMonitorResource.this, statusMessage));
				}
			}});
			*/
	}


}
