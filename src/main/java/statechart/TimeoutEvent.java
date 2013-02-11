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
 * This represents the static timeout event assigned to a transition of the
 * statechart. Whereas an timeout instance (timeout occurrence) is represented
 * by an object of the TimeoutOccurrenceEvent class.
 */
public class TimeoutEvent extends Event {

  /** unique name of the timeout event */
  static public final String TIMEOUT_EVENT_NAME = "TimeoutEvent";
  /** The timeout value in milliseconds */
  private long timeout;

  /**
   * Creates a timeout event to be attached to a transition.
   * 
   * @param timeout The timeout value in milliseconds
   */
  public TimeoutEvent(long timeout) {
    super(TIMEOUT_EVENT_NAME);
    this.timeout = timeout;
  }

  /**
   * Get the timeout value of the event.
   */
  public long getTimout() {
    return timeout;
  }
}
