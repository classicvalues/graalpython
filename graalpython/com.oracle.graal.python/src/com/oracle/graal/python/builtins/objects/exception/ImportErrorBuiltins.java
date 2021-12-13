/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.objects.exception;

import static com.oracle.graal.python.nodes.ErrorMessages.S_IS_AN_INVALID_ARG_FOR_S;
import static com.oracle.graal.python.nodes.SpecialMethodNames.__INIT__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.__REDUCE__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.__STR__;

import java.util.List;

import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.common.EmptyStorage;
import com.oracle.graal.python.builtins.objects.common.HashingStorage;
import com.oracle.graal.python.builtins.objects.common.HashingStorageLibrary;
import com.oracle.graal.python.builtins.objects.dict.PDict;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.lib.PyUnicodeCheckExactNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonVarargsBuiltinNode;
import com.oracle.graal.python.nodes.object.GetClassNode;
import com.oracle.graal.python.nodes.object.GetDictIfExistsNode;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;

@CoreFunctions(extendClasses = PythonBuiltinClassType.ImportError)
public final class ImportErrorBuiltins extends PythonBuiltins {

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return ImportErrorBuiltinsFactory.getFactories();
    }

    protected static final int IDX_MSG = 0;
    protected static final int IDX_NAME = 1;
    protected static final int IDX_PATH = 2;
    public static final int IMPORT_ERR_NUM_ATTRS = IDX_PATH + 1;

    public static final BaseExceptionAttrNode.StorageFactory IMPORT_ERROR_ATTR_FACTORY = (args, factory) -> {
        Object[] attrs = new Object[IMPORT_ERR_NUM_ATTRS];
        if (args.length == 1) {
            attrs[IDX_MSG] = args[0];
        }
        return attrs;
    };

    @Builtin(name = __INIT__, minNumOfPositionalArgs = 1, takesVarArgs = true, takesVarKeywordArgs = true)
    @GenerateNodeFactory
    public abstract static class ImportErrorInitNode extends PythonVarargsBuiltinNode {
        @Specialization
        Object init(PBaseException self, Object[] args, PKeyword[] kwargs,
                        @Cached BaseExceptionBuiltins.BaseExceptionInitNode baseExceptionInitNode) {
            baseExceptionInitNode.execute(self, args);
            Object[] attrs = IMPORT_ERROR_ATTR_FACTORY.create(args, factory());
            for (PKeyword kw : kwargs) {
                switch (kw.getName()) {
                    case "name":
                        attrs[IDX_NAME] = kw.getValue();
                        break;
                    case "path":
                        attrs[IDX_PATH] = kw.getValue();
                        break;
                    default:
                        throw raise(PythonBuiltinClassType.TypeError, S_IS_AN_INVALID_ARG_FOR_S, kw.getName(), "ImportError");
                }
            }
            self.setExceptionAttributes(attrs);
            return PNone.NONE;
        }
    }

    @Builtin(name = "msg", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true, doc = "exception message")
    @GenerateNodeFactory
    public abstract static class ImportErrorMsgNode extends PythonBuiltinNode {
        @Specialization
        Object generic(PBaseException self, Object value,
                        @Cached BaseExceptionAttrNode attrNode) {
            return attrNode.execute(self, value, IDX_MSG, IMPORT_ERROR_ATTR_FACTORY);
        }
    }

    @Builtin(name = "name", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true, doc = "module name")
    @GenerateNodeFactory
    public abstract static class ImportErrorNameNode extends PythonBuiltinNode {
        @Specialization
        Object generic(PBaseException self, Object value,
                        @Cached BaseExceptionAttrNode attrNode) {
            return attrNode.execute(self, value, IDX_NAME, IMPORT_ERROR_ATTR_FACTORY);
        }
    }

    @Builtin(name = "path", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true, doc = "module path")
    @GenerateNodeFactory
    public abstract static class ImportErrorPathNode extends PythonBuiltinNode {
        @Specialization
        Object generic(PBaseException self, Object value,
                        @Cached BaseExceptionAttrNode attrNode) {
            return attrNode.execute(self, value, IDX_PATH, IMPORT_ERROR_ATTR_FACTORY);
        }
    }

    @Builtin(name = __REDUCE__, minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class ImportErrorReduceNode extends PythonUnaryBuiltinNode {
        private Object getState(PBaseException self, GetDictIfExistsNode getDictIfExistsNode, HashingStorageLibrary hashlib) {
            assert self.getExceptionAttributes() != null;
            PDict dict = getDictIfExistsNode.execute(self);
            final Object name = self.getExceptionAttribute(IDX_NAME);
            final Object path = self.getExceptionAttribute(IDX_PATH);
            if (name != null || path != null) {
                HashingStorage storage = (dict != null) ? hashlib.copy(dict.getDictStorage()) : EmptyStorage.INSTANCE;
                if (name != null) {
                    storage = hashlib.setItem(storage, "name", name);
                }
                if (path != null) {
                    storage = hashlib.setItem(storage, "path", path);
                }
                return factory().createDict(storage);
            } else if (dict != null) {
                return dict;
            } else {
                return PNone.NONE;
            }
        }

        @Specialization
        Object reduce(VirtualFrame frame, PBaseException self,
                        @Cached GetClassNode getClassNode,
                        @Cached GetDictIfExistsNode getDictIfExistsNode,
                        @Cached BaseExceptionBuiltins.ArgsNode argsNode,
                        @CachedLibrary(limit = "getCallSiteInlineCacheMaxDepth()") HashingStorageLibrary hashlib) {
            Object clazz = getClassNode.execute(self);
            Object args = argsNode.executeObject(frame, self, PNone.NO_VALUE);
            Object state = getState(self, getDictIfExistsNode, hashlib);
            if (state == PNone.NONE) {
                return factory().createTuple(new Object[]{clazz, args});
            }
            return factory().createTuple(new Object[]{clazz, args, state});
        }
    }

    @Builtin(name = __STR__, minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class ImportErrorStrNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object str(VirtualFrame frame, PBaseException self,
                        @Cached BaseExceptionBuiltins.StrNode exStrNode,
                        @Cached PyUnicodeCheckExactNode unicodeCheckExactNode) {
            assert self.getExceptionAttributes() != null;
            final Object msg = self.getExceptionAttribute(IDX_MSG);
            if (msg != null && unicodeCheckExactNode.execute(msg)) {
                return msg;
            } else {
                return exStrNode.execute(frame, self);
            }
        }
    }
}
