package com.chinatelecom.smartgateway.guogee;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		NetComm netcomm = NetComm.getInstance();
		SerialComm serialcomm = SerialComm.getInstance();
		Protocol protocol = Protocol.getInstance();
		
		netcomm.start(bundleContext);
		serialcomm.start(bundleContext);
		protocol.start(bundleContext);
		netcomm.startThread();
		protocol.startThread();
		serialcomm.startThread();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;

		Protocol.getInstance().stop();
		SerialComm.getInstance().stop();
		NetComm.getInstance().stop();
	}

}
