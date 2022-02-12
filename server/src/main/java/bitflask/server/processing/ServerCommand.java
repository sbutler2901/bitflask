package bitflask.server.processing;

import bitflask.resp.RespArray;
import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

record ServerCommand(ServerCommands command, List<String> args) {

  public static ServerCommand valueOf(@NonNull RespType<?> commandMessage) {
    if (!(commandMessage instanceof RespArray clientMessageRespArray)) {
      throw new IllegalArgumentException("Message must be a RespArray");
    }

    ServerCommands command = getCommandFromMessage(clientMessageRespArray.getValue());
    List<String> args = getArgsFromMessage(clientMessageRespArray.getValue());

    if (!ServerCommands.isValidCommandArgs(command, args)) {
      throw new IllegalArgumentException(
          "Invalid arguments for the command: " + command + ", " + args);
    }

    return new ServerCommand(command, args);
  }

  private static ServerCommands getCommandFromMessage(List<RespType<?>> commandMessageArgs) {
    if (!(commandMessageArgs.get(0) instanceof RespBulkString commandBulkString)) {
      throw new IllegalArgumentException("Message args must be RespBulkStrings");
    }

    String normalizedCommandString = commandBulkString.getValue().trim().toUpperCase();
    try {
      return ServerCommands.valueOf(normalizedCommandString);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown command: " + normalizedCommandString);
    }
  }

  private static List<String> getArgsFromMessage(List<RespType<?>> commandMessageArgs) {
    List<String> args = new ArrayList<>();

    for (int i = 1; i < commandMessageArgs.size(); i++) {
      if (!(commandMessageArgs.get(i) instanceof RespBulkString clientArgBulkString)) {
        throw new IllegalArgumentException("Message args must be RespBulkStrings");
      }
      args.add(clientArgBulkString.getValue());
    }

    return args;
  }
}
