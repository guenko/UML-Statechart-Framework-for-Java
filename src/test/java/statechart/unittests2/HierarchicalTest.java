package statechart.unittests2;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import statechart.*;
import statechart.trace.*;

public class HierarchicalTest {

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
   * constructs the statechart Hi1
   */
  public Statechart buildChartHi1() throws StatechartException {
    Statechart chart = new Statechart("Hi1");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    HierarchicalState h1 = new HierarchicalState("H1", chart, null, null, null);
    PseudoState h1_start = new PseudoState("H1_start", h1, PseudoState.pseudostate_start);
    FinalState h1_final = new FinalState("H1_final", h1);
    HierarchicalState h2 = new HierarchicalState("H2", h1, null, null, null);
    PseudoState h2_start = new PseudoState("H2_start", h2, PseudoState.pseudostate_start);
    FinalState h2_final = new FinalState("H2_final", h2);
    State a = new State("A", h2, null, null, null);
    State b = new State("B", h2, null, null, null);
    State c = new State("C", h2, null, null, null);

    /** transitions */
    new Transition(chart_start, h1);
    new Transition(h1, h1, new TraceAction("H1_H1"));
    new Transition(h1, h1, new Event1(), new TraceAction("Event1_outer"));
    new Transition(h1, chart_final, new Event2(), new TraceAction("Event2_outer"));
    // in H1:
    new Transition(h1_start, h2, new TraceAction("H1_start"));
    new Transition(h2, h1_final, new TraceAction("H1_final"));
    // in H2:
    new Transition(h2_start, a, new TraceAction("H2_start"));
    new Transition(a, b, new Event1(), new TraceAction("Event1_inner"));
    new Transition(b, c, new Event2(), new TraceAction("Event2_inner"));
    new Transition(c, h2_final, new TraceAction("H2_final"));

    return chart;
  }

  /**
   * Test hierarchical states with start and final transition being end
   * transition (without event), test self transition on hierarchical with and
   * without event, test state leaving and state entering behavior
   */
  @Test
  public void testHierarchical1() {
    System.out.println("----testHierarchical1----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartHi1();

      /**** START AND RUN ****/
      MyMetadata myData = new MyMetadata();
      Assert.assertTrue(chart.start(myData));
      Assert.assertTrue(chart.dispatch(myData, new Event1()));
      Assert.assertTrue(chart.dispatch(myData, new Event1()));
      Assert.assertTrue(chart.dispatch(myData, new Event1()));
      Assert.assertTrue(chart.dispatch(myData, new Event2()));
      Assert.assertTrue(chart.dispatch(myData, new Event2()));

      /**** SHUTDOWN AND VERIFY ****/
      myData.shutdown();

      String expected = ": H1 >'H1_start'H1:H2 >'H2_start'H1:H2:A\n";
      expected += "Event1: 'Event1_inner'H1:H2:B\n";
      expected += "Event1: 'Event1_outer'H1 >'H1_start'H1:H2 >'H2_start'H1:H2:A\n";
      expected += "Event1: 'Event1_inner'H1:H2:B\n";
      expected += "Event2: 'Event2_inner'H1:H2:C >'H2_final'H1:H2:H2_final >'H1_final'H1:H1_final >'H1_H1'H1 >'H1_start'H1:H2 >'H2_start'H1:H2:A\n";
      expected += "Event2: 'Event2_outer'final\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  /**
   * constructs the statechart Hi2
   */
  public Statechart buildChartHi2() throws StatechartException {
    Statechart chart = new Statechart("Hi2");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    HierarchicalState h1 = new HierarchicalState("H1", chart, new TraceAction("H1_entry"), null, new TraceAction("H1_exit"));
    HierarchicalState h2 = new HierarchicalState("H2", h1, new TraceAction("H2_entry"), null, new TraceAction("H2_exit"));
    HierarchicalState h3 = new HierarchicalState("H3", chart, new TraceAction("H3_entry"), null, new TraceAction("H3_exit"));
    State a = new State("A", h3, new TraceAction("A_entry"), null, new TraceAction("A_exit"));
    State b = new State("B", h1, new TraceAction("B_entry"), null, new TraceAction("B_exit"));
    State c = new State("C", h2, new TraceAction("C_entry"), null, new TraceAction("C_exit"));

    /** transitions */
    new Transition(chart_start, c, new TraceAction("from_start"));
    new Transition(c, a, new Event1(), new TraceAction("C_to_A"));
    new Transition(a, c, new Event1(), new TraceAction("A_to_C"));
    new Transition(c, b, new Event2(), new TraceAction("C_to_B"));
    new Transition(b, c, new TraceAction("B_to_C"));
    new Transition(c, chart_final, new Event3(), new TraceAction("to_final"));
    return chart;
  }

  /**
   * Tests transitions exiting and entering several hierarchical states, test
   * for wrong calling sequence of exit and entry action, check whether action
   * of transition is called in between
   */
  @Test
  public void testHierarchical2() {
    System.out.println("----testHierarchical2----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartHi2();

      /** test getState() */
      Assert.assertNull(chart.getState("x"));
      Assert.assertNull(chart.getState("H1:H2:C:C"));

      State c = chart.getState("H1:H2:C");
      Assert.assertNotNull(c);
      Assert.assertEquals(c.toString(), "C");

      State h2 = chart.getState("H1:H2");
      Assert.assertNotNull(h2);
      Assert.assertEquals(h2.toString(), "H2");

      State b = chart.getState("H1:B");
      Assert.assertNotNull(b);
      Assert.assertEquals(b.toString(), "B");

      State h1 = chart.getState("H1");
      Assert.assertNotNull(h1);
      Assert.assertEquals(h1.toString(), "H1");

      State start = chart.getState("start");
      Assert.assertNotNull(start);
      Assert.assertEquals(start.toString(), "start");

      // TODO: also for ConcurrentStates

      /**** START AND RUN ****/
      /** test getStateConfiguration() **/
      MyMetadata data = new MyMetadata();
      Assert.assertEquals(data.getStateConfiguration(), "");

      Assert.assertTrue(data.start(chart));
      Assert.assertEquals(data.getStateConfiguration(), "H1:H2:C");

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertEquals(data.getStateConfiguration(), "H3:A");

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertEquals(data.getStateConfiguration(), "H1:H2:C");

      Assert.assertTrue(data.dispatch(new Event2()));
      Assert.assertEquals(data.getStateConfiguration(), "H1:H2:C");

      Assert.assertTrue(data.dispatch(new Event3()));
      Assert.assertEquals(data.getStateConfiguration(), "final");

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      Assert.assertEquals(data.getStateConfiguration(), "");

      String expected = ": 'from_start''H1_entry''H2_entry''C_entry'H1:H2:C\n";
      expected += "Event1: 'C_exit''H2_exit''H1_exit''C_to_A''H3_entry''A_entry'H3:A\n";
      expected += "Event1: 'A_exit''H3_exit''A_to_C''H1_entry''H2_entry''C_entry'H1:H2:C\n";
      expected += "Event2: 'C_exit''H2_exit''C_to_B''B_entry'H1:B >'B_exit''B_to_C''H2_entry''C_entry'H1:H2:C\n";
      expected += "Event3: 'C_exit''H2_exit''H1_exit''to_final'final\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  /**
   * constructs the statechart Hi3
   */
  public Statechart buildChartHi3() throws StatechartException {
    Statechart chart = new Statechart("Hi3");

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    HierarchicalState h1 = new HierarchicalState("H1", chart, new TraceAction("H1_entry"), null, new TraceAction("H1_exit"));
    PseudoState h1_start = new PseudoState("H1_start", h1, PseudoState.pseudostate_start);
    State h1_a = new State("A", h1, new TraceAction("H1A_entry"), new TraceAction("H1A_do"), new TraceAction("H1A_exit"));
    FinalState h1_final = new FinalState("H1_final", h1);

    HierarchicalState h2 = new HierarchicalState("H2", chart, new TraceAction("H2_entry"), null, new TraceAction("H2_exit"));
    PseudoState h2_start = new PseudoState("H2_start", h2, PseudoState.pseudostate_start);
    State h2_a = new State("A", h2, new TraceAction("H2A_entry"), new TraceAction("H2A_do"), new TraceAction("H2A_exit"));
    State h2_b = new State("B", h2, new TraceAction("B_entry"), new TraceAction("B_do"), new TraceAction("B_exit"));
    FinalState h2_final = new FinalState("H2_final", h2);

    /** transitions */
    new Transition(chart_start, h1);
    // H1 --
    new Transition(h1_start, h1_a);
    new Transition(h1_a, h1_final, new Event1());
    // H1 --
    new Transition(h1, chart_final, new Event1());
    new Transition(h1, h2, new Event2());
    // H2 --
    new Transition(h2_start, h2_a);
    new Transition(h2_a, h2_b, new Event2());
    new Transition(h2_b, h2_final);
    // H2 --
    new Transition(h2, chart_final, new Event2());

    return chart;
  }

  @Test
  public void testHierarchical3() {
    System.out.println("----testHierarchical3----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartHi3();

      /**** START AND RUN ****/
      MyMetadata data = new MyMetadata();
      Assert.assertTrue(data.start(chart));

      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertTrue(data.dispatch(new Event1()));

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      String expected = ": 'H1_entry'H1 >'H1A_entry''H1A_do'H1:A\n";
      expected += "Event1: 'H1A_exit'H1:H1_final\n";
      expected += "Event1: 'H1_exit'final\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

      /**** START AND RUN ****/
      data = new MyMetadata();
      Assert.assertTrue(data.start(chart));

      Assert.assertTrue(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event2()));

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      expected = ": 'H1_entry'H1 >'H1A_entry''H1A_do'H1:A\n";
      expected += "Event2: 'H1A_exit''H1_exit''H2_entry'H2 >'H2A_entry''H2A_do'H2:A\n";
      expected += "Event2: 'H2A_exit''B_entry''B_do'H2:B >'B_exit'H2:H2_final\n";
      expected += "Event2: 'H2_exit'final\n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  /**
   * constructs the statechart Hi4
   */
  public Statechart buildChartHi4(String namePostfix, boolean firstEvent, boolean secondEvent, boolean thirdEvent) throws StatechartException {
    Statechart chart = new Statechart("Hi4" + namePostfix);

    /** states */
    PseudoState chart_start = new PseudoState("start", chart, PseudoState.pseudostate_start);
    FinalState chart_final = new FinalState("final", chart);

    State a = new State("A", chart, new TraceAction("A_entry"), null, new TraceAction("A_exit"));
    State b = new State("B", chart, new TraceAction("B_entry"), null, new TraceAction("B_exit"));
    HierarchicalState h1 = new HierarchicalState("H1", chart, new TraceAction("H1_entry"), null, new TraceAction("H1_exit"));
    PseudoState h1_start = new PseudoState("h1_start", h1, PseudoState.pseudostate_start);
    FinalState h1_final = new FinalState("h1_final", h1);

    HierarchicalState h2 = new HierarchicalState("H2", h1, new TraceAction("H2_entry"), null, new TraceAction("H2_exit"));
    PseudoState h2_start = new PseudoState("h2_start", h2, PseudoState.pseudostate_start);
    FinalState h2_final = new FinalState("h2_final", h2);

    HierarchicalState h3 = new HierarchicalState("H3", h2, new TraceAction("H3_entry"), null, new TraceAction("H3_exit"));
    PseudoState h3_start = new PseudoState("h3_start", h3, PseudoState.pseudostate_start);
    FinalState h3_final = new FinalState("h3_final", h3);

    HierarchicalState h4 = new HierarchicalState("H4", h3, new TraceAction("H4_entry"), null, new TraceAction("H4_exit"));
    PseudoState h4_start = new PseudoState("h4_start", h4, PseudoState.pseudostate_start);
    State c = new State("C", h4, new TraceAction("C_entry"), null, new TraceAction("C_exit"));
    State d = new State("D", h4, new TraceAction("D_entry"), null, new TraceAction("D_exit"));
    FinalState h4_final = new FinalState("h4_final", h4);

    /** transitions */
    new Transition(chart_start, a);
    new Transition(a, h1, firstEvent ? new Event1() : null);
    new Transition(h1, b, thirdEvent ? new Event1() : null);
    new Transition(b, chart_final);

    new Transition(h1_start, h2);
    new Transition(h2, h1_final);

    new Transition(h2_start, h3);
    new Transition(h3, h2_final);

    new Transition(h3_start, h4);
    new Transition(h4, h3_final);

    new Transition(h4_start, c);
    new Transition(c, d, secondEvent ? new Event1() : null);
    new Transition(d, h4_final);

    return chart;
  }

  private static final String expectedPart1 = "'A_entry'A";
  private static final String expectedPart2 = "'A_exit''H1_entry'H1 >'H2_entry'H1:H2 >'H3_entry'H1:H2:H3 >'H4_entry'H1:H2:H3:H4 >'C_entry'H1:H2:H3:H4:C";
  private static final String expectedPart3 = "'C_exit''D_entry'H1:H2:H3:H4:D >'D_exit'H1:H2:H3:H4:h4_final >'H4_exit'H1:H2:H3:h3_final >'H3_exit'H1:H2:h2_final >'H2_exit'H1:h1_final";
  private static final String expectedPart4 = "'H1_exit''B_entry'B >'B_exit'final";

  @Test
  public void testHierarchical4_1() {
    System.out.println("----testHierarchical4_1----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartHi4("_1", true, true, true);

      /**** START AND RUN ****/
      MyMetadata data = new MyMetadata();
      Assert.assertTrue(data.start(chart));

      Assert.assertFalse(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertFalse(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertFalse(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertFalse(data.dispatch(new Event2()));

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      String expected = ": " + expectedPart1 + "\n";
      expected += "Event2: \n";
      expected += "Event1: " + expectedPart2 + "\n";
      expected += "Event2: \n";
      expected += "Event1: " + expectedPart3 + "\n";
      expected += "Event2: \n";
      expected += "Event1: " + expectedPart4 + "\n";
      expected += "Event2: \n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testHierarchical4_2() {
    System.out.println("----testHierarchical4_2----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartHi4("_2", true, false, true);

      /**** START AND RUN ****/
      MyMetadata data = new MyMetadata();
      Assert.assertTrue(data.start(chart));

      Assert.assertFalse(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertFalse(data.dispatch(new Event2()));
      Assert.assertTrue(data.dispatch(new Event1()));
      Assert.assertFalse(data.dispatch(new Event2()));

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      String expected = ": " + expectedPart1 + "\n";
      expected += "Event2: \n";
      expected += "Event1: " + expectedPart2 + " >" + expectedPart3 + "\n";
      expected += "Event2: \n";
      expected += "Event1: " + expectedPart4 + "\n";
      expected += "Event2: \n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testHierarchical4_3() {
    System.out.println("----testHierarchical4_3----");
    try {
      /**** BUILD STATECHART ****/
      Statechart chart = buildChartHi4("_3", false, false, false);

      /**** START AND RUN ****/
      MyMetadata data = new MyMetadata();
      Assert.assertTrue(data.start(chart));

      Assert.assertFalse(data.dispatch(new Event2()));
      Assert.assertFalse(data.dispatch(new Event1()));

      /**** SHUTDOWN AND VERIFY ****/
      data.shutdown();
      String expected = ": " + expectedPart1 + " >" + expectedPart2 + " >" + expectedPart3 + " >" + expectedPart4 + "\n";
      expected += "Event2: \n";
      expected += "Event1: \n";
      Assert.assertEquals(expected, CollectingLineTracer.fetchCollectedTrace());

    } catch (StatechartException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }
  }

  @AfterClass
  public static void shutdownTests() {
    Statechart.shutdown();
  }

  public void testAll() {
    testHierarchical1();
    testHierarchical2();
    testHierarchical3();
    testHierarchical4_1();
    testHierarchical4_2();
    testHierarchical4_3();
    shutdownTests();
  }

  public static void main(String[] args) {
    HierarchicalTest test = new HierarchicalTest();
    test.testAll();
  }
}
