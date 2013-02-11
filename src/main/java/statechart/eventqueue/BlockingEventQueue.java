package statechart.eventqueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import statechart.Event;
import statechart.Metadata;
import statechart.Statechart;
import statechart.StatechartThreadFactory;

/**
 * Implements event queuing functions by means of a blocking queue.
 * 
 * @See {@link LinkedBlockingQueue}
 */
public class BlockingEventQueue implements EventQueue, Runnable {

  private static Logger log = Logger.getLogger(BlockingEventQueue.class);

  /** name of the thread handling the events */
  static final String THREAD_GROUP_NAME = "event";
  /** maximum event queue capacity */
  static final int MAX_QUEUE_CAPACITY = 1000;
  /**
   * maximum time waiting if necessary up to the specified wait time for space
   * to become available in the event queue
   */
  static final int MAX_WAIT_TIME_OFFER_MS = 1000;

  /**
   * the queue entry with data the event needs for execution (dispatching) of
   * the event
   */
  static class EventQueueEntry {
    private Statechart statechart;
    private Metadata data;
    private Event event;

    public EventQueueEntry(Statechart statechart, Metadata data, Event event) {
      this.statechart = statechart;
      this.data = data;
      this.event = event;
    }
  }

  /** executor to manage the needed threads (here only one thread is started) */
  private ExecutorService executor = Executors.newCachedThreadPool(new StatechartThreadFactory(THREAD_GROUP_NAME));
  /** the event queue holding a series of event queue entries */
  private LinkedBlockingQueue<EventQueueEntry> eventQueue = new LinkedBlockingQueue<EventQueueEntry>(MAX_QUEUE_CAPACITY);

  /** the statechart this event queue works for */
  private Statechart chart;
  /** is a shutdown of the event queue triggered */
  private boolean shutdownTriggered = false;
  /** is the event queue completely down */
  private volatile boolean isShutdownCompletted = false;
  /** semaphore used to wait for the event queue shutdown */
  private Semaphore shutdownSema = new Semaphore(1);

  /*
   * Creates and starts the event queue. An event dispatching thread is started
   * to take the event from the head of the queue and executes (dispatches) it.
   */
  public BlockingEventQueue(Statechart chart) {
    this.chart = chart;
    // start the one event dispatching thread
    executor.execute(this);
  }

  /**
   * The event handling thread takes the next event from the head of the queue
   * (if there is any) and executes (dispatches) the event.
   */
  @Override
  public void run() {
    log.info("started");
    try {
      shutdownSema.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
      return;
    }
    while (!shutdownTriggered) {
      try {
        EventQueueEntry eventQueueEntry = eventQueue.take();
        if (shutdownTriggered) {
          break;
        }
        log.debug("after take() dispatch event: " + String.valueOf(eventQueueEntry.event));
        eventQueueEntry.statechart.dispatch(eventQueueEntry.data, eventQueueEntry.event);
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }
    }
    eventQueue.clear();
    isShutdownCompletted = true;
    log.info("shutdown completed");
    shutdownSema.release();
  }

  @Override
  public boolean addEvent(Metadata data, Event event) {
    log.debug(String.valueOf(event));
    try {
      if (eventQueue.offer(new EventQueueEntry(chart, data, event), MAX_WAIT_TIME_OFFER_MS, TimeUnit.MILLISECONDS))
        return true;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    log.error("Event " + String.valueOf(event) + " could not be placed into the event queue!");
    return false;
  }

  @Override
  public synchronized boolean shutdown(long milliseconds) {
    if (isShutdownCompletted) {
      return true;
    }
    log.info("shutdown triggered");
    shutdownTriggered = true;
    // add a null event to the queue to return form the event dispatching
    // thread's take() method in run()
    addEvent(null, null);
    boolean rc = false;
    try {
      rc = shutdownSema.tryAcquire(milliseconds, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    shutdownSema.release();
    return rc;

  }

  @Override
  public boolean isShutdown() {
    return isShutdownCompletted;
  }
}
