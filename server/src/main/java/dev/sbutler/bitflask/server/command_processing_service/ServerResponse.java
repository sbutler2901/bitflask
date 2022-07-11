package dev.sbutler.bitflask.server.command_processing_service;

import java.util.Optional;

public record ServerResponse(Status status, Optional<String> response,
                             Optional<String> errorMessage) {

  public enum Status {
    OK,
    FAILED
  }
}
