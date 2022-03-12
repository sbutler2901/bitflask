package dev.sbutler.bitflask.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single self-contained file for storing data
 */
class StorageSegment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private final StorageSegmentFile storageSegmentFile;
  private final ConcurrentMap<String, StorageEntry> keyStorageEntryMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong(0);

  public StorageSegment(StorageSegmentFile storageSegmentFile) {
    this.storageSegmentFile = storageSegmentFile;
  }

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   */
  public void write(String key, String value) {
    byte[] encodedKeyAndValue = encodeKeyAndValue(key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedKeyAndValue.length);

    try {
      storageSegmentFile.write(encodedKeyAndValue, writeOffset);
      createAndAddNewStorageEntry(key, value, writeOffset);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private byte[] encodeKeyAndValue(String key, String value) {
    String keyAndValueCombined = key + value;
    return keyAndValueCombined.getBytes(StandardCharsets.UTF_8);
  }

  private void createAndAddNewStorageEntry(String key, String value, long offset) {
    StorageEntry storageEntry = new StorageEntry(offset, key.length(), value.length());
    // Handle newer value being written and added in another thread for same key
    keyStorageEntryMap.merge(key, storageEntry, (retrievedStorageEntry, writtenStorageEntry) ->
        retrievedStorageEntry.getSegmentOffset() < writtenStorageEntry.getSegmentOffset()
            ? writtenStorageEntry
            : retrievedStorageEntry
    );
  }

  /**
   * Reads the provided key's value from the segment file
   *
   * @param key the key to find the data in the segment file
   * @return the value for the key from the segment file, if it exists
   */
  public Optional<String> read(String key) {
    if (!containsKey(key)) {
      return Optional.empty();
    }

    StorageEntry storageEntry = keyStorageEntryMap.get(key);
    try {
      byte[] readBytes = storageSegmentFile.read(storageEntry.getTotalLength(),
          storageEntry.getSegmentOffset());
      String value = decodeValue(readBytes, storageEntry.getKeyLength());
      return Optional.of(value);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  private String decodeValue(byte[] readBytes, int keyLength) {
    String entry = new String(readBytes).trim();
    return entry.substring(keyLength);
  }

  /**
   * Checks if the segment contains the provided key
   *
   * @param key the key to be searched for
   * @return whether it contains the key, or not
   */
  public boolean containsKey(String key) {
    return keyStorageEntryMap.containsKey(key);
  }

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > NEW_SEGMENT_THRESHOLD;
  }

}
