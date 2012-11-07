package org.gridkit.nimble.btrace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.client.Client;

import org.gridkit.nimble.btrace.ext.ClearSamplesCmd;
import org.gridkit.nimble.btrace.ext.PollSamplesCmd;
import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.util.NamedThreadFactory;

public class BTraceClientOps {    
    private static long INTERRUPT_DELAY_MS = 1000;
    
    private ExecutorService executor = Executors.newCachedThreadPool(
        new NamedThreadFactory(this.getClass() + "Executor", true, Thread.NORM_PRIORITY)
    );
        
    private ScheduledExecutorService interruptExecutor = Executors.newSingleThreadScheduledExecutor(
        new NamedThreadFactory(this.getClass() + "InterruptExecutor", true, Thread.MIN_PRIORITY)
    );
        
    public void submit(final Client client, final Class<?> clazz, final String[] params, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                client.submit(clazz.getName(), getClassBytes(clazz), params);
                return null;
            }
        }, timeoutMs);

    }
    
    public PollSamplesCmdResult pollSamples(Client client, final Collection<Class<?>> classes, long timeoutMs) throws InterruptedException, IOException {
        AbstractCommand.Initializer<PollSamplesCmd> initializer = new AbstractCommand.Initializer<PollSamplesCmd>() {
            @Override
            public void init(PollSamplesCmd cmd) {
                Set<String> scriptClasses = new HashSet<String>();
                
                for (Class<?> clazz : classes) {
                    scriptClasses.add(clazz.getName());
                }
                
                cmd.setScriptClasses(scriptClasses);
            }
        };
        
        PollSamplesCmdResult result = (PollSamplesCmdResult)client.getCommChannel()
                                                                  .sendCommand(PollSamplesCmd.class, initializer)
                                                                  .get(timeoutMs);
        
        return result;
    }

    public boolean clearSamples(Client client, final Collection<Class<?>> classes, long timeoutMs) throws InterruptedException, IOException {
        AbstractCommand.Initializer<ClearSamplesCmd> initializer = new AbstractCommand.Initializer<ClearSamplesCmd>() {
            @Override
            public void init(ClearSamplesCmd cmd) {
                Set<String> scriptClasses = new HashSet<String>();
                
                for (Class<?> clazz : classes) {
                    scriptClasses.add(clazz.getName());
                }
                
                cmd.setScriptClasses(scriptClasses);
            }
        };
                
        Boolean result = (Boolean)client.getCommChannel()
                                        .sendCommand(ClearSamplesCmd.class, initializer)
                                        .get(timeoutMs);
        
        return result;
    }
    
    public void exit(final Client client, final int exitCode, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        execute(new Callable<Void>() {
            @Override
            public Void call() {
                client.exit(exitCode);
                return null;
            }
        }, timeoutMs);
    }
    
    private <T> T execute(Callable<T> callable, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        final Future<T> future = executor.submit(callable);
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            interruptExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    future.cancel(true);
                }
            }, INTERRUPT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }
    
    private static byte[] getClassBytes(Class<?> clazz) throws IOException {
        InputStream is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        byte[] result = buffer.toByteArray();
        
        return result;
    }
}