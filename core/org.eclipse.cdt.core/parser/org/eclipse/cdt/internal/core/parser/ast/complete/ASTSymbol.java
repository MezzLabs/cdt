/**********************************************************************
 * Copyright (c) 2002,2003 Rational Software Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM Rational Software - Initial API and implementation
***********************************************************************/
package org.eclipse.cdt.internal.core.parser.ast.complete;

import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTScope;
import org.eclipse.cdt.internal.core.parser.pst.IContainerSymbol;
import org.eclipse.cdt.internal.core.parser.pst.ISymbol;
import org.eclipse.cdt.internal.core.parser.pst.ISymbolOwner;
import org.eclipse.cdt.internal.core.parser.pst.ParserSymbolTableError;
import org.eclipse.cdt.internal.core.parser.pst.TypeInfo;

/**
 * @author jcamelon
 *
 */
public abstract class ASTSymbol extends ASTSymbolOwner implements ISymbolOwner, IASTDeclaration
{
	
    public ASTSymbol( ISymbol symbol )
    {
        super(symbol);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.parser.ast.IASTScopedElement#getOwnerScope()
     */
    public IASTScope getOwnerScope()
    {
    	if( symbol.getContainingSymbol() != null )
    		return (IASTScope)symbol.getContainingSymbol().getASTExtension().getPrimaryDeclaration();
    	return null;
    }

    public IContainerSymbol getLookupQualificationSymbol() throws LookupError {
    	ISymbol sym = getSymbol();
		TypeInfo info = null;
		try{
			info = sym.getTypeInfo().getFinalType();
		} catch( ParserSymbolTableError e ){
			throw new LookupError();
		}
		
		if( info.isType( TypeInfo.t_type ) && info.getTypeSymbol() != null && info.getTypeSymbol() instanceof IContainerSymbol )
			return (IContainerSymbol) info.getTypeSymbol();
		else if( sym instanceof IContainerSymbol )
			return (IContainerSymbol) sym;
	
		return null;
    }
    
    public boolean shouldFilterLookupResult( ISymbol sym ){
    	TypeInfo info = null;
    	try{
			info = getSymbol().getTypeInfo().getFinalType();
		} catch( ParserSymbolTableError e ){
			return true;
		}
		
		if( info.checkBit( TypeInfo.isConst ) && !sym.getTypeInfo().checkBit( TypeInfo.isConst ) )
			return true;
		
		if( info.checkBit( TypeInfo.isVolatile ) && !sym.getTypeInfo().checkBit( TypeInfo.isVolatile ) )
			return true;
		
		return false;
		
    }
}
