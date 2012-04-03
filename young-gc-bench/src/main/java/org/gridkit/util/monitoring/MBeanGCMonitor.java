package org.gridkit.util.monitoring;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;


public class MBeanGCMonitor {

	MBeanServerConnection connection;
	
	private Map<String, CollectorTracker> trackers = new LinkedHashMap<String, CollectorTracker>();
	
	public MBeanGCMonitor(MBeanServerConnection connection) {
		this.connection = connection;
		initTrackers();
	}
	
	private void initTrackers() {
		try {
			for(ObjectName name: connection.queryNames(null, null)) {
				if (name.getDomain().equals("java.lang") && "GarbageCollector".equals(name.getKeyProperty("type"))) {
					CollectorTracker tracker = new CollectorTracker(connection, name);
					trackers.put(tracker.name, tracker);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String calculateStats() {
		StringBuilder sb = new StringBuilder();
		for(CollectorTracker ct: trackers.values()) {
			if (sb.length() > 0) { 
				sb.append('\n');
			}
			sb.append(ct.calculateStats());
		}
		return sb.toString();
	}

	public String reportCollection	() {
		StringBuilder sb = new StringBuilder();
		for(CollectorTracker ct: trackers.values()) {
			String report = ct.reportCollection();
			if (report.length() == 0) {
				continue;
			}
			if (sb.length() > 0) { 
				sb.append('\n');
			}
			sb.append(report);
		}
		return sb.toString();
	}
	
	private static class CollectorTracker {
		
		private MBeanServerConnection mserv;
		private ObjectName mbean;
		
		private String name;
		private long initialCount;
		private long initialTime;
		
		private long prevTimestamp = 0;
		private long lastCount;
		
		public CollectorTracker(MBeanServerConnection mserv, ObjectName gcbean) {
			try {
				this.mserv = mserv;
				this.mbean = gcbean;
				
				this.name = getName();
				this.initialCount = getCollectionCount();
				this.initialTime = getCollectionTime();
				
				while(getCollectionCount() != initialCount) {
					initialCount = getCollectionCount();
					initialTime = getCollectionTime();				
				}
				
				lastCount = initialCount;
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private String getName() throws JMException, IOException {
			return (String) mserv.getAttribute(mbean, "Name");
		}

		private long getCollectionCount() throws JMException, IOException {
			return (Long)mserv.getAttribute(mbean, "CollectionCount");
		}

		private long getCollectionTime() throws JMException, IOException {
			return (Long)mserv.getAttribute(mbean, "CollectionTime");
		}

		private CompositeData getLastGcInfo() throws JMException, IOException {
			return (CompositeData)mserv.getAttribute(mbean, "LastGcInfo");
		}
		
		@SuppressWarnings("unchecked")
		public String reportCollection() {
			try {
				long count = getCollectionCount();
				CompositeData lastGC = getLastGcInfo();
				
				while(getCollectionCount() != count) {
					count = getCollectionCount();
					lastGC = getLastGcInfo();
				}
				
				if (count == lastCount) {
					return "";
				}
				else {
					lastCount = count;					
					StringBuilder builder = new StringBuilder();
					int id = ((Number) lastGC.get("id")).intValue(); 
					long dur = (Long) lastGC.get("duration");
					long startTs = (Long) lastGC.get("startTime");
					
					builder.append("[GC: ").append(name).append("#").append(id).append(" time: ");
					builder.append(dur).append("ms");
					if (prevTimestamp != 0) {
						long inter = startTs - prevTimestamp;
						builder.append(" interval: ").append(inter).append("ms");
					}
					builder.append(" mem:");
					prevTimestamp = startTs;
					Map<List<?>, CompositeData> beforeGC = (Map<List<?>, CompositeData>) lastGC.get("memoryUsageBeforeGc");
					Map<List<?>, CompositeData> afterGC = (Map<List<?>, CompositeData>) lastGC.get("memoryUsageAfterGc");
					for(List<?> memPool: afterGC.keySet()) {
						String poolName = (String) memPool.get(0);
						if (poolName.contains("Perm") || poolName.contains("Cache")) {
							// ignore
							continue;
						}
						Object[] poolKey = new Object[]{poolName};
						CompositeData mbefore = (CompositeData) beforeGC.get(poolKey).get("value"); 
						CompositeData mafter = (CompositeData) afterGC.get(poolKey).get("value"); 
						long before = (Long) mbefore.get("used");
						long after = (Long) mafter.get("used");
						long max = (Long) mbefore.get("max");
						String mb,ma,mm,md;
						if (max > 1024 << 20) {
							ma = (after >> 20) + "m";
							mb = (before >> 20) + "m";
							mm = (max >> 20) + "m";
							md = (after - before) / (1 << 20) + "m";
						}
						else {
							ma = (after >> 10) + "k";
							mb = (before >> 10) + "k";
							mm = (max >> 10) + "k";
							md = (after - before) / (1 << 10) + "k";
						}
						if (!md.startsWith("-")) {
							md = "+" + md;
						}
						
						builder.append(" ").append(poolName).append(": ").append(mb).append(md).append("->").append(ma).append("(max:").append(mm).append(")");
					}
					builder.append("]");
					return builder.toString();
				}
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		}
		
		public String calculateStats() {
			try {
				long count = getCollectionCount();
				long time = getCollectionTime();
				
				while(getCollectionCount() != count) {
					count = getCollectionCount();
					time = getCollectionTime();				
				}
				
				double avg = ((double)(time - initialTime)) / ((double)(count - initialCount));
				
				StringBuilder builder = new StringBuilder();
				builder.append(String.format("%s[ collections: %d | avg: %.4f secs | total: %.1f secs ]", name, count - initialCount, avg / 1000d, (time - initialTime) / 1000d));
				
				return builder.toString();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
