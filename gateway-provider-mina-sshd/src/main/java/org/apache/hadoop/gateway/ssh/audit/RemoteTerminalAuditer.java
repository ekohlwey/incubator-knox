package org.apache.hadoop.gateway.ssh.audit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.hadoop.gateway.ssh.shell.KnoxTunnelShell;

public class RemoteTerminalAuditer {

	private static final RemoteTerminalAuditer INSTANCE = new RemoteTerminalAuditer();

	public static RemoteTerminalAuditer get() {
		return INSTANCE;
	}

	private boolean shutdown;

	private RemoteTerminalAuditer() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				RemoteTerminalAuditer.this.shutdown = true;
			}
		});

	}

	public void auditStream(InputStream in, String resource, String user,
			KnoxTunnelShell originatingShell) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

	}

}
