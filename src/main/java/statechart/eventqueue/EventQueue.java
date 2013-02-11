package statechart.eventqueue;

import statechart.Event;
import statechart.Metadata;

/**
 * Provides functionality for queuing of events in the statechart framework
 */
public interface EventQueue {

  /**
   * Adds an event to the queue
   * 
   * @param data
   * @param event the event to queue
   * @return whether the event was successfully queued
   */
  public boolean addEvent(Metadata data, Event event);

  /**
   * Shutdown the event queue
   * 
   * @param waitMilliseconds maximum time to wait for shutdown
   * @return whether the event queue is down
   */
  public boolean shutdown(long waitMilliseconds);

  /**
   * @return whether the event queue is down
   */
  public boolean isShutdown();
}
