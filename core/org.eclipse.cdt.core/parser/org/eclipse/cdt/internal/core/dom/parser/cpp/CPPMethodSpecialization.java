/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Niefer (IBM) - Initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *     Sergey Prigogin (Google) 
 *     Thomas Corbat (IFS)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunctionType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethodSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateParameterMap;

/**
 * The specialization of a method in the context of a class-specialization.
 */
public class CPPMethodSpecialization extends CPPFunctionSpecialization implements ICPPMethodSpecialization {

	public CPPMethodSpecialization(ICPPMethod orig, ICPPClassType owner, ICPPTemplateParameterMap argMap,
			ICPPFunctionType type, IType[] exceptionSpec) {
		super(orig, owner, argMap, type, exceptionSpec);
	}

	@Override
	public ICPPMethod getSpecializedBinding() {
		return (ICPPMethod) super.getSpecializedBinding();
	}

	@Override
	public boolean isVirtual() {
		ICPPMethod f = getSpecializedBinding();
		if (f != null)
			return f.isVirtual();
		IASTNode definition = getDefinition();
		if (definition != null) {
			IASTNode node = definition.getParent();
			while(node instanceof IASTDeclarator)
				node = node.getParent();
			
			ICPPASTDeclSpecifier declSpec = null;
			if (node instanceof IASTSimpleDeclaration) {
				declSpec = (ICPPASTDeclSpecifier) ((IASTSimpleDeclaration) node).getDeclSpecifier();
			} else if (node instanceof IASTFunctionDefinition) {
				declSpec = (ICPPASTDeclSpecifier) ((IASTFunctionDefinition) node).getDeclSpecifier();
			}
			
			if (declSpec != null) {
				return declSpec.isVirtual();
			}
		}
		return false;
	}

	@Override
	public int getVisibility() {
		ICPPMethod f = getSpecializedBinding();
		if (f != null)
			return f.getVisibility();
		return 0;
	}

	@Override
	public ICPPClassType getClassOwner() {
		return (ICPPClassType) getOwner();
	}
	
	@Override
	public boolean isDestructor() {
		char[] name = getNameCharArray();
		if (name.length > 1 && name[0] == '~')
			return true;

		return false;
	}

	@Override
	public boolean isExplicit() {
		return getSpecializedBinding().isExplicit();
	}

	@Override
	public boolean isImplicit() {
		return getSpecializedBinding().isImplicit();
	}

	@Override
	public boolean isPureVirtual() {
		ICPPMethod f = getSpecializedBinding();
		if (f != null)
			return f.isPureVirtual();

		return false;
	}

	@Override
	public boolean isOverride() {
		return false;
	}

	@Override
	public boolean isFinal() {
		return false;
	}

	@Override
	public IType[] getExceptionSpecification(IASTNode point) {
		if (isImplicit()) {
			return ClassTypeHelper.getInheritedExceptionSpecification(this, point);
		}
		return super.getExceptionSpecification();
	}
}
