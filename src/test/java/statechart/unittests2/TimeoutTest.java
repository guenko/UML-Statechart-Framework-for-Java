package statechart.unittests2;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import statechart.*;
import statechart.trace.*;

public class TimeoutTest {

  /**
   * Our Tracing Metadata
   */
  public static class TestMetadata extends Metadata {

    public TestMetadata() {
      this(null);
    }

    TestMetadata(String name) {
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

  /**
   * Actions
   */
  public static class Action1 implements Action {

    private static Logger log = Logger.getLogger(Action1.class);
    private Action2 action2;
    private long sleepTime;

    public Action1(Action2 action2, long sleepTime) {
      this.action2 = action2;
      this.sleepTime = sleepTime;
    }

    public void execute(Metadata data) {
      String expectedState1 = data.getStateConfiguration();
      log.debug("Action1 expectedState1: " + expectedState1);

      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      data.dispatchAsynchron(new Event2());
      String expectedState2 = data.getStateConfiguration();
      log.debug("Action1 expectedState2: " + expectedState2);

      data.getTracer().traceOut("Action1 in state: " + expectedState2);
      // no state change during action had to be performed, even a sleep was
      // done
      Assert.assertEquals(expectedState1, expectedState2);
      action2.setExpectedState(expectedState2);
    }
  }

  public static class Action2 implements Action {

    private static Logger log = Logger.getLogger(Action2.class);
    private String expectedState;
    public volatile boolean unexpected = false;

    public void setExpectedState(String expectedState) {
      this.expectedState = expectedState;
    }

    public void execute(Metadata data) {
      String actualState = data.getStateConfiguration();
      data.getTracer().traceOut("Action2 in state: " + actualState);
      log.debug("Action2 actualState: " + actualState);

      // check if action2 was performed at the same state as action1, means
      // the timeout transition did not take place (was disabled) between
      // action1 and action2
      if (!expectedState.equals(data.getStateConfiguration())) {
        data.getTracer().traceOut("actualState != expectedState !!!!!!!");
        unexpected = true;
      }
      expectedState = "";
    }
  }

  /** timeout value in ms, at min. 100ms */
  private static final int TIME_OUT = 200;
  /**
   * additional time we wait for the timeout to occur in ms, at min. 50ms and
   * less than TIME_OUT_VALUE_MS
   */
  private static final int TIME_OUT_OFFSET = 100;

  private Action2 to1Action2 = null;

  /**
   * Build statechart To1_2
   */
  public Statechart buildChartTo1(long timeoutValue, long action1SleepTime) throws StatechartException {

    Statechart chart = new Statechart("To1");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);

    HierarchicalState h1 = new HierarchicalState("H1", chart, null, null, null);
    State h1_a = new State("H1_A", h1, null, null, null);
    State h1_b = new State("H1_B", h1, null, null, null);
    State s1 = new State("S1", chart, null, null, null);

    HierarchicalState h2 = new HierarchicalState("H2", chart, null, null, null);
    State h2_a = new State("H2_A", h2, null, null, null);
    State h2_b = new State("H2_B", h2, null, null, null);
    State s2 = new State("S2", chart, null, null, null);

    /** transitions */
    new Transition(chart_start, h1_a);
    Action2 action2 = new Action2();
    Action1 action1 = new Action1(action2, action1SleepTime);
    to1Action2 = action2;

    // one side
    new Transition(h1_a, h1_b, new Event1(), action1);
    new Transition(h1_b, s1);
    new Transition(s1, h1_a);
    new Transition(h1_a, h1_a, new Event2(), action2);
    new Transition(h1, h2_a, new TimeoutEvent(timeoutValue), new TraceAction("Timeout occured"));
    // same for the other side
    new Transition(h2_a, h2_b, new Event1(), action1);
    new Transition(h2_b, s2);
    new Transition(s2, h2_a);
    new Transition(h2_a, h2_a, new Event2(), action2);
    new Transition(h2, h1_a, new TimeoutEvent(timeoutValue), new TraceAction("Timeout occured"));

    return chart;
  }

  /**
   * Tests whether a TimeoutEvent is canceled (disabled) in the right way when
   * the state is left. Triggered by event1 performs action1 a sleep during this
   * the timeout occurred and then action2 adds event2 and then the state which
   * triggered the timeoutEvent is left. This state leaving has to disable the
   * Timeout occurrence so the transition is never triggered and action2 is
   * performed at the same state configuration as action1.
   */
  @Test
  public void testTimeout1_1() {
    System.out.println("----testTimeout1_1----");

    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartTo1(TIME_OUT, 2 * TIME_OUT_OFFSET);

      /**** START AND RUN ****/
      TestMetadata data = new TestMetadata();
      chart.startAsynchron(data);

      int loops = 5;
      for (int i = 0; i < loops; i++) {
        Thread.sleep(TIME_OUT - TIME_OUT_OFFSET);
        chart.dispatchAsynchron(data, new Event1());
        Thread.sleep(2 * TIME_OUT_OFFSET);
      }
      Thread.sleep(TIME_OUT_OFFSET);

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();

      // check if action2 was performed at the same state as action1
      Assert.assertFalse(to1Action2.unexpected);

      // check trace
      String expected = ": H1:H1_A\n";
      for (int i = 0; i < loops; i++) {
        expected += "Event1: 'Action1 in state: H1'H1:H1_B >S1 >H1:H1_A\n";
        expected += "TimeoutOccurrenceEvent: \n";
        expected += "Event2: 'Action2 in state: H1'H1:H1_A\n";
      }
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (StatechartException e) {
      e.printStackTrace();
    }
  }

  /**
   * Tries to trigger Event1 the same time the timeout occurs, so it tries to
   * provoke a raise condition between these two events
   */
  @Test
  public void testTimeout1_2() {
    System.out.println("----testTimeout1_2----");

    long timeOutValue = 50;
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartTo1(timeOutValue, 0);

      /**** START AND RUN ****/
      TestMetadata data = new TestMetadata();
      chart.startAsynchron(data);

      int loops = 50;
      for (int i = 0; i < loops; i++) {
        Thread.sleep(timeOutValue);
        chart.dispatchAsynchron(data, new Event1());
      }
      Thread.sleep(timeOutValue);

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();

      // check if action2 was performed at the same state as action1
      Assert.assertFalse(to1Action2.unexpected);

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (StatechartException e) {
      e.printStackTrace();
    }
  }

  /**
   * Test if the right transition triggers when more than one Timeout
   * transitions exist Check also exception handling for checks and restrictions
   * with TimeoutEvents
   */
  @Test
  public void testTimeout2() {
    System.out.println("----testTimeout2----");

    try {
      /**** BUILD STATECHART ****/
      Statechart chart = new Statechart("To2");

      /** states */
      PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
      HierarchicalState h1 = new HierarchicalState("H1", chart, null, null, null);
      HierarchicalState h2 = new HierarchicalState("H2", h1, null, null, null);
      State a = new State("A", h2, null, null, null);
      State b = new State("B", chart, null, null, null);

      /** transitions */
      new Transition(chart_start, a);
      new Transition(h1, b, new TimeoutEvent(TIME_OUT + TIME_OUT_OFFSET), new TraceAction("h1_to_b"));
      new Transition(h2, b, new TimeoutEvent(TIME_OUT), new TraceAction("h2_to_b"));
      new Transition(a, b, new TimeoutEvent(TIME_OUT + TIME_OUT_OFFSET), new TraceAction("a_to_b"));
      new Transition(b, a, new TraceAction("b_to_a"));

      /**** START AND RUN ****/
      TestMetadata myData = new TestMetadata();
      chart.startAsynchron(myData);

      // After start transition Timeouts are triggered by itself
      int loops = 2;
      Thread.sleep(loops * TIME_OUT + TIME_OUT_OFFSET);

      /**** SHUTDOWN AND VERIFY ****/
      myData.shutdown();

      // check trace
      String expected = ": H1:H2:A\n";
      for (int i = 0; i < loops; i++) {
        expected += "TimeoutOccurrenceEvent: 'h2_to_b'B >'b_to_a'H1:H2:A\n";
        expected += "(TimeoutOccurrenceEvent: \n)?";
      }
      String actual = CollectingLineTracer.fetchCollectedTrace();
      boolean result = actual.matches(expected);
      if (!result) {
        System.out.print(expected);
      }
      Assert.assertTrue(result);

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (StatechartException e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void shutdownTests() {
    Statechart.shutdown();
  }

  public void testAll() {
    testTimeout1_1();
    testTimeout1_2();
    testTimeout2();
    shutdownTests();
  }

  public static void main(String[] args) {
    TimeoutTest test = new TimeoutTest();
    test.testAll();
  }
}
