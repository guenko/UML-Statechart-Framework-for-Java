package statechart.trace;

import statechart.Event;
import statechart.Metadata;

/**
 * Adapter interface for tracing feature.
 */
public interface TracingAdapter {
  /**
   * Switch the tracing feature on or off dynamically.
   * 
   * @param traceOn Whether tracing shall be activated
   */
  public void setTraceOn(boolean traceOn);

  /**
   * Whether the tracing feature is activated.
   * 
   * @return whether tracing is active
   */
  public boolean getTraceOn();

  /**
   * An event has triggered the statechart and is to be dispatched
   * 
   * @param event the triggering event
   */
  public void eventDispatched(Event event);

  /**
   * A state change occurred, but follow up state changes caused by completion
   * transitions may happen.
   * 
   * @param data
   */
  public void stateChanged(Metadata data);

  /**
   * Regarding the event given by traceEventTriggered all state changes have
   * happened now
   */
  public void traceEventFinished();

  /**
   * Tracing mechanism for users of Statechart, e.g. from action or guard
   * implementation. The string is traced by means of the tracing feature and it
   * is subject to the same principles (e.g. is only traced if tracing is
   * active).
   * 
   * @see #getTraceOn()
   * @param str string to be traced
   */
  public void traceOut(String str);
}
