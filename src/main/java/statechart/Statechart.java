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
package statechart;

import java.util.ArrayList;
import java.util.Vector;
import statechart.timeout.ScheduledTimerManager;
import statechart.timeout.TimerManager;

/**
 * The main entry point for using the statechart framework. Contains all
 * necessary methods for delegating incoming events to the substates. When
 * deleting the statechart all substates, actions, events, guards and transition
 * will be deleted automatically.
 */
public class Statechart extends Context {

  public static final String VERSION = "1.2.0";

  /** delimiter character separating the state names in a state path string */
  public final static char STATE_PATH_DELIMITER = ':';
  /** starting character for the active states list of a concurrent state */
  public final static char CONCURRENT_STATE_START_CHAR = '(';
  /** closing character for the active states list of a concurrent state */
  public final static char CONCURRENT_STATE_END_CHAR = ')';
  /**
   * character delimiting the elements in the active states list of a concurrent
   * state
   */
  public final static char CONCURRENT_STATE_DELIMTER_CHAR = '|';

  // private static SchedulerFactory = new DefaultSchedulerFactory();
  private static TimerManager timerManager = null;
  private static ArrayList<Metadata> dataReferenceList = new ArrayList<Metadata>();

  /**
   * Creates the Statechart with the given default and maximum number of
   * threads.
   * 
   * @param name The name of the statechart. This must be unique for all
   *          statecharts in the running JVM.
   * @param threads The maximum number of threads available in the threadpool.
   * @param makeDaemonThreads Specifies if the created threads should be daemon
   *          or non-daemon threads.
   * @throws StatechartException
   */
  public Statechart(String name, int threads, boolean makeDaemonThreads) throws StatechartException {
    super(name, null, null, null, null);
  }

  public Statechart(String name) throws StatechartException {
    this(name, 10, false);
  }

  static TimerManager getTimerManager() {
    if (timerManager == null) {
      timerManager = new ScheduledTimerManager();
    }
    return timerManager;
  }

  void addDataReference(Metadata data) {
    dataReferenceList.add(data);
  }

  void remvoeDataReference(Metadata data) {
    dataReferenceList.remove(data);
  }

  //============================================================================

  /**
   * Associates this state chart with the given data object and starts the state
   * machine synchronously.
   */
  public boolean start(Metadata data) {
    return data.start(this);
  }

  /**
   * Overrides the dispatch method from the state and takes care of delegating
   * the incoming event to the current state.
   */
  public boolean dispatch(Metadata data, Event event) {
    if (data.getTracer() != null) {
      data.getTracer().eventDispatched(event);
    }
    State currentState = data.getData(this).currentState;
    boolean rc = currentState.dispatch(data, event);

    // call dispatch as long as we hit states with completion transitions
    do {
      if (data.getTracer() != null && rc) {
        data.getTracer().stateChanged(data);
      }
      currentState = data.getData(this).currentState;
    } while (currentState != null && currentState.dispatch(data, null));

    if (data.getTracer() != null) {
      data.getTracer().traceEventFinished();
    }
    return rc;
  }

  //============================================================================

  /**
   * Associates this state chart with the given data object and starts the state
   * machine asynchronously.
   * 
   * @throws InterruptedException
   */
  public boolean startAsynchron(Metadata data) throws InterruptedException {
    return data.startAsynchron(this);
  }

  /**
   * Adds an event to the event queue.
   */
  public boolean dispatchAsynchron(Metadata data, Event event) {
    return data.dispatchAsynchron(event);
  }

  //============================================================================

  /**
   * Shutdown of the statechart framework, so shutdown the timer manager and all
   * attached metadata objects
   */
  public static void shutdown() {
    if (timerManager != null) {
      timerManager.shutdown(1000);
      timerManager = null;
    }
    @SuppressWarnings("unchecked")
    ArrayList<Metadata> list = (ArrayList<Metadata>)dataReferenceList.clone();
    for (Metadata data : list) {
      data.shutdown();
    }
  }

  //============================================================================

  /**
   * Sets a new data object to continue from the given state. This is usually
   * needed for persistance to restore the state after a restart of the JVM or
   * if the data object was deleted sometime ago.
   */
  public boolean restoreState(State state, Metadata data) {
    if (data.isActive(this)) {
      return false;
    }
    data.reset();
    // get the path from the state to the root
    Vector<State> path = new Vector<State>();
    State parent = state;
    do {
      path.add(0, parent);
      parent = parent.parent;
    } while (parent != null);

    for (State s : path) {
      s.activate(data);
    }
    return true;
  }

  //============================================================================

}
