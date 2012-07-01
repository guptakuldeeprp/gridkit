package org.gridkit.gatling.remoting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class RemoteFileCache {
	
	private static final String DIGEST_ALGO = "SHA-1";
	private static final String CACHE_PATH = ".cache";
	private Session session;
	private String agentHome;
	private String agentHomePath;
	private ChannelSftp sftp;

	private Map<String, String> fileMapping = new HashMap<String, String>();

	public RemoteFileCache() {		
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setAgentHome(String agentHome) {
		this.agentHome = agentHome;
	}
	
	public void init() throws JSchException, SftpException, IOException {
		sftp = (ChannelSftp) session.openChannel("sftp");
		sftp.connect();
		
		try {
			sftp.mkdir(agentHome);
		}
		catch(SftpException e) {
			// ignore;
		}
		
		sftp.cd(agentHome);
		agentHomePath = sftp.pwd();
		if (!exists(sftp, CACHE_PATH)) {
			sftp.mkdir(CACHE_PATH);
		}
	}
	
	public synchronized String upload(String id, byte[] data) throws SftpException {
		if (! fileMapping.containsKey(id)) {
			String digest = StreamHelper.digest(data, DIGEST_ALGO);
			if (!exists(sftp, CACHE_PATH + "/" + digest)) {
				sftp.mkdir(CACHE_PATH + "/" + digest);			
			}
			String name = id;
			if (name.indexOf('/') > 0) {
				name = name.substring(name.lastIndexOf('/'));
			}
			if (name.indexOf('?') > 0) {
				name = name.substring(0, name.indexOf('?'));
			}
			String rname = CACHE_PATH + "/" + digest + "/" + name;
			if (!exists(sftp, rname)) {
				System.out.println("Uploading: " + rname + " " + data.length + " bytes");
				sftp.put(new ByteArrayInputStream(data), rname);
			}
			else {
				System.out.println("Exists: " + rname + " " + data.length + " bytes");
			}
			
			fileMapping.put(id, agentHomePath + "/" + rname);
		}
		return fileMapping.get(id);
	}

	private boolean exists(ChannelSftp sftp, String path) {
		try {
			return sftp.stat(path) != null;
		} catch (SftpException e) {
			return false;
		}
	}
}
