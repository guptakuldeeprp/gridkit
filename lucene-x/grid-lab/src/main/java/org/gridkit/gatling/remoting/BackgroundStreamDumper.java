package org.gridkit.gatling.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class BackgroundStreamDumper implements Runnable {

	private static List<StreamPair> BACKLOG = new ArrayList<BackgroundStreamDumper.StreamPair>();
	
	static {
		Thread worker = new Thread(new BackgroundStreamDumper());
		worker.setDaemon(true);
		worker.setName("BackgroundStreamCopy");
		worker.start();
	}
	
	public static void link(InputStream is, OutputStream os) {
		synchronized (BACKLOG) {
			BACKLOG.add(new StreamPair(is, os));
		}
	}
	
	@Override
	public void run() {
		byte[] buffer = new byte[1 << 14];
		
		while(true) {
			List<StreamPair> backlog;
			synchronized (BACKLOG) {
				backlog = new ArrayList<BackgroundStreamDumper.StreamPair>(BACKLOG);
			}
			
			int readCount = 0;
			
			for(StreamPair pair: backlog) {
				try {
					if (pair.is.read(buffer, 0, 0) < 0) {
						// EOF
						closePair(pair);
					}
					else if (pair.is.available() > 0) {
						int n = pair.is.read(buffer);
						if (n < 0) {
							closePair(pair);
						}
						else {
							++readCount;
							pair.os.write(buffer, 0, n);
						}
					}
				}
				catch(IOException e) {
					try {
						PrintStream ps = new PrintStream(pair.os);
						e.printStackTrace(ps);
						ps.close();
						pair.is.close();
					}
					catch(Exception x) {
						// ignore;
					}
					synchronized (BACKLOG) {
						BACKLOG.remove(pair);
					}
				}
			}
			
			if (readCount == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}		
	}


	private void closePair(StreamPair pair) {
		synchronized (BACKLOG) {
			BACKLOG.remove(pair);
		}
		try {
			pair.os.close();
		}
		catch(Exception e) {
			// ignore
		}
	}


	private static class StreamPair {
		InputStream is;
		OutputStream os;
		public StreamPair(InputStream is, OutputStream os) {
			super();
			this.is = is;
			this.os = os;
		}
	}	
}
