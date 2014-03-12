package org.apache.hadoop.gateway.ssh.audit;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.apache.hadoop.gateway.ssh.shell.KnoxTunnelShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditorWork {

	private static final Logger LOG = LoggerFactory
			.getLogger(AuditorWork.class);

	private final BufferedReader reader;
	private final BlockingQueue<Runnable> queue;
	final String user;
	private final KnoxTunnelShell originatingShell;
	final String resource;

	public AuditorWork(String resource, String user, BufferedReader buffer,
			KnoxTunnelShell originatingShell, BlockingQueue<Runnable> queue) {
		this.reader = buffer;
		this.queue = queue;
		this.user = user;
		this.resource = resource;
		this.originatingShell = originatingShell;
	}

	public KnoxTunnelShell getOriginatingShell() {
		return originatingShell;
	}

	public BlockingQueue<Runnable> getQueue() {
		return queue;
	}

	public BufferedReader getReader() {
		return reader;
	}

	public String getResource() {
		return resource;
	}

	public String getUser() {
		return user;
	}

	public void run() {
		try {
			if (reader.ready()) {

			}
		} catch (IOException e) {
			LOG.error(
					"Unable to read piped audit log stream. "
							+ "Audits may have been lost. Killing the original shell.",
					e);
			originatingShell.destroy();
		}
	}
}