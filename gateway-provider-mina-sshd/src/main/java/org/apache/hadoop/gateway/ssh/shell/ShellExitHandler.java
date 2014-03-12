package org.apache.hadoop.gateway.ssh.shell;

import java.util.concurrent.locks.ReentrantLock;

public class ShellExitHandler {

	public static final int ERROR_EXIT = 127;

	private final KnoxTunnelShell exitCallbackHolder;
	private boolean exited = false;
	private final ReentrantLock exitLock = new ReentrantLock();

	public ShellExitHandler(KnoxTunnelShell exitCallbackHolder) {
		this.exitCallbackHolder = exitCallbackHolder;
	}

	public void failure(Throwable source) {
		String lineSep = "\n";
		StringBuilder sb = new StringBuilder();
		sb.append("Exception halted execution of terminal:");
		sb.append(lineSep);
		sb.append(source.getMessage());
		for (StackTraceElement element : source.getStackTrace()) {
			sb.append(lineSep);
			sb.append(element);
		}
		exitLock.lock();
		try {
			if (exited) {
				return;
			}
			exited = true;
			exitCallbackHolder.getExitCallback().onExit(ERROR_EXIT,
					sb.toString());
		} finally {
			exitLock.unlock();
		}
	}

	public void normalExit(int result) {
		exitLock.lock();
		try {
			if (exited) {
				return;
			}
			exited = true;
			exitCallbackHolder.getExitCallback().onExit(result);
		} finally {
			exitLock.unlock();
		}

	}

}