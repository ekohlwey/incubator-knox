package org.apache.hadoop.gateway.ssh.shell;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.hadoop.gateway.ssh.commands.SSHAction;
import org.apache.hadoop.gateway.ssh.commands.UnsupportedCommandAction;
import org.apache.sshd.common.util.NoCloseInputStream;

import com.google.common.io.LineReader;

public class ShellInterpreterThread extends Thread implements Closeable {
	private final ShellExitHandler exitHandler;
	private final Map<String, SSHAction> shellActions;
	private final ReentrantLock stopLock = new ReentrantLock();
	private boolean stop = false;
	private final KnoxTunnelShell knoxShell;

	public ShellInterpreterThread(KnoxTunnelShell knoxShell,
			ShellExitHandler exitHandler, Map<String, SSHAction> shellActions) {
		this.knoxShell = knoxShell;
		this.exitHandler = exitHandler;
		this.shellActions = shellActions;
	}

	public void run() {
		boolean run = true;
		int result = 0;
		while (run) {
			BufferedReader reader = knoxShell.getReader();
			String line = null;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				exitHandler.failure(e);
				return;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						exitHandler.failure(e);
						return;
					}
				}
			}
			if (line == null) {
				run = false;
				continue;
			}
			line = line.trim();
			String command;
			String unconsumedLine;
			int spaceIndex = line.indexOf(' ');
			if (spaceIndex > 0) {
				command = line.substring(0, spaceIndex);
				// line was trimmed so we know space is not last char
				unconsumedLine = line.substring(spaceIndex + 1, line.length());
			} else {
				command = line;
				unconsumedLine = "";
			}
			SSHAction action = shellActions.get(command);
			if (action == null) {
				action = new UnsupportedCommandAction();
			}
			try {
				result = action
						.handleCommand(command, unconsumedLine,
								reader,
								knoxShell.getOutputStream(),
								knoxShell.getErrorStream());
			} catch (IOException e) {
				exitHandler.failure(e);
			}
			stopLock.lock();
			run = !stop;
			stopLock.unlock();
		}
		exitHandler.normalExit(result);
	}

	@Override
	public void close() throws IOException {
		if (stop) {
			throw new IOException("Already stopped the shell interpreter.");
		}
		stop = true;
	};

}
