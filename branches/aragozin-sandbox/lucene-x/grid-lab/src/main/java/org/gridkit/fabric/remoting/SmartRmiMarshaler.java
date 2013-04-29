package org.gridkit.fabric.remoting;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.gridkit.util.vicontrol.marshal.SmartViMarshaler;

public class SmartRmiMarshaler implements RmiMarshaler {

    @SuppressWarnings("rawtypes")
	private final Map<Class<?>, Class[]> remoteAutodetectCache = new ConcurrentHashMap<Class<?>, Class[]>();	
	
	@SuppressWarnings("rawtypes")
	private Class[] remoteInterfaceMarkers;

	public SmartRmiMarshaler() {
		remoteInterfaceMarkers = new Class[]{Remote.class};
	}

	public SmartRmiMarshaler(Class<?>... types) {
		remoteInterfaceMarkers = types;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public Object writeReplace(Object obj) throws IOException {
		if (obj instanceof Serializable && !Proxy.isProxyClass(obj.getClass())) {
			return obj; // no marshaling
		}
		else if (isEligbleForExport(obj)){
			Class[] ifs = getRemoteInterfaces(obj);
			return new Exported(obj, ifs);
		}
		else {
			return SmartViMarshaler.marshal(obj);
		}
	}

	@SuppressWarnings("rawtypes")
    protected boolean isEligbleForExport(Object obj) {
		for(Class marker: remoteInterfaceMarkers) {
			if (marker.isInstance(obj)) {
				return true;
			}
		}

		return false;
	}

    protected Class<?>[] getRemoteInterfaces(Object obj) throws IOException {
        Class<?> objClass = obj.getClass();
        Class<?>[] result = remoteAutodetectCache.get(objClass);
        if (result != null) {
            return result;
        } else {
            result = detectRemoteInterfaces(objClass);
            remoteAutodetectCache.put(objClass, result);
            return result;
        }
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Class<?>[] detectRemoteInterfaces(Class<?> objClass) throws IOException {
		Class<?>[] result;
		List<Class> iflist = new ArrayList<Class>();
		iflist.addAll(Arrays.asList(objClass.getInterfaces()));

		Iterator<Class> it = iflist.iterator();
		while (it.hasNext()) {
		    Class intf = it.next();

		    if (!isRemoteInterface(intf)) {
		        it.remove();
		        continue;
		    }

		    for (Class other : iflist) {
		        if (intf != other && intf.isAssignableFrom(other)) {
		            it.remove();
		        }
		    }
		}

		if (iflist.isEmpty()) {
			// no interfaces are explicitly marker as remote
			// this is a special case, assume all interfaces except Remote markers are exported
			for(Class intf: objClass.getInterfaces()) {
				if (!isRemoteInterface(intf)) {
					iflist.add(intf);
				}
			}
			
			reduceSuperTypes(iflist);
		}
		
		if (iflist.isEmpty()) {
			throw new IOException("Cannot calculate remote interface for class " + objClass.getName());
		}
		
		result = iflist.toArray(new Class[iflist.size()]);
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void reduceSuperTypes(List<Class> iflist) {
		Iterator<Class> it = iflist.iterator();
		while (it.hasNext()) {
		    Class intf = it.next();
		    for (Class other : iflist) {
		        if (intf != other && intf.isAssignableFrom(other)) {
		            it.remove();
		        }
		    }
		}	
	}

	@SuppressWarnings("rawtypes")
	private boolean isRemoteInterface(Class intf) {
		boolean remote = false;
		for (Class<?> marker : remoteInterfaceMarkers) {
		    if (marker.isAssignableFrom(intf)) {
		        remote = true;
		        break;
		    }
		}
		return remote;
	}
	
	
	@Override
	public Object readResolve(Object obj) throws IOException {
		if (obj instanceof SmartViMarshaler.AnnonEnvelop) {
			return ((SmartViMarshaler.AnnonEnvelop)obj).unmarshal();
		}
		else {
			return obj;
		}
	}	
}