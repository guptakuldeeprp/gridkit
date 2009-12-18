/**
 * 
 */
package com.griddynamics.gridkit.coherence;

import com.tangosol.net.DefaultCacheServer;

/**
 * @author akornev
 * @since 1.0
 */
public class DefaultCache {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Configure coherence
		System.setProperty("tangosol.pof.config", "pof-config.xml");
		System.setProperty("tangosol.coherence.cacheconfig",
				"coherence-commandpattern-proxy-pof-cache-config.xml");
		System.setProperty("tangosol.coherence.clusterport", "9001");
		DefaultCacheServer.main(args);
	}
}
