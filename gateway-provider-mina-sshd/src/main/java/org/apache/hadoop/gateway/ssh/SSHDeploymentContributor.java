package org.apache.hadoop.gateway.ssh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.gateway.deploy.DeploymentContext;
import org.apache.hadoop.gateway.deploy.ProviderDeploymentContributorBase;
import org.apache.hadoop.gateway.descriptor.FilterParamDescriptor;
import org.apache.hadoop.gateway.descriptor.ResourceDescriptor;
import org.apache.hadoop.gateway.topology.Provider;
import org.apache.hadoop.gateway.topology.Service;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.gss.GSSAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

public class SSHDeploymentContributor extends ProviderDeploymentContributorBase {
	
	private final Thread shutdownHandler = new Thread(){
		public void run() {
			if(sshd!=null){
				try{
					sshd.stop(true);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
	};

	public static final String SSH_PORT = "port";
	public static final String SSH_FINGERPRINT_LOCATION = "ssh-fingerprint-location";
	public static final String KEYTAB_LOCATION = "keytab";
	public static final String PROVIDER_PRINCIPAL = "kerberos-principal";
	public static final String WORKERS = "workers";
	private SshServer sshd;

	@Override
	public String getRole() {
		return "ssh";
	}

	@Override
	public String getName() {
		return "SshProvider";
	}

	@Override
	public void contributeFilter(DeploymentContext context, Provider provider,
			Service service, ResourceDescriptor resource,
			List<FilterParamDescriptor> params) {
		// noop
	}

	@Override
	public void contributeProvider(DeploymentContext context, Provider provider)
			throws SSHServerException {
		Map<String, String> providerParams = provider.getParams();
		int port = Integer.parseInt(providerParams.get(SSH_PORT));
		String sshLocation = providerParams.get(SSH_FINGERPRINT_LOCATION);
		if (sshLocation == null) {
			sshLocation = "/var/lib/knox/ssh.fingerprint";
		}
		String keytabLocation = providerParams.get(KEYTAB_LOCATION);
		if (keytabLocation == null) {
			keytabLocation = "/etc/knox/conf/knox.service.keytab";
		}
		String servicePrincipal = providerParams.get(PROVIDER_PRINCIPAL);
		String workersString = providerParams.get(WORKERS);
		int workers;
		if (workersString != null) {
			workers = Integer.parseInt(workersString);
		} else {
			workers = -1;
		}

		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(port);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(sshLocation));
		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>(
				1);
		userAuthFactories.add(new KnoxUserAuthGSS.Factory());
		sshd.setUserAuthFactories(userAuthFactories);

		GSSAuthenticator authenticator = new GSSAuthenticator();
		authenticator.setKeytabFile(keytabLocation);
		if (servicePrincipal != null) {
			authenticator.setServicePrincipalName(servicePrincipal);
		}
		sshd.setGSSAuthenticator(authenticator);
		if (workers > 0) {
			sshd.setNioWorkers(workers);
		}
		sshd.setShellFactory(new KnoxTunnelShellFactory());
		try {
			sshd.start();
			Runtime.getRuntime().addShutdownHook(shutdownHandler);
		} catch (IOException e) {
			throw new SSHServerException(e);
		}
	}

}
