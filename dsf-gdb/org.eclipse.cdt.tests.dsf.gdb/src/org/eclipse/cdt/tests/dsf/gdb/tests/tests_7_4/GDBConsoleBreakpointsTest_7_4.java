/*******************************************************************************
 * Copyright (c) 2012 Mentor Graphics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Mentor Graphics - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.tests.dsf.gdb.tests.tests_7_4;

import org.eclipse.cdt.tests.dsf.gdb.tests.GDBConsoleBreakpointsTest;
import org.eclipse.cdt.tests.dsf.gdb.tests.ITestConstants;

public class GDBConsoleBreakpointsTest_7_4 extends GDBConsoleBreakpointsTest {
    @Override
	protected void setGdbVersion() {
		setGdbProgramNamesLaunchAttributes(ITestConstants.SUFFIX_GDB_7_4);		
	}
}
