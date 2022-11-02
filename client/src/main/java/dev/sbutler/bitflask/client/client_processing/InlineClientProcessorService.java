package dev.sbutler.bitflask.client.client_processing;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.ReplIOException;
import dev.sbutler.bitflask.client.client_processing.repl.ReplParser;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import dev.sbutler.bitflask.client.client_processing.repl.ReplSyntaxException;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;

public class InlineClientProcessorService implements ClientProcessorService {

  public static class Factory {

    private final ClientProcessor clientProcessor;
    private final OutputWriter outputWriter;

    @Inject
    public Factory(ClientProcessor clientProcessor, OutputWriter outputWriter) {
      this.clientProcessor = clientProcessor;
      this.outputWriter = outputWriter;
    }

    public InlineClientProcessorService create(ReplReader replReader) {
      return new InlineClientProcessorService(clientProcessor, replReader, outputWriter);
    }
  }

  private final ClientProcessor clientProcessor;
  private final ReplReader replReader;
  private final OutputWriter outputWriter;

  private InlineClientProcessorService(ClientProcessor clientProcessor, ReplReader replReader,
      OutputWriter outputWriter) {
    this.clientProcessor = clientProcessor;
    this.replReader = replReader;
    this.outputWriter = outputWriter;
  }

  @Override
  public void run() {
    try {
      Optional<ImmutableList<ReplElement>> clientInputOptional =
          ReplParser.readNextLine(replReader);
      if (clientInputOptional.isEmpty()) {
        triggerShutdown();
        return;
      }
      clientInputOptional.ifPresent(this::processClientInput);
    } catch (ReplSyntaxException | ReplIOException e) {
      outputWriter.writeWithNewLine(e.getMessage());
    }
  }

  private void processClientInput(ImmutableList<ReplElement> clientInput) {
    boolean shouldContinueProcessing = clientProcessor.processClientInput(clientInput);
    if (!shouldContinueProcessing) {
      triggerShutdown();
    }
  }

  @Override
  public void triggerShutdown() {
    try {
      replReader.close();
    } catch (IOException e) {
      // ignored
    }
  }
}
