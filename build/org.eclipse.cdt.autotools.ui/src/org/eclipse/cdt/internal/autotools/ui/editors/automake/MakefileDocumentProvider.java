/*******************************************************************************
 * Copyright (c) 2000, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Anton Leherbauer (Wind River Systems) - Fixed bug 141295
 *******************************************************************************/
package org.eclipse.cdt.internal.autotools.ui.editors.automake;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class MakefileDocumentProvider extends TextFileDocumentProvider implements IMakefileDocumentProvider {
	
	IMakefile fMakefile;

	protected class MakefileAnnotationModel extends AnnotationModel /*implements IProblemRequestor */{
		/**
		 * @param resource
		 */
		public MakefileAnnotationModel(IResource resource) {
			super();
		}

		/**
		 * @param makefile
		 */
		public void setMakefile(IMakefile makefile) {
			fMakefile = makefile;
		}
	}

	/**
	 * Remembers a IMakefile for each element.
	 */
	protected class MakefileFileInfo extends FileInfo {		
		public IMakefile fCopy;
	}

	public MakefileDocumentProvider() {
		IDocumentProvider provider= new TextFileDocumentProvider(new MakefileStorageDocumentProvider());
		provider= new ForwardingDocumentProvider(MakefileDocumentSetupParticipant.MAKEFILE_PARTITIONING, new MakefileDocumentSetupParticipant(), provider);
		setParentDocumentProvider(provider);
	}

	/**
	 */
	private IMakefile createMakefile(IFile file) throws CoreException {
		if (file.exists()) {
			return GNUAutomakefile.createMakefile(file);
		}
		return null;
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createAnnotationModel(org.eclipse.core.resources.IFile)
     */
    protected IAnnotationModel createAnnotationModel(IFile file) {
    	return new MakefileAnnotationModel(file);
    }

    /*
	 * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createFileInfo(java.lang.Object)
	 */
	protected FileInfo createFileInfo(Object element) throws CoreException {
		if (!(element instanceof IFileEditorInput))
			return null;
			
		IFileEditorInput input= (IFileEditorInput) element;
		IMakefile original= createMakefile(input.getFile());
		if (original == null)
			return null;

		FileInfo info= super.createFileInfo(element);
		if (!(info instanceof MakefileFileInfo)) {
			return null;
		}
	
		MakefileFileInfo makefileInfo= (MakefileFileInfo) info;
		setUpSynchronization(makefileInfo);

		makefileInfo.fCopy = original;

		if (makefileInfo.fModel instanceof MakefileAnnotationModel)   {
			MakefileAnnotationModel model= (MakefileAnnotationModel) makefileInfo.fModel;
			model.setMakefile(makefileInfo.fCopy);
		} 		
		return makefileInfo;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#disposeFileInfo(java.lang.Object, org.eclipse.ui.editors.text.TextFileDocumentProvider.FileInfo)
     */
    protected void disposeFileInfo(Object element, FileInfo info) {
	    if (info instanceof MakefileFileInfo) {
		    MakefileFileInfo makefileInfo= (MakefileFileInfo) info;
		    if (makefileInfo.fCopy != null) {
		    	makefileInfo.fCopy = null;
		    }
	    }
	    super.disposeFileInfo(element, info);	
    }
    
    /*
	 * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createEmptyFileInfo()
	 */
	protected FileInfo createEmptyFileInfo() {
		return new MakefileFileInfo();
	}
	
	/*
	 * @see org.eclipse.cdt.internal.autotools.ui.editors.automake.IMakefileDocumentProvider#getWorkingCopy(java.lang.Object)
	 */
	public IMakefile getWorkingCopy(Object element) {
		FileInfo fileInfo= getFileInfo(element);		
		if (fileInfo instanceof MakefileFileInfo) {
			MakefileFileInfo info= (MakefileFileInfo) fileInfo;
			return info.fCopy;
		}
		return null;
	}

	/*
	 * @see org.eclipse.cdt.internal.autotools.ui.editors.automake.MakefileDocumentProvider#shutdown()
	 */
	public void shutdown() {
		@SuppressWarnings("unchecked")
		Iterator e= getConnectedElementsIterator();
		while (e.hasNext())
			disconnect(e.next());
	}
	
}
