package org.apache.hadoop.gateway.ssh.audit;

import java.io.IOException;

import org.apache.hadoop.gateway.ssh.shell.KnoxTunnelShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

	Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

	public void handleError(IOException e, KnoxTunnelShell originatingShell) {
		LOG.error("Unable to read piped audit log stream. "
				+ "Audits may have been lost. Killing the original shell.",
				e);
		originatingShell.destroy();
	}

}