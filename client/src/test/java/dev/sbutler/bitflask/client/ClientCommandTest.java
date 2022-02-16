package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ClientCommandTest {

  @Test
  void getAsRespArray() {
    String command = "GET";
    List<String> args = List.of("test-key");
    RespArray expected = new RespArray(List.of(
        new RespBulkString(command),
        new RespBulkString(args.get(0))
    ));
    ClientCommand clientCommand = new ClientCommand(command, args);

    assertEquals(expected, clientCommand.getAsRespArray());
  }
}
