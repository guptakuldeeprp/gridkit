package com.griddynamics.gridkit.coherence.patterns.command.internal;

/**
 * Simple way to silence warning on per cast, not per method basis.
 * 
 * @author Alexey Ragozin (aragozin@griddynamics.com)
 */
public class AnyType {

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj) {
		return (T)obj;
	}
}
