package statechart.legacy.eventqueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import statechart.Event;
import statechart.Metadata;
import statechart.Statechart;
import statechart.StatechartThreadFactory;
import statechart.eventqueue.EventQueue;

/**
 * The event queue implementation using a thread pool
 */
public class ThreadEventQueue implements EventQueue {

  /**
   * The thread queue entry for an asynchronously dispatched event
   */
  class EventQueueEntry implements Runnable {
    private Statechart statechart;
    private Metadata data;
    private Event event;

    public EventQueueEntry(Statechart statechart, Metadata data, Event event) {
      this.statechart = statechart;
      this.data = data;
      this.event = event;
    }

    @Override
    public void run() {
      // events are always dispatched to the statechart
      // so it should be active otherwise something went wrong
      assert (data.isActive(statechart));
      synchronized (data) {
        statechart.dispatch(data, event);
      }
      statechart = null;
      data = null;
      event = null;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("EventQueueEntry [statechart=");
      builder.append(statechart);
      builder.append(", data=(");
      builder.append(data);
      builder.append("), event=");
      builder.append(event);
      builder.append("]");
      return builder.toString();
    }
  }

  /**
   * The thread pool handling the asynchronously dispatched events and also
   * Timeout events
   * 
   * @see #DelayQueueTimeoutEntry
   */
  private ExecutorService threadPool = null;
  private Statechart chart;

  /**
   * Instantiates the event queue based on thread pooling.
   * 
   * @param name name of thread group holding the threads of the pool
   * @param threads number of threads, at least two are required
   * @param makeDaemonThreads whether to create the threads as daemon rather
   *          than user threads
   */
  public ThreadEventQueue(Statechart chart, String name, int threads, boolean makeDaemonThreads) {
    /**
     * we need at least two threads for asynchronously dispatched event handling
     * and for timeout event handling
     */
    if (threads < 2) {
      threads = 2;
    }
    this.chart = chart;
    threadPool = Executors.newFixedThreadPool(threads, new StatechartThreadFactory(name, makeDaemonThreads));
  }

  public ExecutorService getThreadpool() {
    return threadPool;
  }

  @Override
  public boolean addEvent(Metadata data, Event event) {
    threadPool.execute(new EventQueueEntry(chart, data, event));
    return true;
  }

  @Override
  public boolean shutdown(long milliseconds) {
    threadPool.shutdown();
    try {
      return threadPool.awaitTermination(milliseconds, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean isShutdown() {
    return threadPool.isShutdown();
  }
}
