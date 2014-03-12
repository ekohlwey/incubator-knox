package org.apache.hadoop.gateway.ssh;

public class SSHServerException extends RuntimeException {

	private static final long serialVersionUID = 271867044416800823L;

	public SSHServerException(Throwable source) {
		super(source);
	}

}
