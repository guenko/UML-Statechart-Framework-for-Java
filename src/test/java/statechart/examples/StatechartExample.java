package statechart.examples;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import statechart.*;
import statechart.trace.*;

public class StatechartExample {

  /**
   * Our Metadata with tracing capability
   */
  public static class MyMetadata extends Metadata {
    public int counterValue = 0;

    public MyMetadata() {
      this(null);
    }

    /**
     * @param name tracer identification string
     */
    public MyMetadata(String name) {
      super(new CollectingLineTracer(name, true));
    }
  }

  /**
   * Events
   */
  public static class AnEvent extends Event {
    public AnEvent() {
      super("anEvent");
    }
  }

  public static class AnotherEvent extends Event {
    public AnotherEvent() {
      super("anotherEvent");
    }
  }

  /**
   * Actions
   */
  public class SetValue implements Action {
    private int value;

    public SetValue(int value) {
      this.value = value;
    }

    public void execute(Metadata data) {
      ((MyMetadata)data).counterValue = value;
    }
  }

  public class DecrementValue implements Action {
    public void execute(Metadata data) {
      ((MyMetadata)data).counterValue--;
    }
  }

  public class GreaterThan implements Guard {
    private int comparisionValue;

    public GreaterThan(int comparisionValue) {
      this.comparisionValue = comparisionValue;
    }

    public boolean check(Metadata data) {
      return ((MyMetadata)data).counterValue > comparisionValue;
    }
  }

  /** initial counter value */
  private final int COUNTER_VALUE = 5000;
  /** timeout value in ms, at min. 50ms */
  private static final int TIME_OUT_VALUE_MS = 50;
  /** additional time we wait for the timeout to occur in ms, at min. 50ms */
  private static final int TIME_OUT_WAIT_OFFSET_MS = 50;

  /**
   * constructs the statechart
   * 
   * @param counterInitValue initial value of the counter
   */
  public Statechart buildCartExample(int counterInitValue) throws StatechartException {
    Statechart chart = new Statechart("Example");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    HierarchicalState a = new HierarchicalState("A", chart, new SetValue(counterInitValue), null, null);
    State b = new State("B", a, null, null, null);
    PseudoState junct = new PseudoState("junction", a, PseudoState.pseudostate_junction);
    FinalState a_final = new FinalState("A_final", a);

    ConcurrentState c = new ConcurrentState("C", a, null, null, null);
    // region 1 of the and-state
    HierarchicalState c_r1 = new HierarchicalState("C_R1", c, null, null, null);
    PseudoState c_r1_start = new PseudoState("C_R1_start", c_r1, PseudoState.pseudostate_start);
    State d = new State("D", c_r1, new TraceAction("Concurrent state activated"), null, new TraceAction("Concurrent state deactivated"));

    // region 2 of the and-state
    HierarchicalState c_r2 = new HierarchicalState("C_R2", c, null, null, null);
    PseudoState c_r2_start = new PseudoState("C_R2_start", c_r2, PseudoState.pseudostate_start);
    State e = new State("E", c_r2, new TraceAction("start timeout"), null, null);
    State f = new State("F", c_r2, new DecrementValue(), null, null);

    /** transitions */
    new Transition(chart_start, b);
    new Transition(b, c);
    new Transition(c_r1_start, d);
    new Transition(c_r2_start, e);
    new Transition(e, f, new TimeoutEvent(TIME_OUT_VALUE_MS), new TraceAction("Timeout"));
    new Transition(e, f, new AnEvent());
    new Transition(f, e, new AnotherEvent());
    new Transition(c, junct, new AnEvent());
    new Transition(junct, b, new GreaterThan(0));
    new Transition(junct, a_final);
    new Transition(a, chart_final);

    return chart;
  }

  @Test
  public void exampleRun1() {
    System.out.println("----exampleRun1----");
    try {
      Statechart chart = buildCartExample(2 * COUNTER_VALUE + 1);
      MyMetadata data = new MyMetadata();
      data.getTracer().setTraceOn(false);

      for (int i = 0; i < 2; i++) {
        long time0 = System.currentTimeMillis();
        System.out.println("<Start>");

        Assert.assertTrue(data.start(chart));
        for (int j = 0; j < COUNTER_VALUE; j++) {
          Assert.assertTrue(data.dispatch(new AnEvent()));
          Assert.assertTrue(data.dispatch(new AnEvent()));
        }
        long time1 = System.currentTimeMillis();
        for (int j = 0; j < COUNTER_VALUE; j++) {
          Assert.assertTrue(data.dispatch(new AnEvent()));
          Assert.assertTrue(data.dispatch(new AnEvent()));
        }
        long time2 = System.currentTimeMillis();
        // waiting for timeout to occur in state E
        Thread.sleep(TIME_OUT_VALUE_MS + TIME_OUT_WAIT_OFFSET_MS);
        // still is not in charts final state
        Assert.assertFalse(data.getData(chart).currentState instanceof FinalState);
        // this event dispatch leads to charts final state
        Assert.assertTrue(data.dispatch(new AnEvent()));
        Assert.assertTrue(data.getData(chart).currentState instanceof FinalState);
        // no events are handled furthermore
        Assert.assertFalse(data.dispatch(new AnEvent()));

        System.out.println(data.getData(chart).currentState instanceof FinalState ? "FinalState reached" : "FinalState NOT reached !!!");
        System.out.println("first   " + 2 * COUNTER_VALUE + " event invocations: " + (time1 - time0) + "ms");
        System.out.println("further " + 2 * COUNTER_VALUE + " event invocations: " + (time2 - time1) + "ms");
      }
      data.shutdown();

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (StatechartException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void exampleRun2() {
    System.out.println("----exampleRun2----");
    try {
      Statechart chart = buildCartExample(3);
      MyMetadata data = new MyMetadata("Run2");

      data.start(chart);
      data.dispatch(new AnEvent());
      data.dispatch(new AnEvent());
      Thread.sleep(TIME_OUT_VALUE_MS + TIME_OUT_WAIT_OFFSET_MS);
      data.dispatch(new AnEvent());
      data.dispatch(new AnEvent());
      data.dispatch(new AnEvent());

      String expected = "[Run2] : A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expected += "[Run2] anEvent: A:C(D|F)\n";
      expected += "[Run2] anEvent: 'Concurrent state deactivated'A:junction >A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expected += "[Run2] TimeoutOccurrenceEvent: 'Timeout'A:C(D|F)\n";
      expected += "[Run2] anEvent: 'Concurrent state deactivated'A:junction >A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expected += "[Run2] anEvent: A:C(D|F)\n";
      expected += "[Run2] anEvent: 'Concurrent state deactivated'A:junction >A:A_final >final\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

      data.shutdown();

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (StatechartException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void exampleRun3() {
    System.out.println("----exampleRun3----");
    try {
      Statechart chart = buildCartExample(3);
      MyMetadata data1 = new MyMetadata("Run3.1");
      MyMetadata data2 = new MyMetadata("Run3.2");

      data1.startAsynchron(chart);
      data1.dispatchAsynchron(new AnEvent());
      data2.startAsynchron(chart);
      data1.dispatchAsynchron(new AnEvent());
      data2.dispatchAsynchron(new AnEvent());
      data2.dispatchAsynchron(new AnEvent());
      Thread.sleep(TIME_OUT_VALUE_MS + TIME_OUT_WAIT_OFFSET_MS);
      data2.dispatchAsynchron(new AnotherEvent());
      data1.dispatchAsynchron(new AnEvent());
      data1.dispatchAsynchron(new AnEvent());
      data2.dispatchAsynchron(new AnEvent());
      data2.dispatchAsynchron(new AnEvent());
      data1.dispatchAsynchron(new AnEvent());
      // just wait for statechart completion
      Thread.sleep(TIME_OUT_WAIT_OFFSET_MS);
      // data objects are called to shutdown at Statechart.shutdown()
      // data1.shutdown();
      // data2.shutdown();

      String expectedRun1 = "[Run3.1] : A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expectedRun1 += "[Run3.1] anEvent: A:C(D|F)\n";
      expectedRun1 += "[Run3.1] anEvent: 'Concurrent state deactivated'A:junction >A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expectedRun1 += "[Run3.1] TimeoutOccurrenceEvent: 'Timeout'A:C(D|F)\n";
      expectedRun1 += "[Run3.1] anEvent: 'Concurrent state deactivated'A:junction >A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expectedRun1 += "[Run3.1] anEvent: A:C(D|F)\n";
      expectedRun1 += "[Run3.1] anEvent: 'Concurrent state deactivated'A:junction >A:A_final >final\n";

      String expectedRun2 = "[Run3.2] : A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expectedRun2 += "[Run3.2] anEvent: A:C(D|F)\n";
      expectedRun2 += "[Run3.2] anEvent: 'Concurrent state deactivated'A:junction >A:B >'Concurrent state activated''start timeout'A:C(D|E)\n";
      expectedRun2 += "[Run3.2] TimeoutOccurrenceEvent: 'Timeout'A:C(D|F)\n";
      expectedRun2 += "[Run3.2] anotherEvent: 'start timeout'A:C(D|E)\n";
      expectedRun2 += "[Run3.2] anEvent: A:C(D|F)\n";
      expectedRun2 += "[Run3.2] anEvent: 'Concurrent state deactivated'A:junction >A:A_final >final\n";

      String actual = CollectingLineTracer.fetchCollectedTrace();
      String lines[] = actual.split("\n");
      String actualRun1 = new String();
      String actualRun2 = new String();
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].startsWith("[Run3.1]")) {
          actualRun1 += lines[i] + '\n';
        }
        if (lines[i].startsWith("[Run3.2]")) {
          actualRun2 += lines[i] + '\n';
        }
      }
      Assert.assertEquals(expectedRun1, actualRun1);
      Assert.assertEquals(expectedRun2, actualRun2);

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

  public void exampleRunAll() {
    exampleRun1();
    exampleRun2();
    shutdownTests();
  }

  public static void main(String[] args) {
    StatechartExample example = new StatechartExample();
    example.exampleRunAll();
  }
}
