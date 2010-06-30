/**
 * Copyright 2008-2009 Grid Dynamics Consulting Services, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.griddynamics.convergence.demo.utils.cluster;

import com.googlecode.gridkit.fabric.exec.ProcessExecutor;


public interface Cluster {

	public Host[] getNodes();
	public Host getHost(String address);
	
	public interface Host extends ProcessExecutor {

		public String getHostname();
		
//		public InputStream remoteRead(String remoteFile) throws IOException;
//		public OutputStream remoteWrite(String remoteFile, boolean append) throws IOException;
//		public boolean remoteDelete(String remoteFile) throws IOException;
		
	}
}
