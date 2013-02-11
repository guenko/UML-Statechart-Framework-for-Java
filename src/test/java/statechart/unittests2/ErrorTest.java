package statechart.unittests2;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import statechart.*;
import statechart.trace.*;

public class ErrorTest {

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

  /** timeout value in ms, at min. 100ms */
  private static final int TIME_OUT = 200;
  /**
   * additional time we wait for the timeout to occur in ms, at min. 50ms and
   * less than TIME_OUT_VALUE_MS
   */
  private static final int TIME_OUT_OFFSET = 100;

  /**
   * Test if the right transition triggers when more than one Timeout
   * transitions exist Check also exception handling for checks and restrictions
   * with TimeoutEvents
   */
  @Test
  public void testError1() {
    System.out.println("----testError1----");

    try {
      /**** BUILD STATECHART ****/
      Statechart chart = new Statechart("Er1");

      /** states */
      PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
      HierarchicalState h1 = new HierarchicalState("H1", chart, null, null, null);
      PseudoState h1_start = new PseudoState("start", h1, PseudoState.pseudostate_start);
      @SuppressWarnings("unused")
      PseudoState h1_history = new PseudoState("history", h1, PseudoState.pseudostate_history);
      State a = new State("A", h1, null, null, null);
      State b = new State("B", chart, null, null, null);

      /** parent */
      try {
        b = new State("Bx", null, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.PARENT_NULL, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter parent cannot be null"));
      }
      try {
        b = new PseudoState("Bx", chart, PseudoState.pseudostate_history);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.PARENT_NOT_A_HIERACHICAL_STATE, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parent is not a hierarchical state"));
      }
      try {
        b = new PseudoState("startX", h1, PseudoState.pseudostate_start);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.PARENT_HAS_ALREADY_START_STATE, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parent has already a start state"));
      }
      try {
        h1_history = new PseudoState("historyX", h1, PseudoState.pseudostate_history);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.PARENT_HAS_ALREADY_HISTORY_STATE, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parent has already a history state"));
      }

      /** state name */
      try {
        b = new State(null, chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_INVALID, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name cannot be null or the empty string"));
      }
      try {
        b = new State("", chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_INVALID, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name cannot be null or the empty string"));
      }
      try {
        b = new State("B", chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_NOT_UNIQUE, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name already used. Please define a unique state name"));
      }
      try {
        b = new State("B:", chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_INVALID_CHARACTER, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name contains an invalid character"));
      }
      try {
        b = new State("(ABC", chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_INVALID_CHARACTER, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name contains an invalid character"));
      }
      try {
        b = new State("A|BC", chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_INVALID_CHARACTER, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name contains an invalid character"));
      }
      try {
        b = new State("ABC)", chart, null, null, null);
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.NAME_INVALID_CHARACTER, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Parameter name contains an invalid character"));
      }

      /** Timeout events */
      new Transition(chart_start, h1);
      new Transition(h1_start, a);
      new Transition(h1, a, new TimeoutEvent(TIME_OUT), new TraceAction("b_to_a"));
      try {
        new Transition(b, a, new TimeoutEvent(0), new TraceAction("never seen"));
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.TIMEOUT_NOT_GREATER_NULL, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Negative timeout value or 0 is not allowed"));
      }
      try {
        new Transition(h1, a, new TimeoutEvent(20), new TraceAction("never seen"));
        Assert.fail("StatechartException is expected");
      } catch (StatechartException e) {
        Assert.assertEquals(StatechartException.MORE_THAN_ONE_TIMEOUT_TRANSITIONS, e.getErrorCode());
        Assert.assertTrue(e.getMessage().startsWith("Only one Timeout transistion per state "));
      }

      /**** START AND RUN ****/
      TestMetadata myData = new TestMetadata();
      chart.startAsynchron(myData);

      // After start transition Timeouts are triggered by itself
      Thread.sleep(TIME_OUT + TIME_OUT_OFFSET);

      /**** SHUTDOWN AND VERIFY ****/
      myData.shutdown();

      // check trace
      String expected = ": H1 >H1:A\n";
      expected += "TimeoutOccurrenceEvent: 'b_to_a'H1:A\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

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
    testError1();
    shutdownTests();
  }

  public static void main(String[] args) {
    ErrorTest test = new ErrorTest();
    test.testAll();
  }
}
