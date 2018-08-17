/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.nodes.call.special;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.objects.function.PBuiltinFunction;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonTernaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonVarargsBuiltinNode;
import com.oracle.graal.python.nodes.truffle.PythonTypes;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

@TypeSystemReference(PythonTypes.class)
@ImportStatic(PythonOptions.class)
abstract class CallSpecialMethodNode extends Node {

    /**
     * Returns a new instanceof the builtin if it's a subclass of the given class, and null
     * otherwise.
     */
    private static <T extends PythonBuiltinBaseNode> T getBuiltin(PBuiltinFunction func, Class<T> clazz) {
        NodeFactory<? extends PythonBuiltinBaseNode> builtinNodeFactory = func.getBuiltinNodeFactory();
        if (builtinNodeFactory != null) {
            return clazz.isAssignableFrom(builtinNodeFactory.getNodeClass()) ? clazz.cast(func.getBuiltinNodeFactory().createNode()) : null;
        } else {
            return null;
        }
    }

    protected Assumption singleContextAssumption() {
        return PythonLanguage.singleContextAssumption;
    }

    protected static PythonUnaryBuiltinNode getUnary(PBuiltinFunction func) {
        return getBuiltin(func, PythonUnaryBuiltinNode.class);
    }

    protected static PythonBinaryBuiltinNode getBinary(PBuiltinFunction func) {
        return getBuiltin(func, PythonBinaryBuiltinNode.class);
    }

    protected static PythonTernaryBuiltinNode getTernary(PBuiltinFunction func) {
        return getBuiltin(func, PythonTernaryBuiltinNode.class);
    }

    protected static PythonVarargsBuiltinNode getVarargs(PBuiltinFunction func) {
        return getBuiltin(func, PythonVarargsBuiltinNode.class);
    }
}
