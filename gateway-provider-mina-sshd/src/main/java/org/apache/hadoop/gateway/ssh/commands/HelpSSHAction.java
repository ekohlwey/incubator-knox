package org.apache.hadoop.gateway.ssh.commands;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.apache.sshd.common.util.NoCloseOutputStream;

public class HelpSSHAction extends SSHAction {

	private final Map<String, SSHAction> actions;

	public HelpSSHAction(Map<String, SSHAction> actions) {
		super("help", "", "Print this help message.");
		this.actions = actions;
	}

	@Override
	public int handleCommand(String command, String commandLine, BufferedReader inputStream,
			OutputStream outputStream, OutputStream error) {
		PrintStream printStream = new PrintStream(new NoCloseOutputStream(
				outputStream));
		int longestCommand = 0;
		int longestArgs = 0;
		for (SSHAction action : actions.values()) {
			longestCommand = Math.max(longestCommand, action.getCommand()
					.length());
			longestArgs = Math
					.max(longestArgs, action.getArgGrammar().length());
		}
		printStream.print("Knox SSH Provider help.\n");
		String formatString = "%" + longestCommand + "s %-" + longestArgs
				+ "s %s\n";
		for (SSHAction action : actions.values()) {
			printStream.format(formatString, action.getCommand(),
					action.getArgGrammar(), action.getDescription());
		}
		printStream.close();
		return 0;
	}
}
