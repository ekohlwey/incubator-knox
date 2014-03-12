package org.apache.hadoop.gateway.ssh;

import org.apache.hadoop.gateway.ssh.shell.KnoxTunnelShell;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

public class KnoxTunnelShellFactory implements Factory<Command>{
	
	@Override
	public Command create() {
		return new KnoxTunnelShell();
	}
}
