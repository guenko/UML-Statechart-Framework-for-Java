package statechart.timeout;

import statechart.Metadata;
import statechart.State;
import statechart.TimeoutEvent;

/**
 * Handles timeouts within the statechart. It has to start and cancel timers and
 * when the timer elapse it has to add an TimeoutOccurence event to the event
 * queue.
 */
public interface TimerManager {

  /**
   * Start a timer
   * 
   * @param startingState current state the timeout is started for
   * @param data
   * @param event
   * @return a handle of the started timer in order to cancel the timer
   */
  public TimerHandle startTimer(State startingState, Metadata data, TimeoutEvent event);

  /**
   * Cancel a running timer
   * 
   * @param handle timer handle to cancel
   */
  public void cancelTimer(TimerHandle handle);

  /**
   * shutdown timer manager object
   * 
   * @param waitMillisecondes maximum time to wait for shutdown
   * @return whether the timer manager is down
   */
  public boolean shutdown(long waitMillisecondes);
}
