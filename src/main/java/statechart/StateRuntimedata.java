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
import statechart.timeout.TimerHandle;

/**
 * Holds the runtime specific data for a certain state in the statechart.
 */
public class StateRuntimedata {
  //============================================================================
  // ATTRIBUTES
  //============================================================================
  /**
   * The current time the state is activated
   */
  public long currentTime = 0;

  /**
   * If the state is active
   */
  public boolean active = false;

  /**
   * The current activated substate
   */
  public State currentState = null;

  /**
   * A generic set of states, needed e.g. for history storage or region
   * activation
   */
  public Vector<State> stateset = new Vector<State>();

  /**
   * The set of timeout's currently active for the state
   */
  public TimerHandle timerHandle = null;

  //============================================================================
  // Methods
  //============================================================================
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("StateRuntimedata [active=");
    builder.append(active);
    builder.append(", currentState=");
    builder.append(currentState);
    builder.append(", currentTime=");
    builder.append(currentTime);
    builder.append(", stateset=");
    builder.append(stateset);
    builder.append(", timerHandle=");
    builder.append(timerHandle);
    builder.append("]");
    return builder.toString();
  }
}
