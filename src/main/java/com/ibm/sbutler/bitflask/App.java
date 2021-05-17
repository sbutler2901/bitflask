package com.ibm.sbutler.bitflask;

import com.ibm.sbutler.bitflask.storage.Storage;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class App {

  private static final int NUM_THREADS = 4;

  private final Storage storage;
  private final ThreadPoolExecutor threadPool;

  App() throws IOException {
    this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);
    this.storage = new Storage(threadPool);
  }

  App(Storage storage) {
    this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);
    this.storage = storage;
  }

  public static void main(String[] args) {
    try {
      App app = new App();
      app.start();
      System.exit(0);
    } catch (IOException e) {
      System.out.println("Unable to initialize storage engine. Terminating");
      e.printStackTrace();
      System.exit(1);
    } catch (InterruptedException e) {
      System.out.println("Issue occurred putting main to sleep");
      e.printStackTrace();
    }
  }

  private void printConfigInfo() {
    System.out
        .printf("Runtime processors available (%s)%n", Runtime.getRuntime().availableProcessors());
  }

  private void start() throws InterruptedException {
    System.out.println("Welcome to Bitflask!");
    printConfigInfo();
    REPL repl = new REPL(this.storage);
    this.threadPool.execute(repl);
    while (threadPool.getActiveCount() > 0) {
      Thread.sleep(10000);
    }
  }
}
