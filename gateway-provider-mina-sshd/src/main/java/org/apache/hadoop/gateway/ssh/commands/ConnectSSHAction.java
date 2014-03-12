package org.apache.hadoop.gateway.ssh.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.agent.unix.AgentClient;
import org.apache.sshd.agent.unix.AgentServer;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.NoCloseOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectSSHAction extends SSHAction {

	public static class SSHConnector {

		public int connectSSH(String host, int port, String user, KeyPair key,
				BufferedReader inputStream, OutputStream outputStream,
				OutputStream error) {
			AgentServer agentServer = null;
			AgentClient agentClient = null;
			ChannelShell channelShell = null;
			int exit = 0;
			InputStream tee = null;
			try {
				SshClient client = new SshClient();
				agentServer = new AgentServer();
				String authSock = agentServer.start();
				agentClient = new AgentClient(authSock);

				agentClient.addIdentity(key, "Knox user key");

				ConnectFuture future = client.connect(user, host, port);
				ClientSession session = future.await().getSession();
				int connectCode = session.waitFor(ClientSession.CLOSED
						| ClientSession.AUTHED, 0);

				if ((connectCode & ClientSession.CLOSED) != 0) {
					PrintWriter errorOut = new PrintWriter(
							new NoCloseOutputStream(error));
					errorOut.println("Failed to connect to " + host + ":"
							+ port);
					errorOut.close();
					return SSH_ERROR_CODE;
				}
				channelShell = session.createShellChannel();
				channelShell.setAgentForwarding(true);
				InputStream shellStream = new ReaderInputStream(inputStream);
				PipedInputStream loggingInputStream = new PipedInputStream();
				PipedOutputStream loggingOutputStream = new PipedOutputStream(
						loggingInputStream);
				tee = new TeeInputStream(shellStream, loggingOutputStream, true);
				channelShell.setIn(tee);
				channelShell.setOut(new NoCloseOutputStream(outputStream));
				channelShell.setOut(new NoCloseOutputStream(error));
				channelShell.open().await();
				channelShell.waitFor(ClientChannel.CLOSED, 0);
				exit = channelShell.getExitStatus();
				session.close(false);
				agentServer.close();
				agentClient.close();
			} catch (Exception e) {
				exit = SSH_ERROR_CODE;
			} finally {
				if (agentServer != null) {
					agentServer.close();
				}
				if (agentClient != null) {
					agentClient.close();
				}
				if (channelShell != null) {
					channelShell.close(false);
				}
				if (tee != null) {
					try {
						tee.close();
					} catch (IOException e) {
						LOG.error("Unable to close logging tee. "
								+ "Audit information may have been lost.", e);
					}
				}
			}
			return exit;
		}

	}

	public static int SSH_ERROR_CODE = 255;
	private static final Logger LOG = LoggerFactory.getLogger(SSHAction.class);
	private Matcher matcher = Pattern.compile(
			"\\s*([a-zA-Z0-9.]+)(?::([0-9])+)?\\s*$").matcher("");
	private final String user;
	private final String keyFile;
	private SSHConnector sshConnector;

	public ConnectSSHAction(String user, String keyFile) {
		this(user, keyFile, new SSHConnector());
	}

	public ConnectSSHAction(String user, String keyFile, SSHConnector connector) {
		super("connect", "<host>[:<port>]",
				"Connect to a server within the Knox cluster.");
		this.user = user;
		this.keyFile = keyFile;
	}

	@Override
	public int handleCommand(String command, String commandLine,
			BufferedReader inputStream, OutputStream outputStream,
			OutputStream error) {
		matcher.reset(commandLine);
		if (matcher.matches()) {
			KeyPair key = new FileKeyPairProvider(new String[] { keyFile })
					.loadKeys().iterator().next();
			String host = matcher.group(1);
			String portString = matcher.group(2);
			int port;
			if (portString != null) {
				port = Integer.parseInt(portString);
			} else {
				port = 22;
			}
			return sshConnector.connectSSH(host, port, portString, key,
					inputStream, outputStream, error);
		} else {
			PrintWriter errorWriter = new PrintWriter(new NoCloseOutputStream(
					error));
			errorWriter.println("Invalid argument: " + commandLine);
			errorWriter
					.println("please use a hostname of the form <host>[:<port>]");
			return SSH_ERROR_CODE;
		}
	}

}
