package org.apache.hadoop.gateway.ssh.audit;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.hadoop.gateway.audit.api.Action;
import org.apache.hadoop.gateway.audit.api.ActionOutcome;
import org.apache.hadoop.gateway.audit.api.AuditServiceFactory;
import org.apache.hadoop.gateway.audit.api.Auditor;
import org.apache.hadoop.gateway.audit.api.ResourceType;
import org.apache.hadoop.gateway.audit.log4j.audit.AuditConstants;

public class TerminalAuditor {

	private final ErrorHandler handler;

	public TerminalAuditor(ErrorHandler handler) {
		this.handler = handler;
	}

	private static final Auditor AUDITOR = AuditServiceFactory
			.getAuditService().getAuditor(
					AuditConstants.DEFAULT_AUDITOR_NAME,
					AuditConstants.KNOX_SERVICE_NAME,
					AuditConstants.KNOX_COMPONENT_NAME);

	public void auditWork(AuditorWork work) {
		BufferedReader reader = work.getReader();
		String line;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			handler.handleError(e, work.getOriginatingShell());
			return;
		}
		String user = work.user;
		String resource = work.resource;
		if (line != null) {
			AUDITOR.audit(Action.ACCESS,
					user + "@" + resource + ":" + line,
					ResourceType.TOPOLOGY, ActionOutcome.UNAVAILABLE);
		} else {
			try {
				reader.close();
			} catch (IOException e) {
				handler.handleError(e, work.getOriginatingShell());
				return;
			}
		}
	}
}