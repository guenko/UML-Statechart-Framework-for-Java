/*
 * UML statechart framework (http://github.com/klangfarbe/UML-Statechart-Framework-for-Java)
 *
 * Copyright (C) 2006-2010 Christian Mocek (christian.mocek@googlemail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package statechart.unittests;

import org.junit.Assert;
import org.junit.Test;
import statechart.FinalState;
import statechart.Metadata;
import statechart.State;
import statechart.Statechart;
import statechart.StatechartException;
import statechart.trace.CollectingLineTracer;

public class SemanticTests {

  /** used for better tracing */
  public static class MyMetadata extends Metadata {

    MyMetadata() {
      this("");
    }

    MyMetadata(String name) {
      super(new CollectingLineTracer(name));
      setParameter(new TestParameter());
    }

    String getPath() {
      return ((TestParameter)getParameter()).path;
    }

    String resetPath() {
      return ((TestParameter)getParameter()).path = "";
    }

    void setGuardValue(int guardvalue) {
      ((TestParameter)getParameter()).guardvalue = guardvalue;
    }
  }

  @Test
  public void testEventQueue() throws StatechartException, InterruptedException {
    // Statechart chart = TestCharts.t2(10);
    Statechart chart = TestCharts.t2(2);

    TestEvent s1 = new TestEvent(1);
    TestEvent s2 = new TestEvent(2);
    MyMetadata data = new MyMetadata("EventQueue");

    data.startAsynchron(chart);
    data.dispatchAsynchron(s1);
    data.dispatchAsynchron(s2);

    // Wait until the statechart reached its final state
    State current = null;
    while (current == null || !(current instanceof FinalState)) {
      Thread.sleep(100);
      synchronized (data) {
        current = data.getData(chart).currentState;
      }
    }
    data.shutdown();
    Assert.assertEquals("D:start A:a D:a A:a D:a A:end", data.getPath());
  }

  @Test
  public void testSemantics1() throws StatechartException {
    Statechart chart = TestCharts.t1();

    MyMetadata data = new MyMetadata("Semantics1");

    Assert.assertTrue(chart.start(data));
    Assert.assertEquals("D:start A:a D:a A:b D:b A:end", data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics2() throws StatechartException {
    Statechart chart = TestCharts.t2(10);

    TestEvent s1 = new TestEvent(1);
    TestEvent s2 = new TestEvent(2);
    MyMetadata data = new MyMetadata("Semantics2");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, s1));
    Assert.assertTrue(chart.dispatch(data, s2));
    Assert.assertEquals("D:start A:a D:a A:a D:a A:end", data.getPath());

    // check if more than one signals create a longer way
    data.resetPath();

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, s1));
    Assert.assertTrue(chart.dispatch(data, s1));
    Assert.assertTrue(chart.dispatch(data, s2));
    Assert.assertEquals("D:start A:a D:a A:a D:a A:a D:a A:end", data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics3() throws StatechartException {
    Statechart chart = TestCharts.t3();

    TestEvent s1 = new TestEvent(1);
    MyMetadata data = new MyMetadata("Semantics3");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, s1));
    Assert.assertEquals("D:start A:a D:a A:b D:b A:end", data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics4() throws StatechartException, InterruptedException {
    Statechart chart = TestCharts.t3();

    MyMetadata data = new MyMetadata("Semantics4");

    Assert.assertTrue(chart.start(data));

    State current = null;
    while (current == null || !(current instanceof FinalState)) {
      Thread.sleep(100);
      synchronized (data) {
        current = data.getData(chart).currentState;
      }
    }

    Assert.assertEquals("D:start A:a D:a A:end", data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics5() throws StatechartException {
    Statechart chart = TestCharts.t4();

    MyMetadata data = new MyMetadata("Semantics5");
    data.setGuardValue(0);

    Assert.assertTrue(chart.start(data));
    Assert.assertEquals("D:start A:a D:a A:end", data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics6() throws StatechartException {
    Statechart chart = TestCharts.t4();

    MyMetadata data = new MyMetadata("Semantics6");
    data.setGuardValue(1);

    Assert.assertTrue(chart.start(data));
    Assert.assertEquals("D:start A:a D:a A:j1 D:j1 E:a1 A:b D:b A:end", data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics7() throws StatechartException {
    Statechart chart = TestCharts.t4();

    MyMetadata data = new MyMetadata("Semantics7");
    data.setGuardValue(2);

    Assert.assertTrue(chart.start(data));
    Assert.assertEquals(
        "D:start A:a D:a A:j1 D:j1 E:a2 A:c D:c A:j2 D:j2 E:a3 A:j3 D:j3 E:a4 A:end",
        data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics8() throws StatechartException {
    Statechart chart = TestCharts.h1();

    MyMetadata data = new MyMetadata("Semantics8");

    Assert.assertTrue(chart.start(data));
    Assert.assertEquals(
        "D:start A:p A:start p D:start p A:a D:a A:b D:b A:end p D:end p D:p A:end",
        data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics9() throws StatechartException {
    Statechart chart = TestCharts.h2();

    TestEvent event = new TestEvent(1);
    MyMetadata data = new MyMetadata("Semantics9");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event));
    Assert.assertEquals(
        "D:start A:p A:start p D:start p A:a D:a A:b D:b A:end p D:end p D:p A:end",
        data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics10() throws StatechartException {
    Statechart chart = TestCharts.h3();

    TestEvent event = new TestEvent(1);
    MyMetadata data = new MyMetadata("Semantics10");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event));
    Assert.assertEquals(
        "D:start A:p A:start p D:start p A:a D:a A:b D:b A:end p D:end p D:p A:end",
        data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics11() throws StatechartException {
    Statechart chart = TestCharts.h3();

    TestEvent event = new TestEvent(2);
    MyMetadata data = new MyMetadata("Semantics11");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event));
    Assert.assertEquals("D:start A:p A:start p D:start p A:a D:a D:p A:end",
        data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics12() throws StatechartException {
    Statechart chart = TestCharts.h4();

    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);
    TestEvent event3 = new TestEvent(3);
    MyMetadata data = new MyMetadata("Semantics12");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event3));

    String result = "D:start A:p A:start p D:start p U:history p A:a D:a ";
    result += "D:p A:p A:start p D:start p A:a D:a A:b D:b D:p A:p ";
    result += "A:start p D:start p A:b D:b A:a D:a D:p A:p A:start p ";
    result += "D:start p A:a D:a A:b D:b A:end p D:end p D:p A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics13() throws StatechartException {
    Statechart chart = TestCharts.h5();

    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);
    TestEvent event3 = new TestEvent(3);
    TestEvent event4 = new TestEvent(4);
    MyMetadata data = new MyMetadata("Semantics13");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event4));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event3));

    String result = "D:start A:p A:start p D:start p U:history p A:a D:a ";
    result += "A:q A:start q D:start q A:b D:b A:c D:c D:q D:p A:p ";
    result += "A:start p D:start p A:q A:c D:c D:q A:a D:a A:q ";
    result += "A:start q D:start q A:b D:b D:q A:end p D:end p D:p A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics14() throws StatechartException {
    Statechart chart = TestCharts.h6();

    MyMetadata data = new MyMetadata("Semantics14");

    Assert.assertTrue(chart.start(data));
    Assert.assertEquals("D:start A:p A:q A:r D:r D:q D:p A:x A:y D:y D:x A:end",
        data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics15() throws StatechartException {
    Statechart chart = TestCharts.c1();

    MyMetadata data = new MyMetadata("Semantics15");

    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b ";
    result += "D:a ";
    result += "A:end p-r1 ";
    result += "D:b ";
    result += "A:end p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics16() throws StatechartException {
    Statechart chart = TestCharts.c2();

    MyMetadata data = new MyMetadata("Semantics16");

    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:c ";
    result += "D:a ";
    result += "A:b ";
    result += "D:b ";
    result += "A:end p-r1 ";
    result += "D:c ";
    result += "A:d ";
    result += "D:d ";
    result += "A:e ";
    result += "D:e ";
    result += "A:end p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics17() throws StatechartException {
    Statechart chart = TestCharts.c2();

    MyMetadata data = new MyMetadata("Semantics17");
    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));

    String result = "D:start ";

    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:c ";
    // c . d
    result += "D:c ";
    result += "A:d "; // a . b
    result += "D:a ";
    result += "A:b "; // d . e
    result += "D:d ";
    result += "A:e ";
    // b . end p-r2
    result += "D:b ";
    result += "A:end p-r1 "; // e . end p-r2
    result += "D:e ";
    result += "A:end p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics18() throws StatechartException {
    Statechart chart = TestCharts.c2();

    MyMetadata data = new MyMetadata("Semantics18");
    TestEvent event1 = new TestEvent(1);
    TestEvent event3 = new TestEvent(3);

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event3));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:c "; // c . d
    result += "D:c ";
    result += "A:d "; // S3 Transition
    result += "D:a ";
    result += "D:p-r1 ";
    result += "D:d ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics19() throws StatechartException {
    Statechart chart = TestCharts.c3();

    MyMetadata data = new MyMetadata("Semantics19");

    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b ";
    result += "A:p-r1 ";
    result += "A:a "; // a . end p-r1
    result += "D:a ";
    result += "A:end p-r1 "; // b . end p-r2
    result += "D:b ";
    result += "A:end p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics20() throws StatechartException {
    Statechart chart = TestCharts.c4();

    MyMetadata data = new MyMetadata("Semantics20");

    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b "; // a . end
    result += "D:a ";
    result += "D:p-r1 ";
    result += "D:b ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics21() throws StatechartException {
    Statechart chart = TestCharts.c5();

    MyMetadata data = new MyMetadata("Semantics21");
    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(!chart.dispatch(data, event2));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b "; // a . end
    result += "D:a ";
    result += "D:p-r1 ";
    result += "D:b ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics22() throws StatechartException {
    Statechart chart = TestCharts.c5();

    MyMetadata data = new MyMetadata("Semantics22");
    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event1));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b "; // b . c
    result += "D:b ";
    result += "A:c "; // a . end
    result += "D:a ";
    result += "D:p-r1 ";
    result += "D:c ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics23() throws StatechartException {
    Statechart chart = TestCharts.c6();

    MyMetadata data = new MyMetadata("Semantics23");

    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:fork ";
    result += "D:fork ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:b ";
    result += "D:a ";
    result += "A:end p-r1 ";
    result += "D:b ";
    result += "A:end p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics24() throws StatechartException {
    Statechart chart = TestCharts.c7();

    MyMetadata data = new MyMetadata("Semantics24");
    data.setGuardValue(1);

    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:fork ";
    result += "D:fork ";
    result += "A:p ";
    result += "A:p-r3 ";
    result += "A:c ";
    result += "A:p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:b ";
    result += "D:a ";
    result += "A:end p-r1 ";
    result += "D:b ";
    result += "A:end p-r2 ";
    result += "D:c ";
    result += "A:end p-r3 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:end p-r3 ";
    result += "D:p-r3 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics25() throws StatechartException {
    Statechart chart = TestCharts.c7();

    MyMetadata data = new MyMetadata("Semantics25");
    data.setGuardValue(0);
    
    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:fork ";
    result += "D:fork ";
    result += "A:p ";
    result += "A:p-r3 ";
    result += "A:start p-r3 ";
    result += "D:start p-r3 ";
    result += "A:d ";
    result += "A:p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:b ";
    result += "D:a ";
    result += "A:end p-r1 ";
    result += "D:b ";
    result += "A:end p-r2 ";
    result += "D:d ";
    result += "A:end p-r3 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:end p-r2 ";
    result += "D:p-r2 ";
    result += "D:end p-r3 ";
    result += "D:p-r3 ";
    result += "D:p ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics26() throws StatechartException {
    Statechart chart = TestCharts.c8();

    MyMetadata data = new MyMetadata("Semantics26");

    Assert.assertTrue(chart.start(data));

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b ";
    result += "D:b ";
    result += "A:c ";
    result += "D:a ";
    result += "D:p-r1 ";
    result += "D:c ";
    result += "D:p-r2 ";
    result += "D:p ";
    result += "A:join ";
    result += "D:join ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics27() throws InterruptedException, StatechartException {
    Statechart chart = TestCharts.c9();

    MyMetadata data = new MyMetadata("Semantics27");

    Assert.assertTrue(chart.start(data));

    State current = null;
    while (current == null || !(current instanceof FinalState)) {
      Thread.sleep(100);
      synchronized (data) {
        current = data.getData(chart).currentState;
      }
    }

    String result = "D:start ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:a ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b ";
    result += "A:p-r3 ";
    result += "A:start p-r3 ";
    result += "D:start p-r3 ";
    result += "A:d ";
    result += "D:b ";
    result += "A:c ";
    result += "D:a ";
    result += "D:p-r1 ";
    result += "D:c ";
    result += "D:p-r2 ";
    result += "D:d ";
    result += "D:p-r3 ";
    result += "D:p ";
    result += "A:join ";
    result += "D:join ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  @Test
  public void testSemantics28() throws StatechartException {
    Statechart chart = TestCharts.c10();

    MyMetadata data = new MyMetadata("Semantics28");
    TestEvent event1 = new TestEvent(1);
    TestEvent event2 = new TestEvent(2);

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event1));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event2));

    String result = "D:start ";
    result += "A:c ";
    result += "D:c ";
    // S1
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:end p-r1 ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "E:history ";
    result += "A:a "; // S1
    result += "D:a ";
    result += "A:b "; // S2
    result += "D:b ";
    result += "D:p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:p ";
    result += "A:c "; // S1
    result += "D:c ";
    result += "A:p ";
    result += "A:p-r1 ";
    result += "A:start p-r1 ";
    result += "D:start p-r1 ";
    result += "A:end p-r1 ";
    result += "A:p-r2 ";
    result += "A:start p-r2 ";
    result += "D:start p-r2 ";
    result += "A:b "; // S2
    result += "D:b ";
    result += "D:p-r2 ";
    result += "D:end p-r1 ";
    result += "D:p-r1 ";
    result += "D:p ";
    result += "A:c "; // S2
    result += "D:c ";
    result += "A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

  // Checks the deep history state when the transition is made from the substate
  @Test
  public void testSemantics29() throws StatechartException {
    Statechart chart = TestCharts.h5();

    TestEvent event2 = new TestEvent(2);
    TestEvent event3 = new TestEvent(3);
    TestEvent event4 = new TestEvent(4);
    TestEvent event5 = new TestEvent(5);

    MyMetadata data = new MyMetadata("Semantics29");

    Assert.assertTrue(chart.start(data));
    Assert.assertTrue(chart.dispatch(data, event2));
    Assert.assertTrue(chart.dispatch(data, event4));
    Assert.assertTrue(chart.dispatch(data, event5));
    Assert.assertTrue(chart.dispatch(data, event3));

    String result = "D:start A:p A:start p D:start p U:history p A:a D:a ";
    result += "A:q A:start q D:start q A:b D:b A:c D:c D:q D:p A:d D:d A:p ";
    result += "A:start p D:start p A:q A:c D:c D:q A:end p D:end p D:p A:end";

    Assert.assertEquals(result, data.getPath());
    data.shutdown();
  }

}
