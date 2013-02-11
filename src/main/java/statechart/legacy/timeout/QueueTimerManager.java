package statechart.legacy.timeout;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.apache.log4j.Logger;
import statechart.*;
import statechart.eventqueue.EventQueue;
import statechart.legacy.eventqueue.ThreadEventQueue;
import statechart.timeout.TimerManager;

public class QueueTimerManager implements Runnable, TimerManager {

  private static Logger log = Logger.getLogger(QueueTimerManager.class);

  class TimerHandle implements statechart.timeout.TimerHandle {
    QueueTimerEntry entry;

    TimerHandle(QueueTimerEntry entry) {
      this.entry = entry;
    }
  }

  private ExecutorService threadpool;
  private DelayQueue<QueueTimerEntry> timeoutEntryQueue = new DelayQueue<QueueTimerEntry>();
  private Statechart chart;
  private boolean shutdownTriggered = false;

  public QueueTimerManager(Statechart chart, EventQueue eventQueue) {
    this.chart = chart;
    // DelayQueueTimeoutManager is coupled with the ThreadPoolEventQueue
    this.threadpool = ((ThreadEventQueue)eventQueue).getThreadpool();
    log.debug("started");
    threadpool.execute(this);
  }

  @Override
  public TimerHandle startTimer(State state, Metadata data, TimeoutEvent event) {
    QueueTimerEntry newEntry = new QueueTimerEntry(chart, state, data, event);
    timeoutEntryQueue.add(newEntry);

    // return the handle wrapping our internal entry
    TimerHandle timerHandle = new TimerHandle(newEntry);
    log.debug("start timer: " + Integer.toHexString(timerHandle.hashCode()) + " at state: " + state.toString());
    return timerHandle;
  }

  @Override
  public void cancelTimer(statechart.timeout.TimerHandle timerHandle) {
    // get our internal entry
    QueueTimerEntry entryToRemove = ((TimerHandle)timerHandle).entry;
    // shouldn't really necessary with Run-to-Completion:
    entryToRemove.invalid = true;

    boolean rc = timeoutEntryQueue.remove(entryToRemove);
    log.debug("cancel timer: " + Integer.toHexString(timerHandle.hashCode()) + " rc:" + rc);
  }

  @Override
  public boolean shutdown(long waitMilliseconds) {
    log.info("shutdown triggered");
    shutdownTriggered = true;
    timeoutEntryQueue.clear();
    // add a null event to the queue to return form the delay queue's take()
    // method in run()
    timeoutEntryQueue.add(new QueueTimerEntry());
    return true;
  }

  /**
   * Dequeues elements from the timeout queue and dispatches them
   */
  @Override
  public void run() {
    while (!shutdownTriggered && !threadpool.isShutdown()) {
      try {
        QueueTimerEntry entry = timeoutEntryQueue.take();
        if (shutdownTriggered || threadpool.isShutdown()) {
          break;
        }
        log.debug("after take() and execute now");
        threadpool.execute(entry);
      } catch (InterruptedException e) {
        // ignore the exception. Just run the next loop and take method if
        // necessary
      } catch (RejectedExecutionException e) {
        // Normally this means that the thread pool is in shut down
      }
    }
    log.info("shutdown completed");
  }
}
