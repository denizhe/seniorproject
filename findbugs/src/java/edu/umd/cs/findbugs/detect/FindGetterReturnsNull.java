/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.bcel.PreorderDetector;

/**
 * @author Deniz Hekimci
 */
public class FindGetterReturnsNull extends PreorderDetector {

    BugReporter bugReporter;

    BugAccumulator bugAccumulator;

    private ClassContext classContext;

    private Method method;

    private Collection<BugInstance> pendingBugs = new LinkedList<BugInstance>();

    private static final boolean DEBUG_NULLRETURN = SystemProperties.getBoolean("fnd.debug.nullreturn");

    public static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug");

    public FindGetterReturnsNull(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;
        classContext.getJavaClass().accept(this);
    }

    @Override
    public void visitMethod(Method method) {
        super.visitMethod(method);
        try {
            if (method.getCode() == null)
                return;
            String currentMethod = null;
            JavaClass jclass = classContext.getJavaClass();
            CFG cfg = classContext.getCFG(method);
            if (method.isAbstract() || method.isNative() || method.getCode() == null)
                return;
            currentMethod = SignatureConverter.convertMethodSignature(jclass, method);
            if (DEBUG)
                System.out.println("Checking for NP in " + currentMethod);
            for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
                Location location = i.next();
                Instruction ins = location.getHandle().getInstruction();
                if (ins.getOpcode() == Constants.ARETURN) {
                    this.method = method;
                    examineReturnInstruction(location);
                }
            }
        } catch (CFGBuilderException e) {
            e.printStackTrace();
        }
    }

    private void examineReturnInstruction(Location location) {
        try {
            IsNullValueDataflow invDataflow = classContext.getIsNullValueDataflow(method);
            IsNullValueFrame frame = invDataflow.getFactAtLocation(location);
            ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getFactAtLocation(location);
            if (!vnaFrame.isValid())
                return;

            if (!frame.isValid())
                return;

            IsNullValue tos = frame.getTopValue();
            if (tos.isDefinitelyNull()) {

                if (DEBUG_NULLRETURN) {
                    System.out.println("Checking null return at " + location);
                }
                String methodName = method.getName();
                System.out.println(methodName);
                if (methodName.startsWith("get")) {
                    System.out.println("-->" + methodName);
                    String bugPattern = "NP_GETTER_RETURNS_NULL";
                    int priority = NORMAL_PRIORITY;
                    BugInstance warning = new BugInstance(this, bugPattern, priority).addClassAndMethod(
                            classContext.getJavaClass(), method);
                    pendingBugs.add(warning);
                }
            }
        } catch (DataflowAnalysisException e) {
            e.printStackTrace();
        } catch (CFGBuilderException e) {
            e.printStackTrace();
        }

        for (BugInstance b : pendingBugs)
            bugReporter.reportBug(b);
        pendingBugs.clear();
    }

    @Override
    public void report() {
    }

}
