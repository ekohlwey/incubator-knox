package org.apache.hadoop.gateway.ssh.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hadoop.gateway.ssh.commands.ConnectSSHAction;
import org.apache.hadoop.gateway.ssh.commands.HelpSSHAction;
import org.apache.hadoop.gateway.ssh.commands.SSHAction;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnoxTunnelShell implements Command {

	private static final Logger LOG = LoggerFactory
			.getLogger(KnoxTunnelShell.class);

	private Map<String, SSHAction> shellActions = new HashMap<String, SSHAction>();
	private ExitCallback exitCallback;
	private OutputStream errorStream;
	private OutputStream outputStream;
	private final ShellExitHandler exitHandler = new ShellExitHandler(this);
	private final ShellInterpreterThread interpreterThread = new ShellInterpreterThread(
			this, exitHandler, shellActions);

	private BufferedReader reader;

	@Override
	public void destroy() {
		try {
			interpreterThread.close();
		} catch (IOException e) {
			LOG.error("Error while closing interpreter thread.", e);
		}
	}

	@Override
	public void setErrorStream(OutputStream arg0) {
		this.errorStream = arg0;
	}

	@Override
	public void setExitCallback(ExitCallback arg0) {
		this.exitCallback = arg0;
	}

	@Override
	public void setInputStream(InputStream arg0) {
		this.reader = new BufferedReader(new InputStreamReader(
				new NoCloseInputStream(arg0)));
	}

	@Override
	public void setOutputStream(OutputStream arg0) {
		this.outputStream = arg0;
	}

	@Override
	public void start(Environment arg0) throws IOException {
		Map<String, String> env = arg0.getEnv();
		final String user = env.get(Environment.ENV_USER);
		List<SSHAction> actions = new ArrayList<SSHAction>();
		actions.add(new ConnectSSHAction(user, null));
		actions.add(new HelpSSHAction(shellActions));
		interpreterThread.start();
	}

	public ExitCallback getExitCallback() {
		return exitCallback;
	}

	public OutputStream getErrorStream() {
		return errorStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * Get the input stream, as a buffered reader. Since the lines typed into
	 * the console are audited, we use a reader for IO. If an input stream is
	 * required, wrap this buffer in a {@link ReaderInputStream}.
	 */
	public BufferedReader getReader() {
		return reader;
	}

}
