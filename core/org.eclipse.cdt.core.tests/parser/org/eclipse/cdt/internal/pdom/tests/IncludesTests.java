/*******************************************************************************
 * Copyright (c) 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.pdom.tests;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.pdom.PDOM;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMFile;
import org.eclipse.core.runtime.IPath;

/**
 * @author Doug Schaefer
 *
 */
public class IncludesTests extends PDOMTestBase {

	protected ICProject project;
	
	protected void setUp() throws Exception {
		project = createProject("includesTests");
	}

	public void test1() throws Exception {
		PDOM pdom = (PDOM) CCorePlugin.getPDOMManager().getPDOM(project);
		IPath loc = project.getProject().getLocation().append("I2.h");
		PDOMFile file = pdom.getFile(loc.toOSString());
		assertNotNull(file);
		PDOMFile[] allIncludedBy = file.getAllIncludedBy();
		assertEquals(9, allIncludedBy.length); // i.e. all of them
	}

}
