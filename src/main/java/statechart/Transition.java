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

import java.util.Vector;
import org.apache.log4j.Logger;
import statechart.timeout.TimerHandle;

/**
 * Describes a Transition between states.
 */
public class Transition {

  private static Logger log = Logger.getLogger(Transition.class);

  //============================================================================
  // ATTRIBUTES
  //============================================================================
  // The triggering event or 0 if no event is used.
  Event event = null;

  // The guard watching if the transition can trigger.
  Guard guard = null;

  // The action to execute when the transition triggers.
  Action action = null;

  // List of all states which must be deactivated when triggering.
  Vector<State> deactivate = new Vector<State>();

  // List of all states which must be activated when triggering.
  Vector<State> activate = new Vector<State>();

  //============================================================================
  // METHODS
  //============================================================================
  public Transition(State start, State end) throws StatechartException {
    init(start, end, null, null, null);
  }

  //============================================================================

  public Transition(State start, State end, Event event) throws StatechartException {
    init(start, end, event, null, null);
  }

  //============================================================================

  public Transition(State start, State end, Guard guard) throws StatechartException {
    init(start, end, null, guard, null);
  }

  //============================================================================

  public Transition(State start, State end, Action action) throws StatechartException {
    init(start, end, null, null, action);
  }

  //============================================================================

  public Transition(State start, State end, Event event, Guard guard) throws StatechartException {
    init(start, end, event, guard, null);
  }

  //============================================================================

  public Transition(State start, State end, Event event, Action action) throws StatechartException {
    init(start, end, event, null, action);
  }

  //============================================================================

  public Transition(State start, State end, Guard guard, Action action) throws StatechartException {
    init(start, end, null, guard, action);
  }

  //============================================================================

  public Transition(State start, State end, Event event, Guard guard, Action action) throws StatechartException {
    init(start, end, event, guard, action);
  }

  //============================================================================

  /**
   * Checks whether an event is associated with this transition or not.
   */
  boolean hasEvent() {
    return event != null;
  };

  //============================================================================

  /**
   * Checks whether a guard is associated with this transition or not.
   */
  boolean hasGuard() {
    return guard != null;
  };

  //============================================================================

  /**
   * Checks whether an action is associated with this transition or not.
   */
  boolean hasAction() {
    return action != null;
  };

  //============================================================================

  /**
   * Executes the transition and triggers the new step.
   */
  boolean execute(Event event, Metadata data) {

    /** check if the event has to be handled */

    // return if event at transition exists and does not match with event to
    // handle
    if (this.event != null && !this.event.equals(event)) {
      return false;
    }

    // return if event at transition exists but event to handle is null
    if (this.event != null && event == null) {
      return false;
    }

    /* if an timeout occurred raised by an elapsed timer */
    if (event instanceof TimeoutOccurrenceEvent) {
      TimerHandle timerHandle = (TimeoutOccurrenceEvent)event;

      State startingState = ((TimeoutOccurrenceEvent)event).getStartingState();
      // if this transition is in the set of transition of the TimeoutEvent
      // starting state
      if (!startingState.transitions.contains(this)) {
        // is not the transition of this TimeoutEvent we are handling here
        return false;
      }

      // this is the starting state of the Timeout transition
      log.trace("lookup TimerHandle at transistion starting state: " + startingState.toString());
      StateRuntimedata runtimedata = data.getData(startingState);
      if (runtimedata.timerHandle != timerHandle) {
        // the timeout handle is not set so timer was canceled in between, so do
        // not trigger this transition
        log.trace("TimerHandle not found: " + Integer.toHexString(timerHandle.hashCode()) + " -> ignore transition");
        return false;
      }
      // found the timerHandle being set at timer start, so this timeout
      // occurrence
      // is still valid (timer was not canceled until now)
      log.trace("TimerHandle found: " + Integer.toHexString(timerHandle.hashCode()));
      // go further on with event processing, so this event and the timer handle
      // is treaded as consumed
      runtimedata.timerHandle = null;
    }

    // check guards
    if (!allowed(data)) {
      return false;
    }

    /** now perform the transition */

    // check if saving the active states for history is needed and do so if
    // necessary
    for (int i = 0; i < deactivate.size(); i++) {
      State state = deactivate.get(i);
      if (state instanceof HierarchicalState && ((HierarchicalState)state).history != null) {
        // this hierarchical state has a history state, so save the history
        // before deactivation of the sub states
        ((HierarchicalState)state).storeHistory(data);
      }
    }

    // deactivate all states
    for (int i = 0; i < deactivate.size(); i++) {
      deactivate.get(i).deactivate(data);
    }

    // Execute exit-action
    if (action != null) {
      action.execute(data);
    }

    // Activate all new states.
    for (int i = 0; i < activate.size(); i++) {
      /*
       * check if we activate an concurrent state imlicit and if so make sure
       * adding the correct region to the list of regions to ignore on
       * activation. It is activated by this transition.
       */
      if (i + 1 < activate.size() && activate.get(i) instanceof ConcurrentState) {
        ConcurrentState s = (ConcurrentState)activate.get(i);
        StateRuntimedata cd = data.createRuntimedata(s);

        if (!cd.stateset.contains(activate.get(i + 1))) {
          cd.stateset.add(activate.get(i + 1));
        }
      }
      activate.get(i).activate(data);
    }
    return true;
  }

  //============================================================================

  /**
   * Checks if all constraints are fulfilled. To do this the whole path up to
   * the next real state is checked.
   */
  boolean allowed(Metadata data) {
    if (guard != null && !guard.check(data)) {
      return false;
    }

    /*
     * if target is a pseudostate, call lookup to check if we do not stay in
     * this state. So get the last state in the activate list. end() is behind
     * the last element so we have to select the prior element by using *--.
     */
    State target = (State)activate.lastElement();
    if (target instanceof PseudoState) {
      return ((PseudoState)target).lookup(data);
    }

    return true;
  }

  //============================================================================

  /**
   * Initializes the Transition
   * 
   * @throws StatechartException
   */
  private void init(State start, State end, Event event, Guard guard, Action action) throws StatechartException {
    this.event = event;
    this.guard = guard;
    this.action = action;

    // Check if there exists already another timeout transition (transition with
    // an TimeoutEvent)
    // starting from this state.
    // Since we store the timeout handle at the state the Timeout transition
    // starts from,
    // we would not be able to assign a timeout occurrence to the right Timeout
    // transition,
    // if there would be more than one starting from the same state.
    if (event instanceof TimeoutEvent) {
      if (((TimeoutEvent)event).getTimout() <= 0) {
        throw new StatechartException(StatechartException.TIMEOUT_NOT_GREATER_NULL);
      }
      for (Transition transition : start.transitions) {
        if (transition.event instanceof TimeoutEvent) {
          throw new StatechartException(StatechartException.MORE_THAN_ONE_TIMEOUT_TRANSITIONS);
        }
      }
    }

    Transition.calculateStateSet(start, end, deactivate, activate);
    start.addTransition(this);

    // for handling join states correctly, we need to know the incoming
    // transition
    if (end instanceof PseudoState && ((PseudoState)end).getType() == PseudoState.pseudostate_join) {
      ((PseudoState)end).addIncomingTransition(this);
    }
  }

  //============================================================================

  /**
   * Calculates all the states which must be deactivated and then activated when
   * triggering the transition.
   */
  private static void calculateStateSet(State start, State end, Vector<State> deactivate, Vector<State> activate) {
    // temp vectors for calculating the LCA (least common ancestor)
    Vector<State> a = new Vector<State>();
    Vector<State> d = new Vector<State>();

    // get all states for possible deactivation
    State s = start;
    while (s != null) {
      d.add(0, s);
      Context context = s.parent;

      // If context is hierarchical or concurrent state, get it as parent.
      if (context != null && !(context instanceof Statechart)) {
        s = context;
      } else {
        s = null;
      }
    }

    // get all states for possible activation
    State e = end;
    while (e != null) {
      a.add(0, e);
      Context context = e.parent;

      // If context is hierarchical or concurrent state, get it as parent.
      if (context != null && !(context instanceof Statechart)) {
        e = context;
      } else {
        e = null;
      }
    }

    /*
     * Get LCA number. It is min-1 by default. Therefore we make sure that if
     * start equals end, we do not get the whole path up to the root node if the
     * state is a substate.
     */
    int min = a.size() < d.size() ? a.size() : d.size();
    int lca = min - 1;

    // get the LCA-State
    if (start != end) {
      // if the first entry is not equal we got the LCA
      for (lca = 0; lca < min; lca++) {
        if (a.get(lca) != d.get(lca)) {
          break;
        }
      }
    }

    // Fill the given vectors for the transition
    for (int j = lca; j < d.size(); j++) {
      deactivate.add(0, d.get(j));
    }

    for (int j = lca; j < a.size(); j++) {
      activate.add(a.get(j));
    }
  }
}
