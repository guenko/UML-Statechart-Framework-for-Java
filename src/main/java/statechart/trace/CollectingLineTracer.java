package statechart.trace;

/**
 * A TracingAdaptor implementation with more sophisticated tracing capabilities:
 * prints traces per line to standard out (may be switched off) and collects
 * trace strings of all tracer objects. The collected string may be fetched for
 * test result comparison.
 */
public class CollectingLineTracer extends SimpleTracer {

  static final int LINE_TRACE_CAPACITY = 100;
  static final int COLLECTED_TRACE_CAPACITY = 1000;

  /** buffer for overall trace string collection */
  private static StringBuilder collectedTrace;
  /** the buffer for line string collection */
  private StringBuilder lineTrace;
  /** labels this tracer in order to distinguish it from others */
  private String name;

  /** whether each line is to be printed to system out */
  private boolean printLine;

  /**
   * standard constructor with tracing switched off
   */
  public CollectingLineTracer() {
    this(null);
  }

  /**
   * simple constructor with tracing switched on
   * 
   * @param name identification string of this tracer object in order to
   *          distinguish it from others
   */
  public CollectingLineTracer(String name) {
    this(name, true);
    initCollectedTrace();
  }

  /**
   * detailed constructor with tracing switched on
   * 
   * @param name labels this tracer object in order to distinguish it from
   *          others
   * @param printLine whether each line is to be printed to system out
   */
  public CollectingLineTracer(String name, boolean printLine) {
    super.setTraceOn(true);
    this.name = name;
    this.printLine = printLine;
    initLineTrace();
    initCollectedTrace();
  }

  /** whether each line is to be printed to system out */
  public void setPrintLine(boolean printLine) {
    this.printLine = printLine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void writeTrace(String str) {
    if (lineTrace.length() == 0) {
      if (name != null) {
        lineTrace.append("[" + name + "] ");
      }
    }
    lineTrace.append(str);
    if (str.charAt(str.length() - 1) == '\n') {
      if (printLine) {
        System.out.print(lineTrace);
      }
      appendCollectedTrace(lineTrace);
      initLineTrace();
    }
  }

  private void initLineTrace() {
    lineTrace = new StringBuilder(LINE_TRACE_CAPACITY);
  }

  synchronized private static void initCollectedTrace() {
    collectedTrace = new StringBuilder(COLLECTED_TRACE_CAPACITY);
  }

  synchronized private static void appendCollectedTrace(StringBuilder str) {
    collectedTrace.append(str);
  }

  /**
   * @return the overall trace string collected so far. It has collected all
   *         trace string from all living Metadata objects since the last tracer
   *         object was instantiated or since last call of this method.
   */
  synchronized public static String fetchCollectedTrace() {
    String ret = collectedTrace.toString();
    initCollectedTrace();
    return ret;
  }
}
