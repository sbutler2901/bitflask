package dev.sbutler.bitflask.client.repl;

import java.util.List;

public enum ReplCommand {
  EXIT,
  TEST,
  HELP;

  public static boolean isReplCommand(String command) {
    if (command == null) {
      return false;
    }
    String compareString = command.trim().toUpperCase();
    return compareString.equals(EXIT.toString())
        || compareString.equals(TEST.toString())
        || compareString.equals(HELP.toString());
  }

  public static boolean isValidReplCommand(ReplCommand replCommand, List<String> args) {
    return switch (replCommand) {
      case EXIT, HELP -> args == null || args.size() == 0;
      case TEST -> args != null && args.size() == 1;
    };
  }
}
