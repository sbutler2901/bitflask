package dev.sbutler.bitflask.storage.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationConstants.StorageSegmentCreationModeArgs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;

public class StorageConfigurationTest {

  @Test
  void propertyFile() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG)),
        storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(
        Path.of(defaultProvider.getDefaultValueFor(
            StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG)),
        storageConfiguration.getStorageStoreDirectoryPath());
    assertEquals(Long.parseLong(defaultProvider.getDefaultValueFor(
            StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG)),
        storageConfiguration.getStorageSegmentSizeLimit());
    assertEquals(StandardOpenOption.CREATE, storageConfiguration.getStorageSegmentCreationMode());
    assertEquals(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG)),
        storageConfiguration.getStorageCompactionThreshold());
  }

  @Test
  void propertyFile_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("~/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_segmentSizeLimit() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_storageSegmentCreationMode() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn("append").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_NAME));
  }

  @Test
  void propertyFile_illegalConfiguration_compactionThreshold() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(
            defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG));
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    Path expectedSegmentDirPath = Paths.get("/random/absolute/path");
    String[] argv = new String[]{
        StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        expectedSegmentDirPath.toString(),
        StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.TRUNCATE.getRawArg(),
        StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(expectedSegmentDirPath, storageConfiguration.getStorageStoreDirectoryPath());
    assertEquals(200, storageConfiguration.getStorageSegmentSizeLimit());
    assertEquals(1, storageConfiguration.getStorageCompactionThreshold());
  }

  @Test
  void commandLineFlags_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "-1",
        StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "~/random/relative/path",
        StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_segmentSizeLimitFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "-1",
        StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_storageSegmentCreationMode() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        "append",
        StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_NAME));
  }

  @Test
  void commandLineFlags_illegalConfiguration_segmentCompactionThresholdFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "-1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationConstants.STORAGE_COMPACTION_THRESHOLD_FLAG));
  }
}
