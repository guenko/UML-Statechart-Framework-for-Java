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
import statechart.timeout.TimerHandle;

/**
 * Represents a simple state of the statechart. For debugging purposes each
 * state can have a name assigned for identification.
 */
public class State {

  //============================================================================
  // ATTRIBUTES
  //============================================================================
  // The entry action to execute.
  protected Action entryAction = null;

  // The do action to execute.
  protected Action doAction = null;

  // The exit action to execute.
  protected Action exitAction = null;

  // List of the transitions leaving this state.
  protected ArrayList<Transition> transitions = new ArrayList<Transition>();

  // The parent of this state.
  protected Context parent = null;

  // The name of this state.
  protected String name = null;

  private static final char illegalNameChars[] = { Statechart.STATE_PATH_DELIMITER, Statechart.CONCURRENT_STATE_START_CHAR, Statechart.CONCURRENT_STATE_END_CHAR,
                                                  Statechart.CONCURRENT_STATE_DELIMTER_CHAR };

  /**
   * Creates a state as substate of the given parent and with the given actions.
   * 
   * @param name name of the state, has to be unique within the parent state.
   *          Not allowed to be null or empty and must not contain the path
   *          delimiter character
   *          {@value statechart.Context#STATE_PATH_DELIMITER}
   * @param parent the parent this states belongs, so this state is a substate
   *          of this parent. Null if this state is the top level state.
   * @param entryAction the entry action of the state
   * @param doAction the do-action of the state
   * @param exitAction the exit action of the state
   * @throws StatechartException
   */
  public State(String name, Context parent, Action entryAction, Action doAction, Action exitAction) throws StatechartException {

    if (parent == null && !(this instanceof Statechart)) {
      throw new StatechartException(StatechartException.PARENT_NULL);
    }
    if (name == null || name.isEmpty()) {
      throw new StatechartException(StatechartException.NAME_INVALID);
    }
    for (int i = 0; i < illegalNameChars.length; i++) {
      if (name.indexOf(illegalNameChars[i]) != -1) {
        throw new StatechartException(StatechartException.NAME_INVALID_CHARACTER);
      }
    }

    this.parent = parent;

    // get up until the the top level state is reached
    if (parent != null) {
      while (parent.parent != null) {
        parent = parent.parent;
      }
      // top level state has to be the statechart
      if (!(parent instanceof Statechart)) {
        throw new StatechartException(StatechartException.NO_TOP_LEVEL_STATECHART);
      }
      // the state name has to be unique within our parent
      if (this.parent.getSubstate(name) != null) {
        throw new StatechartException(StatechartException.NAME_NOT_UNIQUE);
      }
      this.parent.addSubstate(this);
    }

    this.name = name;
    this.entryAction = entryAction;
    this.doAction = doAction;
    this.exitAction = exitAction;
  }

  //============================================================================

  /**
   * Gets the parent of this state
   */
  public Context getParent() {
    return parent;
  };

  //============================================================================

  /**
   * Sets the entry action. If there is already an action given, it will be
   * destroyed first.
   */
  public void setEntryAction(Action action) {
    this.entryAction = action;
  }

  //============================================================================

  /**
   * Sets the do action. If there is already an action given, it will be
   * destroyed first.
   */
  public void setDoAction(Action action) {
    this.doAction = action;
  }

  //============================================================================

  /**
   * Sets the exit action. If there is already an action given, it will be
   * destroyed first.
   */
  public void setExitAction(Action action) {
    this.exitAction = action;
  }

  //============================================================================

  /**
   * Sets the name of the state used for debugging purposes.
   */
  public void setName(String name) {
    this.name = name;
  }

  //============================================================================

  /**
   * return the given name
   */
  public String toString() {
    assert (name != null);
    return name;
  }

  //============================================================================

  /**
   * Adds a transition to the list of transitions leaving this state.
   */
  void addTransition(Transition transition) {
    // make sure transition with guards are checked first!
    if (transition.hasGuard()) {
      transitions.add(0, transition);
    } else {
      transitions.add(transition);
    }
  }

  //============================================================================

  /**
   * Activates the state.
   */
  boolean activate(Metadata data) {
    if (!data.isActive(this)) {
      data.activate(this);

      // set up a timer if the state has an outgoing transition with a
      // TimeoutEvent
      for (int i = 0; i < transitions.size(); i++) {
        Transition t = transitions.get(i);
        if (t.event != null && t.event instanceof TimeoutEvent) {
          TimerHandle timerHandle = Statechart.getTimerManager().startTimer(this, data, (TimeoutEvent)t.event);
          assert (timerHandle != null);
          // set the timer handle in order to cancel if state is left before
          // timeout is raised
          StateRuntimedata runtimedata = data.getData(this);
          assert (runtimedata.timerHandle == null);
          runtimedata.timerHandle = timerHandle;
        }
      }

      if (entryAction != null) {
        entryAction.execute(data);
      }

      if (doAction != null) {
        doAction.execute(data);
      }
      return true;
    }
    return false;
  }

  //============================================================================

  /**
   * Deactivates the state.
   */
  void deactivate(Metadata data) {
    if (data.isActive(this)) {
      // Check if there are running timers started by a TimeoutEvent transition
      // leaving this state
      TimerHandle timerHandle = data.getData(this).timerHandle;
      if (timerHandle != null) {
        // if so cancel the running timer
        Statechart.getTimerManager().cancelTimer(timerHandle);
      }
      // we have to clear the timerHandle since the deactivate()
      // will not remove the RuntimeData in any case
      data.getData(this).timerHandle = null;
      data.deactivate(this);

      if (exitAction != null) {
        exitAction.execute(data);
      }
    }
  }

  //============================================================================

  /**
   * Dispatches the given event.
   */
  /**
   * Dispatch the given event to this state. Try to find an transition consuming
   * this event.
   * 
   * @param data the runtime data
   * @param event the event to dispatch
   * @return whether the event had triggered a transition and is consumed
   */
  boolean dispatch(Metadata data, Event event) {
    for (int i = 0; i < transitions.size(); i++) {
      if (transitions.get(i).execute(event, data)) {
        return true;
      }
    }
    return false;
  }

  //============================================================================

  /**
   * Get a string representing the current state configuration starting with
   * this state. This shows all active end node states of all concurrent states.
   */
  String getStateConfigurationInternal(Metadata data) {
    String traceString = toString();
    StateRuntimedata stateRuntimeData = data.getData(this);
    if (stateRuntimeData == null) {
      return traceString;
    }
    State currentState = stateRuntimeData.currentState;
    /*
     * check also for isActive (existence in activeState map), since in between
     * a transition the currentState may have a tangling reference (the state it
     * reference to is already removed from the activeStates map)
     */
    if (currentState != null && data.isActive(currentState)) {
      traceString += Statechart.STATE_PATH_DELIMITER + currentState.getStateConfigurationInternal(data);
    }
    return traceString;
  }

  /**
   * Retrieve the state in the hierarchical tree below this state identified by
   * the path relative to this state. This returned path specification lists the
   * substate names starting from the current level down to the bottom delimited
   * by the ':' character
   * 
   * @param path relative path starting with the substate of this state, e.g.
   *          "substate:subsubstate:subsubsubsate"
   * @return the state found or null if not found
   */
  public State getState(String path) {
    // when this method is not overwritten this state is a simple state, thus it
    // cannot have a substate
    return null;
  }
}
