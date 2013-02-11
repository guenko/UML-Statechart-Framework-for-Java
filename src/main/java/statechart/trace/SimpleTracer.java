package statechart.trace;

import statechart.Event;
import statechart.Metadata;

/**
 * A TracingAdaptor implementation with simple tracing features, writing to
 * standard out.
 */
public class SimpleTracer implements TracingAdapter {

  /** whether tracing is on */
  boolean traceOn = true;
  /** to control intersection before follow up state changes */
  private boolean segmentStarted = false;

  /**
   * Creates a simple tracer.
   */
  public SimpleTracer() {
  }

  /**
   * {@inheritDoc}
   */
  public void setTraceOn(boolean traceOn) {
    this.traceOn = traceOn;
  }

  /**
   * {@inheritDoc}
   */
  public boolean getTraceOn() {
    return traceOn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void eventDispatched(Event event) {
    if (traceOn) {
      writeTrace((event == null ? "" : String.valueOf(event)) + ": ");
      segmentStarted = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stateChanged(Metadata data) {
    if (traceOn) {
      if (!segmentStarted) {
        // intersection marker before a follow up state change is traced
        writeTrace(" >");
      }
      writeTrace(data.getStateConfiguration());
      segmentStarted = false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void traceEventFinished() {
    if (traceOn) {
      writeTrace("\n");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void traceOut(String str) {
    if (traceOn) {
      if (!segmentStarted) {
        writeTrace(" >");
        segmentStarted = true;
      }
      writeTrace("'" + str + "'");
    }
  }

  /**
   * The string part to be traced is handed over to this method, so derived
   * classes may change the default behavior, e.g. print out to other
   * destination or collect it into a buffer. The method is only called when
   * tracing is on.
   * 
   * @see #setTraceOn()
   * @param tracePart a string part to be traced. When an event is fully
   *          dispatched (means we see no further state changes for the event
   *          last triggered) an "\n" string is handed over.
   */
  protected void writeTrace(String tracePart) {
    // this tracer simply prints the pieces to standard out
    System.out.print(tracePart);
  }
}
