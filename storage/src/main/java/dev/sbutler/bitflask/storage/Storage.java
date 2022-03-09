package dev.sbutler.bitflask.storage;

import java.io.IOException;
import java.util.Optional;

public interface Storage {

  void write(String key, String value) throws IOException, IllegalArgumentException;

  Optional<String> read(String key);
}
