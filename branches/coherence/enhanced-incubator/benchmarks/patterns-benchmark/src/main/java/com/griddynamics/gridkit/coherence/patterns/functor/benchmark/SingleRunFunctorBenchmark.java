package com.griddynamics.gridkit.coherence.patterns.functor.benchmark;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.griddynamics.gridkit.coherence.patterns.command.benchmark.TestHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;

public class SingleRunFunctorBenchmark
{
	public static void warmUp(PatternFacade facade, Set<Member> members)
	{
		Map<Member,FunctorBenchmarkWorkerParams> workers = new HashMap<Member,FunctorBenchmarkWorkerParams>();
		
		for (Member m : members)
		{
			workers.put(m, new FunctorBenchmarkWorkerParams("touch", 10,   // ThreadCount
																	 100, // Invocation per thread 
																	 0));  // Operations. per second
		}
		
		FunctorBenchmarkDispatcher dispatcher = new FunctorBenchmarkDispatcher(10);
		
		TestHelper.sysout("Starting warm up ...");
		for (int i = 1; i <= 5; ++i)
		{
			FunctorBenchmarkStats res = dispatcher.execute(facade, workers);
			TestHelper.sysout("Run " + i + ": " + res.toString());
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
		}
		TestHelper.sysout("Warm up ended");
	}
	
	public static void configureCoherence()
	{
		// Configure coherence
		setSysProp("tangosol.pof.config"                        , "benchmark-pof-config.xml");
		setSysProp("tangosol.coherence.cacheconfig"             , "benchmark-pof-cache-config.xml");
		setSysProp("tangosol.coherence.clusterport"             , "9001");
		setSysProp("tangosol.coherence.distributed.localstorage", "false");	
	}
	
	public static void main(String[] args)
	{
		configureCoherence();
		
		setSysProp("benchmark.functor.threadCount",  "10");
		setSysProp("benchmark.functor.contextCount", "100");
		setSysProp("benchmark.functor.memberCount",  "1");
		
		setSysProp("benchmark.functor.functor"            , "touch");
		//TODO configure invocation service timeout
		setSysProp("benchmark.functor.opsPerSec"          , "500");
        setSysProp("benchmark.functor.invocationPerThread", "1000");
        
        int threadCount = Integer.getInteger("benchmark.functor.threadCount");
        int contextCount = Integer.getInteger("benchmark.functor.contextCount");
        int memberCount = Integer.getInteger("benchmark.functor.memberCount");
        
        String functor = System.getProperty("benchmark.functor.functor");
        int opsPerSec = Integer.getInteger("benchmark.functor.opsPerSec");
        int invocationPerThread = Integer.getInteger("benchmark.functor.invocationPerThread");
        
        PatternFacade facade = PatternFacade.DefaultFacade.getInstance();
		
        @SuppressWarnings("unchecked")
		Set<Member> members = facade.getInvocationService().getInfo().getServiceMembers();
		
        members.remove(CacheFactory.getCluster().getLocalMember());
		
		if (members.size() < memberCount)
		{
			throw new RuntimeException("Not enought cluster members to start SingleRunFunctorBenchmark");
		}
		
		//warmUp(facade, members);
		
		int threadCountStep = threadCount / memberCount;
		int threadCountAdd  = threadCount % memberCount;
		
		Map<Member,FunctorBenchmarkWorkerParams> workers = new HashMap<Member,FunctorBenchmarkWorkerParams>();
		
		for (Member m : members)
		{
			int memberThreadCount = threadCountStep;
			if (threadCountAdd > 0)
			{
				threadCountAdd--;
				memberThreadCount++;
			}
			
			workers.put(m, new FunctorBenchmarkWorkerParams(functor, memberThreadCount, invocationPerThread, opsPerSec));
		}
		
		FunctorBenchmarkDispatcher dispatcher = new FunctorBenchmarkDispatcher(contextCount);
		
		TestHelper.sysout("Starting benchmark up ...");
		FunctorBenchmarkStats res = dispatcher.execute(facade, workers);
		
		System.out.println("--------------------------------------------------------------------------------");
		System.out.println(res.toString());
		System.out.print("Java MS stats");
		System.out.println(res.getJavaMsStats().toString());
		System.out.print("Java NS stats");
		System.out.println(res.getJavaNsStats().toString());
		System.out.print("Coherence MS stats");
		System.out.println(res.getCoherenceMsStats().toString());
		System.out.println("--------------------------------------------------------------------------------");
	}
	
	private static void setSysProp(String prop, String value)
	{
		if (System.getProperty(prop) == null)
		{
			System.setProperty(prop, value);
		}
		System.out.println("[SysProp] " + prop + ": " + System.getProperty(prop));
	}
}
