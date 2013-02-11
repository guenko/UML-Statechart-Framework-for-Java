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

/**
 * The implementation of the OR composite state.
 */
public class HierarchicalState extends Context {
  //============================================================================
  // ATTRIBUTES
  //============================================================================
  PseudoState history = null;

  //============================================================================
  // METHODS
  //============================================================================
  /**
   * Creates a simple OR-composite-state with the given actions.
   * 
   * @throws StatechartException
   */
  public HierarchicalState(String name, Context parent, Action entryAction, Action doAction, Action exitAction) throws StatechartException {
    super(name, parent, entryAction, doAction, exitAction);

    if (parent instanceof ConcurrentState) {
      ((ConcurrentState)parent).addRegion(this);
    }
  }

  //============================================================================

  void storeHistory(Metadata data) {
    StateRuntimedata statedata = data.getData(this);
    if (history != null && statedata.currentState != startState && statedata.currentState != history && !(statedata.currentState instanceof FinalState)) {
      history.storeHistory(data);
    }
  }

  //============================================================================

  /**
   * Deactivates the state and informs the substates that they deactivate too.
   */
  @Override
  void deactivate(Metadata data) {
    if (!data.isActive(this)) {
      return;
    }

    StateRuntimedata statedata = data.getData(this);

    if (statedata == null) {
      super.deactivate(data);
      return;
    }

    /*
     * deactivate substate
     */
    if (statedata.currentState != null) {
      statedata.currentState.deactivate(data);
    }

    statedata.currentState = null;

    super.deactivate(data);
  }

  //============================================================================

  /**
   * Overrides the dispatch method from the state. It takes care of delegating
   * the incoming event to the current substate of this state and handles start
   * state end final state behavior.
   */
  @Override
  boolean dispatch(Metadata data, Event event) {
    if (!data.isActive(this)) {
      return false;
    }

    StateRuntimedata statedata = data.getData(this);

    // if no other substate is active (just after activation of this state) take
    // the start state if exists one
    if (statedata.currentState == null && startState != null) {
      // activate the start state
      data.activate(startState);
      // so the current substate of this state is set now (to the start state)
      statedata.currentState.activate(data);
    }

    // delegate event to the current active substate.
    if (statedata.currentState != null) {
      if (statedata.currentState.dispatch(data, event)) {
        // return if event triggered an transition and was consumed
        return true;
      }
    }

    // no substate handled the event, so try to find a transition on this state
    // which can do
    for (int i = 0; i < transitions.size(); i++) {
      Transition t = transitions.get(i);

      // completion transitions are only applicable if the current substate is
      // the final state
      if (statedata.currentState instanceof FinalState || t.hasEvent()) {
        // Otherwise try to trigger the transition with the event. */
        if (t.execute(event, data)) {
          return true;
        }
      }
    }
    return false;
  }
}
