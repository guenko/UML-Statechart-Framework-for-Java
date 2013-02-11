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

import java.util.HashMap;
import java.util.Map;
import statechart.eventqueue.BlockingEventQueue;
import statechart.eventqueue.EventQueue;
import statechart.trace.TracingAdapter;

/**
 * Describes runtime specific data related to a statechart. The main data is the
 * currently active state, or in general all active states when using hierarchy.
 * For every active state a StateRuntimedata Object is created storing runtime
 * specific data for this state (e.g. the time since entering the state). The
 * StateRuntimedata object is allocated only when it is needed (at least then
 * the state is active), otherwise it is deleted.
 */
public class Metadata {

  /** the state chart this data object is associated with */
  private Statechart chart = null;
  /** The event queue for the events belonging to this data object */
  private EventQueue eventQueue = null;
  /** key map holding the StateRuntimedata per state when it is needed */
  private Map<State, StateRuntimedata> activeStates = new HashMap<State, StateRuntimedata>();
  /** user may store own data there */
  private Parameter parameter = null;
  /**
   * the current tracer used for the tracing feature, if not set tracing is
   * disabled
   */
  private TracingAdapter tracer = null;

  /**
   * Creates a Metadata object without tracing facility.
   */
  public Metadata() {
  }

  /**
   * Creates a Metadata object with the given Tracing adapter.
   * 
   * @param tracer Tracer to be used
   */
  public Metadata(TracingAdapter tracer) {
    this.tracer = tracer;
  }

  /**
   * @return the event queue associated with this data object
   */
  public EventQueue getEventQueue() {
    return eventQueue;
  }

  /**
   * @return user parameter if set, null otherwise
   */
  public Parameter getParameter() {
    return parameter;
  }

  /**
   * @param parameter user specific data
   */
  public void setParameter(Parameter parameter) {
    this.parameter = parameter;
  }

  /**
   * @param tracer the current tracer used for the tracing feature, when null
   *          tracing is disabled
   */
  public void setTracer(TracingAdapter tracer) {
    this.tracer = tracer;
  }

  /**
   * @return the current tracer used for the tracing feature, when null is
   *         returned tracing is disabled
   */
  public TracingAdapter getTracer() {
    return tracer;
  }

  //============================================================================

  /**
   * 
   */
  /**
   * Associates the given state chart with this data object and starts the state
   * machine by triggering the start transitions
   * 
   * @param chart the state chart this data object should be associated with
   * @return
   */
  public boolean start(Statechart chart) {
    this.chart = chart;
    chart.addDataReference(this);
    eventQueue = new BlockingEventQueue(chart);
    reset();
    activate(chart);
    activate(chart.startState);
    return dispatch(null);
  }

  /**
   * dispatches the event to the statechart, which takes care of delegating the
   * incoming event to the current state.
   */
  public boolean dispatch(Event event) {
    assert (chart != null);
    return chart.dispatch(this, event);
  }

  /**
   * Initializes the Statechart in the runtime data. Sets the start state and
   * triggers the initial state chart transitions asynchronously.
   * 
   * @throws InterruptedException
   */
  public boolean startAsynchron(Statechart chart) throws InterruptedException {
    this.chart = chart;
    chart.addDataReference(this);
    eventQueue = new BlockingEventQueue(chart);
    reset();
    activate(chart);
    activate(chart.startState);
    return dispatchAsynchron(null);
  }

  /**
   * Adds an event to the event queue.
   * 
   * @param event
   * @return whether the event is placed to the event queue
   */
  public boolean dispatchAsynchron(Event event) {
    assert (chart != null);
    // TODO: really needed?
    if (eventQueue.isShutdown()) {
      return false;
    }
    return eventQueue.addEvent(this, event);
  }

  /**
   * Shutdown the data object
   */
  public void shutdown() {
    // shutdown the event queue
    eventQueue.shutdown(1000);
    // remove reference from the state chart if it is already initialized
    if (chart != null) {
      chart.remvoeDataReference(this);
      chart = null;
    }
    reset();
  }

  //============================================================================

  /**
   * Checks whether the given state is active or not.
   */
  public boolean isActive(State state) {
    assert (state != null);
    if (activeStates.containsKey(state)) {
      return getData(state).active;
    }
    return false;
  }

  //============================================================================

  /**
   * Gets the runtime specific data of the state.
   * 
   * @return The data or NULL if the state is not active
   */
  public StateRuntimedata getData(State state) {
    return activeStates.get(state);
  }

  //============================================================================

  /**
   * Activates a state for this Metadata. If the state is not in the Hashmap it
   * will be added and a new StateRuntimeData is created.
   */
  void activate(State state) {
    StateRuntimedata data = getData(state);
    if (data == null) {
      data = new StateRuntimedata();
      activeStates.put(state, data);
    }

    data.active = true;
    data.currentTime = System.currentTimeMillis();
    data.currentState = null;

    // update the context. if context is null we are at top level
    if (state.parent != null) {
      data = activeStates.get(state.parent);
      data.currentState = state;
    }
  }

  //============================================================================

  /**
   * Deactivates the state and frees the allocated resources.
   */
  void deactivate(State state) {
    if (activeStates.containsKey(state)) {
      StateRuntimedata data = getData(state);

      // If we store the history of a hierarchical state, keep it
      if (state instanceof PseudoState && (((PseudoState)state).type == PseudoState.pseudostate_deep_history || ((PseudoState)state).type == PseudoState.pseudostate_history)) {
        data.active = false;
        return;
      }
      data = null;
      activeStates.remove(state);
    }
  }

  //============================================================================

  /**
   * Gets the runtime data for a state. The difference to the normal getData
   * method is, that a new StateRuntimedata is created if it don't exists in the
   * hashmap. If the data already exists, it is returned instead.
   */
  StateRuntimedata createRuntimedata(State s) {
    StateRuntimedata data = activeStates.get(s);
    if (data == null) {
      data = new StateRuntimedata();
      activeStates.put(s, data);
    }
    return data;
  }

  //============================================================================

  /**
   * Resets the metadata object for reuse
   */
  public void reset() {
    activeStates.clear();
  }

  /**
   * Get a string representing the current state configuration starting with the
   * current substate of the statechart (the name of the statechart itself is
   * not included). This shows all active end node states of all concurrent
   * states.
   */
  public String getStateConfiguration() {
    if (chart == null) {
      return "";
    }
    // get first level substate of the statechart this data is associated
    State currentState = getData(chart).currentState;
    // if statechart data is not yet initialized
    if (currentState == null) {
      return "";
    }
    // start with state name of the current substate of the first level, so the
    // statechart name itself is not included
    return currentState.getStateConfigurationInternal(this);
  }
}
