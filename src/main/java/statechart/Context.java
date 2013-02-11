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

/**
 * The base class of a state holding substates, used to set up the state
 * hierarchy.
 */
public abstract class Context extends State {

  /**
   * the start start within this context (e.g. hierarchical state, concurrent
   * state, state chart)
   */
  protected PseudoState startState = null;
  /**
   * All the substates this state (context) has. Used to check the name
   * uniqueness about substates and to retrieve a state by its state path.
   */
  private ArrayList<State> substates = new ArrayList<State>();

  /**
   * @see statechart.State#State
   */
  public Context(String name, Context parent, Action entryAction, Action doAction, Action exitAction) throws StatechartException {
    super(name, parent, entryAction, doAction, exitAction);
  }

  /**
   * Retrieves the substate which has the given name
   * 
   * @param name the name of the substate searched for
   * @return the substate or null if this state has not a substate holding this
   *         name
   */
  public State getSubstate(String name) {
    for (State state : substates) {
      if (name.equals(state.name)) {
        return state;
      }
    }
    return null;
  }

  /**
   * The given state is a substate of this state.
   * 
   * @param state the new substate added
   */
  public void addSubstate(State state) {
    substates.add(state);
  }

  @Override
  public State getState(String path) {
    int firstDelimiter = path.indexOf(Statechart.STATE_PATH_DELIMITER);
    if (firstDelimiter == -1) {
      return getSubstate(path);
    } else {
      State substate = getSubstate(path.substring(0, firstDelimiter));
      if (substate == null) {
        return null;
      }
      return substate.getState(path.substring(firstDelimiter + 1));
    }
  }
}
