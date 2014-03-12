package org.apache.hadoop.gateway.ssh.audit;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class TerminalAuditThread extends Thread {

	private final ArrayBlockingQueue<AuditorWork> workQueue;
	private boolean stop;
	private final ReentrantLock stopLock = new ReentrantLock();
	private final TerminalAuditor auditor;

	public TerminalAuditThread(ArrayBlockingQueue<AuditorWork> workQueue,
			TerminalAuditor auditor) {
		this.workQueue = workQueue;
		this.auditor = auditor;
		setDaemon(true);
	}

	@Override
	public void run() {
		boolean run = true;
		while (run) {
			AuditorWork work;
			try {
				work = workQueue.take();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				continue;
			}
			auditor.auditWork(work);

			stopLock.lock();
			try {
				run = !stop;
			} finally {
				stopLock.unlock();
			}
		}
	}

	public void close() {
		stopLock.lock();
		stop = true;
		stopLock.unlock();
	}
}