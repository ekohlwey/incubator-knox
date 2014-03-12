package org.apache.hadoop.gateway.ssh;

import org.apache.hadoop.gateway.audit.api.Action;
import org.apache.hadoop.gateway.audit.api.ActionOutcome;
import org.apache.hadoop.gateway.audit.api.AuditServiceFactory;
import org.apache.hadoop.gateway.audit.api.Auditor;
import org.apache.hadoop.gateway.audit.api.ResourceType;
import org.apache.hadoop.gateway.audit.log4j.audit.AuditConstants;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.auth.gss.UserAuthGSS;
import org.apache.sshd.server.session.ServerSession;

public class KnoxUserAuthGSS extends UserAuthGSS {

	private static final Auditor AUDITOR = AuditServiceFactory
			.getAuditService().getAuditor(AuditConstants.DEFAULT_AUDITOR_NAME,
					AuditConstants.KNOX_SERVICE_NAME,
					AuditConstants.KNOX_COMPONENT_NAME);

	public static class Factory extends UserAuthGSS.Factory {
		public org.apache.sshd.server.UserAuth create() {

			return new KnoxUserAuthGSS();
		};
	}

	@Override
	public Boolean auth(ServerSession session, String username, String service,
			Buffer buffer) throws Exception {

		Boolean auth = super.auth(session, username, service, buffer);
		if (auth == null) {
			// continue
		} else if (auth) {
			AUDITOR.audit(Action.AUTHENTICATION, username,
					ResourceType.PRINCIPAL, ActionOutcome.SUCCESS);
		} else {
			AUDITOR.audit(Action.AUTHENTICATION, username,
					ResourceType.PRINCIPAL, ActionOutcome.FAILURE);
		}
		return auth;
	}

}
