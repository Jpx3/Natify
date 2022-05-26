package de.jpx3.natify.translation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public final class AsyncTaskProcessor {
  private static int pool = 0;
  public ThreadPoolExecutor executor;
  private CountDownLatch countDownLatch;
  private int tasksToAccomplish = 0;

  public void aquire(int tasksToAccomplish) {
    this.tasksToAccomplish = tasksToAccomplish;
    this.countDownLatch = new CountDownLatch(tasksToAccomplish);
    int threads = Runtime.getRuntime().availableProcessors() / 2;
    this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads, new ThreadFactory() {
      final int pool = AsyncTaskProcessor.pool++;
      int thread = 0;

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "natify-executor-" + (pool) + "-" + (this.thread++));
        thread.setPriority(Thread.MAX_PRIORITY);
        return thread;
      }
    });
  }

  public void push(Runnable runnable) {
    executor.execute(() -> {
      runnable.run();
      countDownLatch.countDown();
    });
  }

  public void await() {
    try {
      countDownLatch.await();
      executor.shutdownNow();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
}
