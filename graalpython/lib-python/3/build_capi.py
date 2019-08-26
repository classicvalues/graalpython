# Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or
# data (collectively the "Software"), free of charge and under any and all
# copyright rights in the Software, and any and all patent rights owned or
# freely licensable by each licensor hereunder covering either (i) the
# unmodified Software as contributed to or provided by such licensor, or (ii)
# the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import sys
import os
import logging
import importlib.util
import distutils.log as dlog


capi_home = sys.graal_python_capi_home
capi_module_home = sys.graal_python_capi_module_home

def load_setup():
    spec = importlib.util.spec_from_file_location("setup.py", os.path.join(sys.graal_python_cext_src, "setup.py"))
    setup_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(setup_module)
    return setup_module


def configure_logging(args):
    if "-v" in args or "--verbose" in args:
        dlog.set_verbosity(dlog.DEBUG)
        logging.basicConfig(level=logging.DEBUG)
    elif "-q" in args or "--quiet" in args:
        dlog.set_verbosity(dlog.ERROR)
        logging.basicConfig(level=logging.ERROR)
    else:
        logging.basicConfig(level=logging.INFO)


def ensure_capi(args=[]):
    configure_logging(args)
    from distutils.sysconfig import get_config_var
    if not os.path.exists(os.path.join(capi_home, "libpython" + get_config_var("EXT_SUFFIX"))):
        return load_setup().build(capi_home, capi_module_home)
    return 0


def build(args=[]):
    configure_logging(args)
    return load_setup().build(capi_home, capi_module_home)


def clean(args=[]):
    configure_logging(args)
    return load_setup().clean(capi_home, capi_module_home)


class CapiNotBuiltContextManager:
    def __init__(self, module_name):
        self.module_name = module_name

    def __enter__(self):
        pass

    def __exit__(self, typ, val, tb):
        if typ and issubclass(typ, ModuleNotFoundError):
            print("Could not locate module '%s'. Did you forget to build the C API using 'graalpython -m build_capi'?" % self.module_name)
        # this causes the exception to propagate in any case
        return False


def hint(module_name):
    return CapiNotBuiltContextManager(module_name)


if __name__ == "__main__":
    if "clean" in sys.argv:
        clean(sys.argv)
    else:
        build(sys.argv)
