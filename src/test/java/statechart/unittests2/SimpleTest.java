package statechart.unittests2;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import statechart.*;
import statechart.trace.*;

public class SimpleTest {

  /**
   * Our Tracing Metadata
   */
  public static class MyMetadata extends Metadata {

    public MyMetadata() {
      this(null);
    }

    MyMetadata(String name) {
      super(new CollectingLineTracer(name));
    }
  }

  /**
   * Events
   */
  public static class Event1 extends Event {
    public Event1() {
      super("Event1");
    }
  }

  public static class Event2 extends Event {
    public Event2() {
      super("Event2");
    }
  }

  public static class Event3 extends Event {
    public Event3() {
      super("Event3");
    }
  }

  /**
   * constructs the statechart Si1
   */
  public Statechart buildChartSi1() throws StatechartException {
    Statechart chart = new Statechart("Si1");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    State a = new State("A", chart, new TraceAction("A_entry"), new TraceAction("A_do"), new TraceAction("A_exit"));
    State b = new State("B", chart, new TraceAction("B_entry"), new TraceAction("B_do"), new TraceAction("B_exit"));

    /** transitions */
    new Transition(chart_start, a);
    new Transition(a, b, new Event1());
    new Transition(b, chart_final, new Event1());
    return chart;
  }

  @Test
  public void testSimple1() {
    System.out.println("----testSimple1----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartSi1();

      /**** START AND RUN ****/
      MyMetadata data = new MyMetadata();
      Assert.assertTrue(data.start(chart));

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertTrue(data.dispatch(new Event1()));

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      String expected = ": 'A_entry''A_do'A\n";
      expected += "Event1: 'A_exit''B_entry''B_do'B\n";
      expected += "Event1: 'B_exit'final\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void shutdownTests() {
    Statechart.shutdown();
  }

  public void testAll() {
    testSimple1();
    shutdownTests();
  }

  public static void main(String[] args) {
    SimpleTest test = new SimpleTest();
    test.testAll();
  }
}
