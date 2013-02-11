package statechart.timeout;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import statechart.Metadata;
import statechart.State;
import statechart.StatechartThreadFactory;
import statechart.TimeoutEvent;
import statechart.TimeoutOccurrenceEvent;
import statechart.timeout.TimerHandle;

/**
 * This TimerManager implementation used a ScheduledExecutorService to start and
 * cancel timers and to raise an timeout occurrence event when the timer is
 * executed
 * 
 * @author Administrator
 */
public class ScheduledTimerManager implements TimerManager {

  private static Logger log = Logger.getLogger(ScheduledTimerManager.class);
  /** name of the thread handling the timers for execution */
  private static final String THREAD_GROUP_NAME = "timer";

  /**
   * Holds the data to be used when the timer is elapsed. This is also a timer
   * handle and a timeout occurrence event
   */
  class TimerData extends TimeoutOccurrenceEvent implements Runnable {

    private Metadata data;
    // returned by the scheduler used for timer cancellation
    private ScheduledFuture<?> future = null;

    public TimerData(State startingState, Metadata data) {
      super(startingState);
      this.data = data;
    }

    public void setFuture(ScheduledFuture<?> future) {
      this.future = future;
    }

    /**
     * Is executed when the timeout duration has elapsed
     */
    @Override
    public void run() {
      // add a timeout occurrence event to the event queue
      data.dispatchAsynchron(this);
    }
  }

  /**
   * the timer scheduler used to schedule timers and to execute it within a
   * single thread
   */
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new StatechartThreadFactory(THREAD_GROUP_NAME));

  /**
   * Initiate the timer manager
   * 
   * @param eventQueue the event queue the raised timeout occurrence events are
   *          added to
   */
  public ScheduledTimerManager() {
    log.info("started");
  }

  @Override
  public TimerHandle startTimer(State startingState, Metadata data, TimeoutEvent event) {
    // create a new timer data object holding the data necessary for timer
    // execution
    TimerData timerData = new TimerData(startingState, data);
    try {
      // schedule the timer for execution after the given timeout value
      ScheduledFuture<?> future = scheduler.schedule(timerData, event.getTimout(), TimeUnit.MILLISECONDS);
      timerData.setFuture(future);
    } catch (RejectedExecutionException e) {
      // in case we are just in shutdown
      return timerData;
    }
    log.trace("start timer: " + Integer.toHexString(timerData.hashCode()) + " at state: " + startingState.toString());
    // return the timer data as handle to the client so it is able
    // to cancel this scheduled timer before execution
    return timerData;
  }

  @Override
  public void cancelTimer(statechart.timeout.TimerHandle timerHandle) {
    assert (timerHandle != null);
    // future may be null during shutdown
    if (((TimerData)timerHandle).future != null) {
      // cancel execution of this scheduled timer but not if it has already
      // started the run() method
      boolean rc = ((TimerData)timerHandle).future.cancel(false);
      log.trace("cancel timer: " + Integer.toHexString(timerHandle.hashCode()) + " rc: " + rc);
    }
  }

  @Override
  public boolean shutdown(long waitMilliseconds) {
    log.info("shutdown triggered");
    scheduler.shutdown();
    boolean rc = false;
    try {
      rc = scheduler.awaitTermination(waitMilliseconds, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    log.info("shutdown completed: " + rc);
    return rc;
  }
}
