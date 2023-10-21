package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;

/** Commands for the client unrelated to Bitflask. */
public sealed interface LocalCommand extends ClientCommand {

  /** The user has requested help. */
  final class Help implements LocalCommand {

    private final OutputWriter outputWriter;

    public Help(OutputWriter outputWriter) {
      this.outputWriter = outputWriter;
    }

    @Override
    public boolean execute() {
      outputWriter.writeWithNewLine("I can't help you.");
      return true;
    }

    public static boolean commandStringMatches(String command) {
      return "HELP".equalsIgnoreCase(command.trim());
    }
  }

  /** The user has requested for the Client to exit. */
  final class Exit implements LocalCommand {

    @Override
    public boolean execute() {
      return false;
    }

    public static boolean commandStringMatches(String command) {
      return "EXIT".equalsIgnoreCase(command.trim());
    }
  }

  /** The user has provided an unknown command. */
  final class Unknown implements LocalCommand {

    private final OutputWriter outputWriter;
    private final String command;

    public Unknown(OutputWriter outputWriter, String command) {
      this.outputWriter = outputWriter;
      this.command = command;
    }

    @Override
    public boolean execute() {
      outputWriter.writeWithNewLine(String.format("Unknown command [%s].", command));
      return true;
    }
  }
}
