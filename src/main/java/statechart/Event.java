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
 * Interface for an event for the statechart.
 */
public abstract class Event {
  /**
   * The name of the event used to identify the event and to distinguish it from
   * other events
   */
  private String id = new String();

  /**
   * Creates an event with a given id
   * 
   * @param id the event's unique name
   */
  public Event(String id) {
    this.id = id;
  };

  /**
   * Check if this event is the same event as the given one. E.g. called by the
   * transition handling to check if it should handle the given event.
   * 
   * @param event the event to compare
   * @param data the runtime data object
   */
  public boolean equals(Event event) {
    return this.id.equals(event != null ? event.id : null);
  };

  /**
   * The string representation of the event, her the unique name of the event
   */
  @Override
  public String toString() {
    return id;
  }
}
