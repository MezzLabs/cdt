/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */
package org.eclipse.cdt.debug.mi.core.cdi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager;
import org.eclipse.cdt.debug.core.cdi.ICDICatchEvent;
import org.eclipse.cdt.debug.core.cdi.ICDICondition;
import org.eclipse.cdt.debug.core.cdi.ICDILocation;
import org.eclipse.cdt.debug.core.cdi.model.ICDIBreakpoint;
import org.eclipse.cdt.debug.core.cdi.model.ICDICatchpoint;
import org.eclipse.cdt.debug.core.cdi.model.ICDILocationBreakpoint;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.core.cdi.model.ICDIWatchpoint;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.model.Breakpoint;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
import org.eclipse.cdt.debug.mi.core.cdi.model.Watchpoint;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MIBreakAfter;
import org.eclipse.cdt.debug.mi.core.command.MIBreakCondition;
import org.eclipse.cdt.debug.mi.core.command.MIBreakDelete;
import org.eclipse.cdt.debug.mi.core.command.MIBreakDisable;
import org.eclipse.cdt.debug.mi.core.command.MIBreakEnable;
import org.eclipse.cdt.debug.mi.core.command.MIBreakInsert;
import org.eclipse.cdt.debug.mi.core.command.MIBreakList;
import org.eclipse.cdt.debug.mi.core.command.MIBreakWatch;
import org.eclipse.cdt.debug.mi.core.event.MIBreakpointChangedEvent;
import org.eclipse.cdt.debug.mi.core.event.MIBreakpointCreatedEvent;
import org.eclipse.cdt.debug.mi.core.event.MIBreakpointDeletedEvent;
import org.eclipse.cdt.debug.mi.core.event.MIEvent;
import org.eclipse.cdt.debug.mi.core.output.MIBreakInsertInfo;
import org.eclipse.cdt.debug.mi.core.output.MIBreakListInfo;
import org.eclipse.cdt.debug.mi.core.output.MIBreakWatchInfo;
import org.eclipse.cdt.debug.mi.core.output.MIBreakpoint;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;

/**
 * Breakpoint Manager for the CDI interface.
 */
public class BreakpointManager extends SessionObject implements ICDIBreakpointManager {

	List breakList;
	boolean allowInterrupt;
	boolean autoupdate;

	public BreakpointManager(Session session) {
		super(session);
		breakList = Collections.synchronizedList(new ArrayList());
		allowInterrupt = true;
		autoupdate = false;
	}

	public MIBreakpoint[] getMIBreakpoints() throws CDIException {
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();
		MIBreakList breakpointList = factory.createMIBreakList();
		try {
			s.getMISession().postCommand(breakpointList);
			MIBreakListInfo info = breakpointList.getMIBreakListInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
			return info.getMIBreakpoints();
		} catch (MIException e) {
			throw new MI2CDIException(e);
		}
	}

	boolean containsBreakpoint(int number) {
		return (getBreakpoint(number) != null);
	}

	boolean hasBreakpointChanged(MIBreakpoint miBreakpoint) {
		boolean changed = false;
		int no = miBreakpoint.getNumber();
		Breakpoint point = getBreakpoint(no);
		if (point != null) {
			MIBreakpoint miBreak = point.getMIBreakpoint();
			changed = (miBreak.isEnabled() != miBreakpoint.isEnabled()) ||
				!miBreak.getCondition().equals(miBreakpoint.getCondition()) ||
				(miBreak.getIgnoreCount() != miBreakpoint.getIgnoreCount());
		}
		return changed;
	}

	public Breakpoint getBreakpoint(int number) {
		ICDIBreakpoint[] bkpts = listBreakpoints();
		for (int i = 0; i < bkpts.length; i++) {
			if (bkpts[i] instanceof Breakpoint) {
				Breakpoint point = (Breakpoint) bkpts[i];
				MIBreakpoint miBreak = point.getMIBreakpoint();
				if (miBreak.getNumber() == number) {
					return point;
				}
			}
		}
		return null;
	}

	Watchpoint getWatchpoint(int number) {
		return (Watchpoint)getBreakpoint(number);
	}

	Breakpoint[] listBreakpoints() {
		return (Breakpoint[]) breakList.toArray(new Breakpoint[0]);
	}

	boolean suspendInferior() throws CDIException {
		boolean shouldRestart = false;
		Session s = (Session)getSession();
		ICDITarget currentTarget = s.getCurrentTarget();
		// Stop the program and disable events.
		if (currentTarget instanceof Target) {
			Target target = (Target)currentTarget;
			if (target.isRunning() && allowInterrupt) {
				int lastToken = target.getLastExecutionToken();
				shouldRestart = true;
				((EventManager)s.getEventManager()).disableEventToken(lastToken);
				target.suspend();
				((EventManager)s.getEventManager()).enableEventToken(lastToken);
			}
		}
		return shouldRestart;
	}

	void resumeInferior(boolean shouldRestart) throws CDIException {
		if (shouldRestart) {
			Session s = (Session)getSession();
			ICDITarget target = s.getCurrentTarget();
			target.resume();
		}
	}

	public void deleteBreakpoint (int no) {
		Breakpoint[] points = listBreakpoints();
		for (int i = 0; i < points.length; i++) {
			if (points[i].getMIBreakpoint().getNumber() == no) {
				breakList.remove(points[i]);
				break;
			}
		}
	}

	public void enableBreakpoint(ICDIBreakpoint breakpoint) throws CDIException {
		int number = 0;
		if (breakpoint instanceof Breakpoint
			&& breakList.contains(breakpoint)) {
			number = ((Breakpoint) breakpoint).getMIBreakpoint().getNumber();
		} else {
			throw new CDIException("Not a CDT breakpoint");
		}
		boolean state = suspendInferior();
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();
		MIBreakEnable breakEnable = factory.createMIBreakEnable(new int[] { number });
		try {
			s.getMISession().postCommand(breakEnable);
			MIInfo info = breakEnable.getMIInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		} finally {
			// Resume the program and enable events.
			resumeInferior(state);
		}
		((Breakpoint) breakpoint).getMIBreakpoint().setEnabled(true);
		// Fire a changed Event.
		MISession mi = s.getMISession();
		mi.fireEvent(new MIBreakpointChangedEvent(((Breakpoint)breakpoint).getMIBreakpoint().getNumber()));
	}

	public void disableBreakpoint(ICDIBreakpoint breakpoint) throws CDIException {
		int number = 0;
		if (breakpoint instanceof Breakpoint
			&& breakList.contains(breakpoint)) {
			number = ((Breakpoint) breakpoint).getMIBreakpoint().getNumber();
		} else {
			throw new CDIException("Not a CDT breakpoint");
		}
		boolean state = suspendInferior();
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();
		MIBreakDisable breakDisable =
			factory.createMIBreakDisable(new int[] { number });
		try {
			s.getMISession().postCommand(breakDisable);
			MIInfo info = breakDisable.getMIInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		} finally {
			resumeInferior(state);
		}
		((Breakpoint) breakpoint).getMIBreakpoint().setEnabled(false);
		// Fire a changed Event.
		MISession mi = s.getMISession();
		mi.fireEvent(new MIBreakpointChangedEvent(((Breakpoint)breakpoint).getMIBreakpoint().getNumber()));
	}

	public void setCondition(ICDIBreakpoint breakpoint, ICDICondition condition) throws CDIException {
		int number = 0;
		if (breakpoint instanceof Breakpoint
			&& breakList.contains(breakpoint)) {
			number = ((Breakpoint) breakpoint).getMIBreakpoint().getNumber();
		} else {
			throw new CDIException("Not a CDT breakpoint");
		}

		boolean state = suspendInferior();
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();

		// reset the values to sane states.
		String exprCond = condition.getExpression();
		if (exprCond == null) {
			exprCond = "";
		}
		int ignoreCount = condition.getIgnoreCount();
		if (ignoreCount < 0) { 
			ignoreCount = 0;
		}

		try {
			MIBreakCondition breakCondition =
				factory.createMIBreakCondition(number, exprCond);
			s.getMISession().postCommand(breakCondition);
			MIInfo info = breakCondition.getMIInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
			MIBreakAfter breakAfter =
				factory.createMIBreakAfter(number, ignoreCount);
			s.getMISession().postCommand(breakAfter);
			info = breakAfter.getMIInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		} finally {
			resumeInferior(state);
		}
		// Fire a changed Event.
		MISession mi = s.getMISession();
		mi.fireEvent(new MIBreakpointChangedEvent(((Breakpoint)breakpoint).getMIBreakpoint().getNumber()));
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#update()
	 */
	public void update() throws CDIException {
		MIBreakpoint[] newMIBreakpoints = getMIBreakpoints();
		List eventList = new ArrayList(newMIBreakpoints.length);
		for (int i = 0; i < newMIBreakpoints.length; i++) {
			int no = newMIBreakpoints[i].getNumber();
			if (containsBreakpoint(no)) {
				if (hasBreakpointChanged(newMIBreakpoints[i])) {
					// Fire ChangedEvent
					eventList.add(new MIBreakpointChangedEvent(no)); 
				}
			} else {
				// add the new breakpoint and fire CreatedEvent
				if (newMIBreakpoints[i].isWatchpoint()) {
					breakList.add(new Watchpoint(this, newMIBreakpoints[i]));
				} else {
					breakList.add(new Breakpoint(this, newMIBreakpoints[i]));
				}
				eventList.add(new MIBreakpointCreatedEvent(no)); 
			}
		}
		// Check if any breakpoint was removed.
		Breakpoint[] oldBreakpoints = listBreakpoints();
		for (int i = 0; i < oldBreakpoints.length; i++) {
			boolean found = false;
			int no = oldBreakpoints[i].getMIBreakpoint().getNumber();
			for (int j = 0; j < newMIBreakpoints.length; j++) {
				if (no == newMIBreakpoints[j].getNumber()) {
					found = true;
					break;
				}
			}
			if (!found) {
				// Fire destroyed Events.
				eventList.add(new MIBreakpointDeletedEvent(no)); 
			}
		}
		MISession mi = ((Session)getSession()).getMISession();
		MIEvent[] events = (MIEvent[])eventList.toArray(new MIEvent[0]);
		mi.fireEvents(events);
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#allowProgramInterruption()
	 */
	public void allowProgramInterruption(boolean e) {
		allowInterrupt = e;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#deleteAllBreakpoints()
	 */
	public void deleteAllBreakpoints() throws CDIException {
		deleteBreakpoints(listBreakpoints());
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#deleteBreakpoint(ICDIBreakpoint)
	 */
	public void deleteBreakpoint(ICDIBreakpoint breakpoint) throws CDIException {
		deleteBreakpoints(new ICDIBreakpoint[] { breakpoint });
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#deleteBreakpoints(ICDIBreakpoint[])
	 */
	public void deleteBreakpoints(ICDIBreakpoint[] breakpoints) throws CDIException {
		int[] numbers = new int[breakpoints.length];
		for (int i = 0; i < numbers.length; i++) {
			if (breakpoints[i] instanceof Breakpoint
				&& breakList.contains(breakpoints[i])) {
				numbers[i] =
					((Breakpoint) breakpoints[i]).getMIBreakpoint().getNumber();
			} else {
				throw new CDIException("Not a CDT breakpoint");
			}
		}
		boolean state = suspendInferior();
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();
		MIBreakDelete breakDelete = factory.createMIBreakDelete(numbers);
		try {
			s.getMISession().postCommand(breakDelete);
			MIInfo info = breakDelete.getMIInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		} finally {
			resumeInferior(state);
		}
		List eventList = new ArrayList(breakpoints.length);
		for (int i = 0; i < breakpoints.length; i++) {
			int no = ((Breakpoint)breakpoints[i]).getMIBreakpoint().getNumber();
			eventList.add(new MIBreakpointDeletedEvent(no));
		}
		MISession mi = s.getMISession();
		MIEvent[] events = (MIEvent[])eventList.toArray(new MIEvent[0]);
		mi.fireEvents(events);
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#getBreakpoints()
	 */
	public ICDIBreakpoint[] getBreakpoints() throws CDIException {
		update();
		return (ICDIBreakpoint[]) breakList.toArray(new ICDIBreakpoint[0]);
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#getBreakpoints()
	 */
//	public ICDIBreakpoint[] getBreakpoints() throws CDIException {
//		Session s = getCSession();
//		CommandFactory factory = s.getMISession().getCommandFactory();
//		MIBreakList breakpointList = factory.createMIBreakList();
//		try {
//			s.getMISession().postCommand(breakpointList);
//			MIBreakListInfo info = breakpointList.getMIBreakListInfo();
//			if (info == null) {
//				throw new CDIException("No answer");
//			}
//			MIBreakpoint[] mipoints = info.getBreakpoints();
//			for (int i = 0; i < miPoints.length; i++) {
//				if (!containsBreakpoint(miPoints[i].getNumber())) {
//					// FIXME: Generate a Create/Change Event??
//					breakList.add(new Breakpoint(this, miPoints[i]));
//				}
//			}
//			// FIXME: Generate a DestroyEvent for deleted ones.
//		} catch (MIException e) {
//			throw new MI2CDIException(e);
//		}
//		return (ICDIBreakpoint[]) listBreakpoints();
//	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#setCatchpoint(int, ICDICatchEvent, String, ICDICondition, boolean)
	 */
	public ICDICatchpoint setCatchpoint( int type, ICDICatchEvent event, String expression,
		ICDICondition condition) throws CDIException {
		throw new CDIException("Not Supported");
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#setLocationBreakpoint(int, ICDILocation, ICDICondition, boolean, String)
	 */
	public ICDILocationBreakpoint setLocationBreakpoint( int type, ICDILocation location,
		ICDICondition condition, String threadId) throws CDIException {

		boolean hardware = (type == ICDIBreakpoint.HARDWARE);
		boolean temporary = (type == ICDIBreakpoint.TEMPORARY);
		String exprCond = null;
		int ignoreCount = 0;
		StringBuffer line = new StringBuffer();
		if (condition != null) {
			exprCond = condition.getExpression();
			ignoreCount = condition.getIgnoreCount();
		}

		if (location != null) {
			String file = location.getFile();
			String function = location.getFunction();
			if (file != null && file.length() > 0) {
				line.append(file).append(':');
				if (function != null && function.length() > 0) {
					line.append(function);
				} else {
					line.append(location.getLineNumber());
				}
			} else if (function != null && function.length() > 0) {
				line.append(function);
			} else if (location.getLineNumber() != 0) {
				line.append(location.getLineNumber());
			} else {
				line.append('*').append(location.getAddress());
			}
		}

		boolean state = suspendInferior();
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();
		MIBreakInsert breakInsert =
			factory.createMIBreakInsert( temporary, hardware, exprCond,
				ignoreCount, line.toString());
		MIBreakpoint[] points = null;
		try {
			s.getMISession().postCommand(breakInsert);
			MIBreakInsertInfo info = breakInsert.getMIBreakInsertInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
			points = info.getMIBreakpoints();
			if (points == null || points.length == 0) {
				throw new CDIException("Error parsing");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		} finally {
			resumeInferior(state);
		}
		Breakpoint bkpt = new Breakpoint(this, points[0]);
		breakList.add(bkpt);

		// Fire a created Event.
		MISession mi = s.getMISession();
		mi.fireEvent(new MIBreakpointCreatedEvent(bkpt.getMIBreakpoint().getNumber()));
		return bkpt;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#setWatchpoint(int, int, String, ICDICondition, boolean)
	 */
	public ICDIWatchpoint setWatchpoint( int type, int watchType, String expression,
		ICDICondition condition) throws CDIException {
		boolean access = ( (watchType & ICDIWatchpoint.WRITE) == ICDIWatchpoint.WRITE && 
						   (watchType & ICDIWatchpoint.READ) == ICDIWatchpoint.READ );
		boolean read = ( !((watchType & ICDIWatchpoint.WRITE) == ICDIWatchpoint.WRITE) && 
						  (watchType & ICDIWatchpoint.READ) == ICDIWatchpoint.READ );
		boolean state = suspendInferior();
		Session s = (Session)getSession();
		CommandFactory factory = s.getMISession().getCommandFactory();
		MIBreakWatch breakWatch =
			factory.createMIBreakWatch(access, read, expression);
		MIBreakpoint[] points = null;
		try {
			s.getMISession().postCommand(breakWatch);
			MIBreakWatchInfo info = breakWatch.getMIBreakWatchInfo();
			points = info.getMIBreakpoints();
			if (info == null) {
				throw new CDIException("No answer");
			}
			if (points == null || points.length == 0) {
				throw new CDIException("Parsing Error");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		} finally {
			resumeInferior(state);
		}
		Watchpoint bkpt = new Watchpoint(this, points[0]);
		breakList.add(bkpt);

		// Fire a created Event.
		MISession mi = s.getMISession();
		mi.fireEvent(new MIBreakpointCreatedEvent(bkpt.getMIBreakpoint().getNumber()));
		return bkpt;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#createCondition(int, String)
	 */
	public ICDICondition createCondition(int ignoreCount, String expression) {
		return new Condition(ignoreCount, expression);
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#createLocation(String, String, int)
	 */
	public ICDILocation createLocation(String file, String function, int line) {
		return new Location(file, function, line);
	}
	
	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#createLocation(long)
	 */
	public ICDILocation createLocation(long address) {
		return new Location(address);
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#isAutoUpdate()
	 */
	public boolean isAutoUpdate() {
		return autoupdate;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDIBreakpointManager#setAutoUpdate(boolean)
	 */
	public void setAutoUpdate(boolean update) {
		autoupdate = update;
	}

}
