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
package statechart.unittests;

import statechart.Action;
import statechart.Metadata;

public class TestAction implements Action {
  private String name;
  private String action;

  public TestAction(String name, String action) {
    this.name = name;
    this.action = action;
  }

  public void execute(Metadata data) {
    TestParameter parameter = ((TestParameter)data.getParameter());
    parameter.path +=
        (parameter.path.length() != 0 ? " " : "")
            + action + ":" + name;
  };
}
