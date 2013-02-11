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
package statechart.legacy.timeout;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import statechart.Metadata;
import statechart.State;
import statechart.Statechart;
import statechart.TimeoutEvent;

/**
 * The queue entry for a timeout. The execution is delayed for a specific amount
 * of milliseconds.
 * 
 * @Note This class has a natural ordering that is inconsistent with equals.
 */
class QueueTimerEntry implements Runnable, Delayed {

  private static AtomicInteger idCounter = new AtomicInteger(1);

  private Statechart statechart;
  private State state;
  private Metadata data;
  private TimeoutEvent event;
  /** all time values are given in ms */
  private long relativeTimeout;
  private long absoluteTimeout;
  private long added;
  private Integer id;
  volatile boolean invalid = false;

  public QueueTimerEntry(Statechart statechart, State state, Metadata data, TimeoutEvent event) {
    this.statechart = statechart;
    this.state = state;
    this.data = data;
    this.event = event;
    this.relativeTimeout = event.getTimout();
    this.added = System.currentTimeMillis();
    this.absoluteTimeout = this.added + relativeTimeout;
    this.id = idCounter.getAndIncrement();
  }

  /**
   * Generates an null event, just to shutdown the Timeoutmanager
   */
  public QueueTimerEntry() {
  }

  @Override
  public void run() {
    // if the state this entry belongs to is not active anymore ignore it
    if (!invalid && data.isActive(state)) {
      statechart.dispatch(data, event);
    }
    invalid = true;
    statechart = null;
    state = null;
    data = null;
    event = null;
  }

  @Override
  public long getDelay(TimeUnit sourceUnit) {
    long currentTime = System.currentTimeMillis();
    long duration = absoluteTimeout - currentTime;
    return sourceUnit.convert(duration, TimeUnit.MILLISECONDS);
    // return duration <= 0 ? 0 : sourceUnit.convert(duration,
    // TimeUnit.MILLISECONDS);
  }

  /*
   * FIXME The behavior of this method is a bit strange in order to handle a bug
   * in JDK 5. If used in JDK 6 or newer this method can be changed so that it
   * only compares the absoluteTimeout.
   */
  @Override
  public int compareTo(Delayed d) {
    if (d instanceof QueueTimerEntry) {
      QueueTimerEntry entry = (QueueTimerEntry)d;
      if (this == entry) {
        return 0;
      } else if (this.absoluteTimeout < entry.absoluteTimeout) {
        return -1;
      } else if (this.absoluteTimeout > entry.absoluteTimeout) {
        return 1;
      } else {
        return this.id.compareTo(entry.id);
      }
    } else {
      throw new ClassCastException("Cannot compare an object of " + this.getClass() + " to an object of " + d.getClass());
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DelayQueueTimeoutEntry [id=");
    builder.append(id);
    builder.append(", added=");
    builder.append(added);
    builder.append(", data=(");
    builder.append(data);
    builder.append("), event=");
    builder.append(event);
    builder.append(", invalid=");
    builder.append(invalid);
    builder.append(", state=");
    builder.append(state);
    builder.append(", statechart=");
    builder.append(statechart);
    builder.append(", relativeTimeout=");
    builder.append(relativeTimeout);
    builder.append(", absoluteTimeout=");
    builder.append(absoluteTimeout);
    builder.append("]");
    return builder.toString();
  }
}
