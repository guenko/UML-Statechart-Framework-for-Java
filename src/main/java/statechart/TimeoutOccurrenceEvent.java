package statechart;

import statechart.timeout.TimerHandle;

/**
 * This event indicates the raise of a timeout then the timeout duration is
 * elapsed. This event is handled by the statechart framework and it acts also
 * for the timer handle used to match the timeout occurrence to the timer
 * started.
 */
public class TimeoutOccurrenceEvent extends Event implements TimerHandle {

  /**
   * the state which started the Timeout, means where the TimeoutEvent
   * transition starts
   */
  protected State startingState;

  public TimeoutOccurrenceEvent(State startingState) {
    // the event name for the statechart framework. Need to have the same name
    // as the TimeoutEvent
    // in order to match with the TimeoutEvent object located at the transition
    super(TimeoutEvent.TIMEOUT_EVENT_NAME);
    this.startingState = startingState;
  }

  public State getStartingState() {
    return startingState;
  }

  @Override
  public String toString() {
    // used for tracing purposes only. The event indicates the occurrence of an
    // timeout and is not the static timeout transition event
    return "TimeoutOccurrenceEvent";
  }
}
