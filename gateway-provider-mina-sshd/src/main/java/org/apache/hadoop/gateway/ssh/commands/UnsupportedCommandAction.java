package org.apache.hadoop.gateway.ssh.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.sshd.common.util.NoCloseOutputStream;

public class UnsupportedCommandAction extends SSHAction {

	public UnsupportedCommandAction() {
		super(null, null, null);
	}

	@Override
	public int handleCommand(String command, String commandLine, BufferedReader inputStream,
			OutputStream outputStream, OutputStream error) throws IOException {
		PrintStream errorWriter = new PrintStream(new NoCloseOutputStream(error));
		errorWriter.println("-knox: " + command + ": command not found");
		errorWriter.println("type 'help' for a list of commands");
		errorWriter.close();
		return 127;
	}

}
