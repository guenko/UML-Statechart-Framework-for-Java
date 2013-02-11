package statechart.trace;

import statechart.Action;
import statechart.Metadata;

/**
 * Convenience action useful for tracing
 */
public class TraceAction implements Action {
  private String traceString;

  /**
   * @param string to trace out
   */
  public TraceAction(String value) {
    this.traceString = value;
  }

  public void execute(Metadata data) {
    if (data.getTracer() != null) {
      data.getTracer().traceOut(traceString);
    }
  }
}
