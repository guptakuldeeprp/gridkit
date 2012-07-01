package org.gridkit.util.vicontrol.jvm;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.gridkit.gatling.remoting.ControlledProcess;
import org.gridkit.util.vicontrol.MassExec;
import org.gridkit.util.vicontrol.ViNode;
import org.gridkit.util.vicontrol.ViNodeConfig;
import org.gridkit.util.vicontrol.ViNodeConfig.ReplyStartupHooks;
import org.gridkit.util.vicontrol.VoidCallable;

public class JvmNode implements ViNode {

	private String name;
	private Process process;
	private ExecutorService executor;
	
	private WrapperPrintStream stdOut;
	private WrapperPrintStream stdErr;
	
	private ViNodeConfig config = new ViNodeConfig();
	
	private boolean active;
	
	public JvmNode(String name, ViNodeConfig config, ControlledProcess cp) throws IOException {
		this.name = name;
		this.process = cp.getProcess();
		this.executor = cp.getExecutionService();
		
		config.apply(this.config);
		
		stdOut = new WrapperPrintStream("[" + name + "] ", System.out);
		stdErr = new WrapperPrintStream("[" + name + "] ", System.err);
		
		process.getOutputStream().close();
		BackgroundStreamDumper.link(process.getInputStream(), stdOut);
		BackgroundStreamDumper.link(process.getErrorStream(), stdErr);
		
		initStartupHooks();
		active = true;
	}

	private void initStartupHooks() throws IOException {
		ReplyStartupHooks replay = new ReplyStartupHooks() {
			@Override
			public void addStartupHook(String name, Runnable hook, boolean override) {
				try {
					executor.submit(hook).get();
				} catch (Exception e) {
					ExceptionHelper.throwUnchecked(e);
				}				
			}
		};
		
		try {
			config.apply(replay);
		}
		catch(Exception e) {
			throw new IOException("Node '" + name + "' has failed to initialize", e);
		}
	}

	public Process getProcess() {
		return process;
	}
	
	@Override
	public void exec(Runnable task) {
		try {
			submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
		}
	}

	@Override
	public void exec(VoidCallable task) {
		try {
			submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
		}
	}

	@Override
	public <T> T exec(Callable<T> task) {
		try {
			return submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
			throw new Error("Unreachable");			
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Future<Void> submit(Runnable task) {
		ensureStarted();
		return (Future<Void>) executor.submit(task);
	}

	@Override
	public Future<Void> submit(VoidCallable task) {
		ensureStarted();
		return executor.submit(new VoidCallable.VoidCallableWrapper(task));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ensureStarted();
		return executor.submit(task);
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		return MassExec.singleNodeMassExec(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(Runnable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(VoidCallable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	private synchronized void ensureStarted() {
		if (!active) {
			throw new IllegalStateException("Node '" + name + "' is not active");
		}
	}

	@Override
	public void setProp(final String propName, final String value) {
		exec(new Runnable(){
			@Override
			public void run() {
				System.setProperty(propName, value);
			}
		});
	}

	@Override
	public void setProps(Map<String, String> props) {
		final Map<String, String> copy = new LinkedHashMap<String, String>(props);
		exec(new Runnable() {
			@Override
			public void run() {
				for(String name: copy.keySet()) {
					System.setProperty(name, copy.get(name));
				}
			}
		});
	}

	@Override
	public void addStartupHook(String name, Runnable hook, boolean override) {
		throw new IllegalStateException("Node " + name + " is started already");
	}

	@Override
	public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
		if (active) {
			config.addShutdownHook(name, hook, override);
		}
	}

	@Override
	public String getProp(final String propName) {
		// TODO handling special props
		return exec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return System.getProperty(propName);
			}
		});
	}

	@Override
	public void suspend() {
		// TODO
		
	}

	@Override
	public void resume() {
		// TODO
		
	}

	@Override
	public synchronized void shutdown() {
		// TODO call shutdown hooks
		if (active) {
			Future<Void> f = submit(new Runnable() {
				@Override
				public void run() {
					System.exit(0);
				}
			});
			try {
				f.get(100, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				// it doesn't matter 
			}
			process.destroy();
			
			active = false;
		}		
	}

	// TODO make wrapper print stream shared utility class
	private static class WrapperPrintStream extends FilterOutputStream {

		private String prefix;
		private boolean startOfLine;
		private PrintStream printStream;
		
		public WrapperPrintStream(String prefix, PrintStream printStream) {
			super(printStream);
			this.prefix = prefix;
			this.startOfLine = true;
			this.printStream = printStream;
		}
		
		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}
		
		@Override
		public synchronized void write(int c) throws IOException {
			synchronized(printStream) {
				checkNewLine();
				if (c == '\n') {
					startOfLine = true;
				}
				super.write(c);
				if (startOfLine) {
					// flush after end of line
					super.flush();
				}
			}
		}

		private void checkNewLine() {
			if (startOfLine) {
				printStream.append(prefix);
				startOfLine = false;
			}
		}
	
		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			synchronized(printStream) {
				checkNewLine();
				for (int i = 0; i != len; ++i) {
					if (b[off + i] == '\n') {
						writeByChars(b, off, len);
						return;
					}
				}
				super.write(b, off, len);
			}
		}

		private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
			for (int i = 0; i != len; ++i) {
				write(cbuf[off + i]);
			}
		}

		@Override
		public void close() throws IOException {
			super.flush();
		}
	}	
}
