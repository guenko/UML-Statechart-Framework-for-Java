package statechart.unittests2;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import statechart.*;
import statechart.trace.*;

public class ConcurrentTest {

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
   * constructs the statechart Co1
   */
  public Statechart buildChartCo1() throws StatechartException {
    Statechart chart = new Statechart("Co1");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    // state a
    State a = new State("A", chart, new TraceAction("A_entry"), new TraceAction("A_do"), new TraceAction("A_exit"));
    // concurrent state
    ConcurrentState b = new ConcurrentState("B", chart, new TraceAction("B_entry"), new TraceAction("B_do"), new TraceAction("B_exit"));
    // region 1
    HierarchicalState r1 = new HierarchicalState("R1", b, null, null, null);
    PseudoState r1_start = new PseudoState("R1_start", r1, PseudoState.pseudostate_start);
    State d = new State("D", r1, new TraceAction("D_entry"), new TraceAction("D_do"), new TraceAction("D_exit"));
    FinalState r1_final = new FinalState("R1_final", r1);
    // region 2
    HierarchicalState r2 = new HierarchicalState("R2", b, null, null, null);
    PseudoState r2_start = new PseudoState("R2_start", r2, PseudoState.pseudostate_start);
    ConcurrentState e = new ConcurrentState("E", r2, new TraceAction("E_entry"), null, new TraceAction("E_exit"));
    FinalState r2_final = new FinalState("R2_final", r2);
    // region e_r1
    HierarchicalState e_r1 = new HierarchicalState("E_R1", e, null, null, null);
    PseudoState e_r1_start = new PseudoState("E_R1_start", e_r1, PseudoState.pseudostate_start);
    State f = new State("F", e_r1, new TraceAction("F_entry"), new TraceAction("F_do"), new TraceAction("F_exit"));
    FinalState e_r1_final = new FinalState("E_R1_final", e_r1);
    // region e_r1
    HierarchicalState e_r2 = new HierarchicalState("E_R2", e, null, null, null);
    PseudoState e_r2_start = new PseudoState("E_R2_start", e_r2, PseudoState.pseudostate_start);
    State g = new State("G", e_r2, new TraceAction("G_entry"), new TraceAction("G_do"), new TraceAction("G_exit"));
    FinalState e_r2_final = new FinalState("E_R2_final", e_r2);
    // region 3
    HierarchicalState r3 = new HierarchicalState("R3", b, null, null, null);
    PseudoState r3_start = new PseudoState("R3_start", r3, PseudoState.pseudostate_start);
    HierarchicalState h = new HierarchicalState("H", r3, new TraceAction("H_entry"), null, new TraceAction("H_exit"));
    PseudoState h_start = new PseudoState("H_start", h, PseudoState.pseudostate_start);
    State i = new State("I", h, new TraceAction("I_entry"), new TraceAction("I_do"), new TraceAction("I_exit"));
    FinalState r3_final = new FinalState("R3_final", r3);
    // state j
    State j = new State("J", chart, new TraceAction("J_entry"), new TraceAction("J_do"), new TraceAction("J_exit"));

    /** transitions */
    new Transition(chart_start, a);
    new Transition(a, b);
    // region 1
    new Transition(r1_start, d);
    new Transition(d, r1_final);
    // region 2
    new Transition(r2_start, e);
    new Transition(e, r2_final);
    // e region 1
    new Transition(e_r1_start, f);
    new Transition(f, e_r1_final);
    // e region 2
    new Transition(e_r2_start, g);
    new Transition(g, e_r2_final);
    // region 3
    new Transition(r3_start, h);
    new Transition(h_start, i);
    new Transition(h, r3_final, new Event1());

    new Transition(b, b, new Event1());
    new Transition(b, j, new Event2());
    new Transition(j, chart_final, new Event3());
    new Transition(b, chart_final, new Event3());

    return chart;
  }

  /**
   * Tests transitions exiting and entering several hierarchical states, test
   * for wrong calling sequence of exit and entry action, check whether action
   * of transition is called in between
   */
  @Test
  public void testConcurrent1() {
    System.out.println("----testConcurrent1----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartCo1();

      Assert.assertNull(chart.getState("x"));
      Assert.assertNull(chart.getState("H1:H2:C:C"));

      State a = chart.getState("A");
      Assert.assertNotNull(a);
      Assert.assertEquals("A", a.toString());

      State d = chart.getState("B:R1:D");
      Assert.assertNotNull(d);
      Assert.assertEquals("D", d.toString());
      // Assert.assertEquals("B:R1:D", d.getPath());

      State f = chart.getState("B:R2:E:E_R1:F");
      Assert.assertNotNull(f);
      Assert.assertEquals("F", f.toString());
      // Assert.assertEquals("B:R2:E:E_R1:F", f.toString());

      /**** START AND RUN ****/
      /** test getStateConfiguration() **/
      MyMetadata data = new MyMetadata();
      Assert.assertEquals(data.getStateConfiguration(), "");

      Assert.assertTrue(data.start(chart));
      Assert.assertEquals(data.getStateConfiguration(), "B(R1_final|R2_final|H:I)");

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertEquals(data.getStateConfiguration(), "B(R1_final|R2_final|R3_final)");

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertEquals(data.getStateConfiguration(), "B(R1_final|R2_final|H:I)");

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertEquals(data.getStateConfiguration(), "B(R1_final|R2_final|R3_final)");

      Assert.assertTrue(data.dispatch(new Event2()));
      Assert.assertEquals(data.getStateConfiguration(), "J");

      Assert.assertTrue(data.dispatch(new Event3()));
      Assert.assertEquals(data.getStateConfiguration(), "final");

      data.shutdown();
      Assert.assertEquals(data.getStateConfiguration(), "");

      String expected =
          ": 'A_entry''A_do'A >'A_exit''B_entry''B_do''D_entry''D_do''E_entry''F_entry''F_do''G_entry''G_do''H_entry'B(D|E(F|G)|H) >'D_exit''F_exit''G_exit''I_entry''I_do'B(R1_final|E(E_R1_final|E_R2_final)|H:I) >'E_exit'B(R1_final|R2_final|H:I)\n";
      expected += "Event1: 'I_exit''H_exit'B(R1_final|R2_final|R3_final)\n";
      expected +=
          "Event1: 'B_exit''B_entry''B_do''D_entry''D_do''E_entry''F_entry''F_do''G_entry''G_do''H_entry'B(D|E(F|G)|H) >'D_exit''F_exit''G_exit''I_entry''I_do'B(R1_final|E(E_R1_final|E_R2_final)|H:I) >'E_exit'B(R1_final|R2_final|H:I)\n";
      expected += "Event1: 'I_exit''H_exit'B(R1_final|R2_final|R3_final)\n";
      expected += "Event2: 'B_exit''J_entry''J_do'J\n";
      expected += "Event3: 'J_exit'final\n";

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
    testConcurrent1();
    shutdownTests();
  }

  public static void main(String[] args) {
    ConcurrentTest test = new ConcurrentTest();
    test.testAll();
  }
}
