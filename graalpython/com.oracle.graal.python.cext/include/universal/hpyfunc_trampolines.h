/* MIT License
 *
 * Copyright (c) 2020, Oracle and/or its affiliates.
 * Copyright (c) 2019 pyhandle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#ifndef HPY_UNIVERSAL_HPYFUNC_TRAMPOLINES_H
#define HPY_UNIVERSAL_HPYFUNC_TRAMPOLINES_H

/* This file should be autogenerated */

typedef struct {
    cpy_PyObject *self;
    cpy_PyObject *result;
} _HPyFunc_args_NOARGS;

typedef struct {
    cpy_PyObject *self;
    cpy_PyObject *arg;
    cpy_PyObject *result;
} _HPyFunc_args_O;

typedef struct {
    cpy_PyObject *self;
    cpy_PyObject *args;
    cpy_PyObject *result;
} _HPyFunc_args_VARARGS;

typedef struct {
    cpy_PyObject *self;
    cpy_PyObject *args;
    cpy_PyObject *kw;
    cpy_PyObject *result;
} _HPyFunc_args_KEYWORDS;

typedef struct {
    cpy_PyObject *self;
    cpy_PyObject *args;
    cpy_PyObject *kw;
    int result;
} _HPyFunc_args_INITPROC;


#define _HPyFunc_TRAMPOLINE_HPyFunc_NOARGS(SYM, IMPL)                   \
    static cpy_PyObject *                                               \
    SYM(cpy_PyObject *self, cpy_PyObject *noargs)                       \
    {                                                                   \
        _HPyFunc_args_NOARGS a = { self };                              \
        _HPy_CallRealFunctionFromTrampoline(                            \
            _ctx_for_trampolines, HPyFunc_NOARGS, IMPL, &a);            \
        return a.result;                                                \
    }

#define _HPyFunc_TRAMPOLINE_HPyFunc_O(SYM, IMPL)                        \
    static cpy_PyObject *                                               \
    SYM(cpy_PyObject *self, cpy_PyObject *arg)                          \
    {                                                                   \
        _HPyFunc_args_O a = { self, arg };                              \
        _HPy_CallRealFunctionFromTrampoline(                            \
            _ctx_for_trampolines, HPyFunc_O, IMPL, &a);                 \
        return a.result;                                                \
    }


#define _HPyFunc_TRAMPOLINE_HPyFunc_VARARGS(SYM, IMPL)                  \
    static cpy_PyObject *                                               \
    SYM(cpy_PyObject *self, cpy_PyObject *args)                         \
    {                                                                   \
        _HPyFunc_args_VARARGS a = { self, args };                       \
        _HPy_CallRealFunctionFromTrampoline(                            \
            _ctx_for_trampolines, HPyFunc_VARARGS, IMPL, &a);           \
        return a.result;                                                \
    }


#define _HPyFunc_TRAMPOLINE_HPyFunc_KEYWORDS(SYM, IMPL)                 \
    static cpy_PyObject *                                               \
    SYM(cpy_PyObject *self, cpy_PyObject *args, cpy_PyObject *kw)       \
    {                                                                   \
        _HPyFunc_args_KEYWORDS a = { self, args, kw };                  \
        _HPy_CallRealFunctionFromTrampoline(                            \
            _ctx_for_trampolines, HPyFunc_KEYWORDS, IMPL, &a);          \
        return a.result;                                                \
    }

#define _HPyFunc_TRAMPOLINE_HPyFunc_INITPROC(SYM, IMPL)                 \
    static int                                                          \
    SYM(cpy_PyObject *self, cpy_PyObject *args, cpy_PyObject *kw)       \
    {                                                                   \
        _HPyFunc_args_INITPROC a = { self, args, kw };                  \
        _HPy_CallRealFunctionFromTrampoline(                            \
            _ctx_for_trampolines, HPyFunc_INITPROC, IMPL, &a);          \
        return a.result;                                                \
    }

/* special case: this function is used as 'tp_dealloc', but from the user
   point of view the slot is HPy_tp_destroy. */
#define _HPyFunc_TRAMPOLINE_HPyFunc_DESTROYFUNC(SYM, IMPL)              \
    static void                                                         \
    SYM(cpy_PyObject *self)                                             \
    {                                                                   \
        _HPy_CallDestroyAndThenDealloc(                                 \
            _ctx_for_trampolines, IMPL, self);                          \
    }


#endif // HPY_UNIVERSAL_HPYFUNC_TRAMPOLINES_H
