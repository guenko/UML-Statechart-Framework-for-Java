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
 * This class defines the exceptions with their specific error codes which can
 * occur when working with the statechart.
 */
public class StatechartException extends Exception {
  private static final long serialVersionUID = 1L;

  /** List of possible error codes */
  public final static int PARENT_HAS_ALREADY_START_STATE = 1;
  public final static int PARENT_HAS_ALREADY_HISTORY_STATE = 2;
  public final static int PARENT_NOT_A_HIERACHICAL_STATE = 3;
  public final static int PARENT_NULL = 4;
  public final static int NAME_INVALID = 5;
  public final static int NAME_INVALID_CHARACTER = 6;
  public final static int NAME_NOT_UNIQUE = 7;
  public final static int NO_TOP_LEVEL_STATECHART = 8;
  public final static int TIMEOUT_NOT_GREATER_NULL = 9;
  public final static int MORE_THAN_ONE_TIMEOUT_TRANSITIONS = 10;

  /**
   * List of textual descriptions of the error code, used as reason for the
   * Exception
   */
  /* @formatter:off */
  private final static String reasons[] = { 
    "-- illegal / internal error --",
    "Parent has already a start state.",
    "Parent has already a history state.",
    "Parent is not a hierarchical state.",
    "Parameter parent cannot be null.",
    "Parameter name cannot be null or the empty string.",
    "Parameter name contains an invalid character.",
    "Parameter name already used. Please define a unique state name within the parent.",
    "Cannot determine path to the top level statechart. Check the hierarchy.",
    "Negative timeout value or 0 is not allowed, use a completion-transition (transition without an event).",
    "Only one Timeout transistion per state is supported.",
  };
  /* @formatter:on */

  private int errorCode = 0;

  public StatechartException(int errorCode) {
    super(reasons[checkErrorCode(errorCode)]);
    this.errorCode = errorCode;
  }

  private static int checkErrorCode(int errorCode) {
    return errorCode < 0 ? 0 : errorCode >= reasons.length ? 0 : errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }
}
