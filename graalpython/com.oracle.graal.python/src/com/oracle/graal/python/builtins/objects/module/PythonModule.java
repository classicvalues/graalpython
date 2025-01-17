/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 * Copyright (c) 2013, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.builtins.objects.module;

import static com.oracle.graal.python.nodes.SpecialAttributeNames.__CACHED__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__DOC__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__FILE__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__LOADER__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__NAME__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__PACKAGE__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.__SPEC__;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.dict.PDict;
import com.oracle.graal.python.builtins.objects.object.PythonObject;
import com.oracle.graal.python.nodes.PGuards;
import com.oracle.graal.python.nodes.object.SetDictNode;
import com.oracle.graal.python.runtime.object.PythonObjectFactory;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;

public final class PythonModule extends PythonObject {

    @CompilationFinal(dimensions = 1) static final Object[] INITIAL_MODULE_ATTRS = new Object[]{__NAME__, __DOC__, __PACKAGE__, __LOADER__, __SPEC__, __CACHED__, __FILE__};

    /**
     * Stores the native {@code PyModuleDef *} structure if this modules was created via the
     * multi-phase extension module initialization mechanism.
     */
    private Object nativeModuleDef;

    private PythonBuiltins builtins;

    public PythonModule(Object clazz, Shape instanceShape) {
        super(clazz, instanceShape);
        setAttribute(__NAME__, PNone.NO_VALUE);
        setAttribute(__DOC__, PNone.NO_VALUE);
        setAttribute(__PACKAGE__, PNone.NO_VALUE);
        setAttribute(__LOADER__, PNone.NO_VALUE);
        setAttribute(__SPEC__, PNone.NO_VALUE);
        setAttribute(__CACHED__, PNone.NO_VALUE);
        setAttribute(__FILE__, PNone.NO_VALUE);
    }

    /**
     * This constructor is just used to created built-in modules such that we can avoid the call to
     * {code __init__}.
     */
    private PythonModule(PythonLanguage lang, String moduleName) {
        super(PythonBuiltinClassType.PythonModule, PythonBuiltinClassType.PythonModule.getInstanceShape(lang));
        setAttribute(__NAME__, moduleName);
        setAttribute(__DOC__, PNone.NONE);
        setAttribute(__PACKAGE__, PNone.NONE);
        setAttribute(__LOADER__, PNone.NONE);
        setAttribute(__SPEC__, PNone.NONE);
        setAttribute(__CACHED__, PNone.NO_VALUE);
        setAttribute(__FILE__, PNone.NO_VALUE);
    }

    /**
     * Only to be used during context creation
     */
    @TruffleBoundary
    public static PythonModule createInternal(String moduleName) {
        PythonObjectFactory factory = PythonObjectFactory.getUncached();
        PythonModule pythonModule = new PythonModule(PythonLanguage.get(null), moduleName);
        PDict dict = factory.createDictFixedStorage(pythonModule);
        SetDictNode.getUncached().execute(pythonModule, dict);
        return pythonModule;
    }

    public PythonBuiltins getBuiltins() {
        return builtins;
    }

    public void setBuiltins(PythonBuiltins builtins) {
        this.builtins = builtins;
    }

    @Override
    public String toString() {
        Object attribute = this.getAttribute(__NAME__);
        return "<module '" + (PGuards.isNoValue(attribute) ? "?" : attribute) + "'>";
    }

    public Object getNativeModuleDef() {
        return nativeModuleDef;
    }

    public void setNativeModuleDef(Object nativeModuleDef) {
        this.nativeModuleDef = nativeModuleDef;
    }
}
