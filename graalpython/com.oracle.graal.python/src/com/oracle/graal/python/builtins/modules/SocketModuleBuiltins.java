/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.modules;

import static com.oracle.graal.python.builtins.PythonBuiltinClassType.NotImplementedError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.OSError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.OverflowError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.SocketGAIError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.TypeError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.ValueError;
import static com.oracle.graal.python.runtime.PosixConstants.AF_INET;
import static com.oracle.graal.python.runtime.PosixConstants.AF_INET6;
import static com.oracle.graal.python.runtime.PosixConstants.AF_UNSPEC;
import static com.oracle.graal.python.runtime.PosixConstants.AI_NUMERICHOST;
import static com.oracle.graal.python.runtime.PosixConstants.SOCK_DGRAM;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.nativeimage.ImageInfo;

import com.oracle.graal.python.annotations.ArgumentClinic;
import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.Python3Core;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.bytes.BytesNodes;
import com.oracle.graal.python.builtins.objects.bytes.PBytes;
import com.oracle.graal.python.builtins.objects.common.SequenceStorageNodes;
import com.oracle.graal.python.builtins.objects.exception.OSErrorEnum;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.builtins.objects.module.PythonModule;
import com.oracle.graal.python.builtins.objects.socket.PSocket;
import com.oracle.graal.python.builtins.objects.socket.SocketNodes;
import com.oracle.graal.python.builtins.objects.socket.SocketNodes.IdnaFromStringOrBytesConverterNode;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.lib.PyLongAsIntNode;
import com.oracle.graal.python.lib.PyLongAsLongNode;
import com.oracle.graal.python.nodes.ErrorMessages;
import com.oracle.graal.python.nodes.PConstructAndRaiseNode;
import com.oracle.graal.python.nodes.PGuards;
import com.oracle.graal.python.nodes.attributes.ReadAttributeFromObjectNode;
import com.oracle.graal.python.nodes.attributes.WriteAttributeToObjectNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.clinic.ArgumentClinicProvider;
import com.oracle.graal.python.nodes.util.CannotCastException;
import com.oracle.graal.python.nodes.util.CastToJavaIntExactNode;
import com.oracle.graal.python.nodes.util.CastToJavaStringNode;
import com.oracle.graal.python.runtime.GilNode;
import com.oracle.graal.python.runtime.PosixConstants;
import com.oracle.graal.python.runtime.PosixSupportLibrary;
import com.oracle.graal.python.runtime.PosixSupportLibrary.AddrInfoCursor;
import com.oracle.graal.python.runtime.PosixSupportLibrary.AddrInfoCursorLibrary;
import com.oracle.graal.python.runtime.PosixSupportLibrary.FamilySpecificSockAddr;
import com.oracle.graal.python.runtime.PosixSupportLibrary.GetAddrInfoException;
import com.oracle.graal.python.runtime.PosixSupportLibrary.Inet4SockAddr;
import com.oracle.graal.python.runtime.PosixSupportLibrary.Inet6SockAddr;
import com.oracle.graal.python.runtime.PosixSupportLibrary.PosixException;
import com.oracle.graal.python.runtime.PosixSupportLibrary.UniversalSockAddr;
import com.oracle.graal.python.runtime.PosixSupportLibrary.UniversalSockAddrLibrary;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.sequence.storage.ObjectSequenceStorage;
import com.oracle.graal.python.runtime.sequence.storage.SequenceStorage;
import com.oracle.graal.python.util.PythonUtils;
import com.oracle.graal.python.util.TimeUtils;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.ValueProfile;

@CoreFunctions(defineModule = "_socket")
public class SocketModuleBuiltins extends PythonBuiltins {

    public static HiddenKey DEFAULT_TIMEOUT_KEY = new HiddenKey("default_timeout");

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return SocketModuleBuiltinsFactory.getFactories();
    }

    private static class Service {
        int port;
        String protocol;

        public Service(int port, String protocol) {
            this.port = port;
            this.protocol = protocol;
        }
    }

    protected static Map<String, List<Service>> services;

    @TruffleBoundary
    private static Map<String, List<Service>> parseServices(TruffleLanguage.Env env) {
        TruffleFile services_file = env.getPublicTruffleFile("/etc/services");
        try {
            BufferedReader br = services_file.newBufferedReader();
            String line;
            Map<String, List<Service>> parsedServices = new HashMap<>();
            while ((line = br.readLine()) != null) {
                String[] service = cleanLine(line);
                if (service == null) {
                    continue;
                }
                String[] portAndProtocol = service[1].split("/");
                List<Service> serviceObj = parsedServices.computeIfAbsent(service[0], k -> new LinkedList<>());
                Service newService = new Service(Integer.parseInt(portAndProtocol[0]), portAndProtocol[1]);
                serviceObj.add(newService);
                if (service.length > 2) {
                    for (int i = 2; i < service.length; i++) {
                        serviceObj = parsedServices.computeIfAbsent(service[i], k -> new LinkedList<>());
                        serviceObj.add(newService);
                    }
                }
            }
            return parsedServices;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @TruffleBoundary
    private static String searchServicesForPort(TruffleLanguage.Env env, int port, String protocol) {
        if (services == null) {
            services = parseServices(env);
        }

        Set<String> servicesNames = services.keySet();

        for (String servName : servicesNames) {
            List<Service> serv = services.get(servName);
            for (Service servProto : serv) {
                if (servProto.port == port && (protocol == null || protocol.equals(servProto.protocol))) {
                    return servName;
                }
            }
        }
        return null;
    }

    private static String[] cleanLine(String input) {
        String line = input;
        if (line.startsWith("#")) {
            return null;
        }
        line = line.replaceAll("\\s+", " ");
        if (line.startsWith(" ")) {
            return null;
        }
        line = line.split("#")[0];
        String[] words = line.split(" ");
        if (words.length < 2) {
            return null;
        }
        return words;
    }

    @Override
    public void initialize(Python3Core core) {
        super.initialize(core);
        builtinConstants.put("SocketType", PythonBuiltinClassType.PSocket);
        builtinConstants.put("error", PythonBuiltinClassType.OSError);
        builtinConstants.put("has_ipv6", true);

        addConstant(PosixConstants.SOL_SOCKET);
        addConstants(PosixConstants.socketType);
        addConstants(PosixConstants.socketFamily);
        addConstants(PosixConstants.socketOptions);
        addConstants(PosixConstants.gaiFlags);
        addConstants(PosixConstants.gaiErrors);
        addConstants(PosixConstants.niFlags);
        addConstants(PosixConstants.ipProto);
        addConstants(PosixConstants.tcpOptions);
        addConstants(PosixConstants.shutdownHow);
        addConstants(PosixConstants.ip4Address);

        if (ImageInfo.inImageBuildtimeCode()) {
            // we do this eagerly for SVM images
            services = parseServices(core.getContext().getEnv());
        }
    }

    @Override
    public void postInitialize(Python3Core core) {
        core.lookupBuiltinModule("_socket").setAttribute(DEFAULT_TIMEOUT_KEY, -1L);
    }

    private void addConstants(PosixConstants.IntConstant[] constants) {
        for (PosixConstants.IntConstant constant : constants) {
            addConstant(constant);
        }
    }

    private void addConstant(PosixConstants.IntConstant constant) {
        if (constant.defined) {
            builtinConstants.put(constant.name, constant.getValueIfDefined());
        }
    }

    // socket(family=AF_INET, type=SOCK_STREAM, proto=0, fileno=None)
    @Builtin(name = "socket", minNumOfPositionalArgs = 1, takesVarArgs = true, takesVarKeywordArgs = true, constructsClass = PythonBuiltinClassType.PSocket)
    @GenerateNodeFactory
    public abstract static class SocketNode extends PythonBuiltinNode {
        // All the "real" work is done by __init__
        @Specialization
        Object socket(Object cls) {
            return factory().createSocket(cls);
        }
    }

    @Builtin(name = "getdefaulttimeout", minNumOfPositionalArgs = 1, declaresExplicitSelf = true)
    @GenerateNodeFactory
    public abstract static class GetDefaultTimeoutNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object get(PythonModule module,
                        @Cached ReadAttributeFromObjectNode readNode) {
            long timeout = (long) readNode.execute(module, DEFAULT_TIMEOUT_KEY);
            return timeout < 0 ? PNone.NONE : TimeUtils.pyTimeAsSecondsDouble(timeout);
        }
    }

    @Builtin(name = "setdefaulttimeout", minNumOfPositionalArgs = 2, declaresExplicitSelf = true)
    @GenerateNodeFactory
    public abstract static class SetDefaultTimeoutNode extends PythonBinaryBuiltinNode {
        @Specialization
        Object set(VirtualFrame frame, PythonModule module, Object value,
                        @Cached SocketNodes.ParseTimeoutNode parseTimeoutNode,
                        @Cached WriteAttributeToObjectNode writeNode) {
            long timeout = parseTimeoutNode.execute(frame, value);
            writeNode.execute(module, DEFAULT_TIMEOUT_KEY, timeout);
            return PNone.NONE;
        }
    }

    @Builtin(name = "gethostname")
    @GenerateNodeFactory
    public abstract static class GetHostnameNode extends PythonBuiltinNode {
        @Specialization
        String doGeneric(VirtualFrame frame,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @Cached SysModuleBuiltins.AuditNode auditNode,
                        @Cached GilNode gil) {
            auditNode.audit("socket.gethostname");
            try {
                gil.release(true);
                try {
                    return posixLib.getPathAsString(getPosixSupport(), posixLib.gethostname(getPosixSupport()));
                } finally {
                    gil.acquire();
                }
            } catch (PosixException e) {
                throw raiseOSErrorFromPosixException(frame, e);
            }
        }
    }

    @Builtin(name = "gethostbyaddr", minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class GetHostByAddrNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object doGeneric(@SuppressWarnings("unused") Object ip) {
            throw raise(NotImplementedError);
        }
    }

    @Builtin(name = "gethostbyname", minNumOfPositionalArgs = 1, numOfPositionalOnlyArgs = 1, parameterNames = {"name"})
    @GenerateNodeFactory
    public abstract static class GetHostByNameNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object getHostByName(VirtualFrame frame, Object nameObj,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @CachedLibrary(limit = "1") UniversalSockAddrLibrary addrLib,
                        @Cached("createIdnaConverter()") IdnaFromStringOrBytesConverterNode idnaConverter,
                        @Cached SysModuleBuiltins.AuditNode auditNode,
                        @Cached SocketNodes.SetIpAddrNode setIpAddrNode) {
            String name = idnaConverter.execute(frame, nameObj);
            auditNode.audit("socket.gethostbyname", factory().createTuple(new Object[]{nameObj}));
            UniversalSockAddr addr = setIpAddrNode.execute(frame, name, AF_INET.value);
            Inet4SockAddr inet4SockAddr = addrLib.asInet4SockAddr(addr);
            try {
                return posixLib.getPathAsString(getPosixSupport(), posixLib.inet_ntop(getPosixSupport(), AF_INET.value, inet4SockAddr.getAddressAsBytes()));
            } catch (PosixException e) {
                throw raiseOSErrorFromPosixException(frame, e);
            }
        }

        protected static IdnaFromStringOrBytesConverterNode createIdnaConverter() {
            return IdnaFromStringOrBytesConverterNode.create("gethostbyname", 1);
        }
    }

    @Builtin(name = "getservbyname", parameterNames = {"servicename", "protocolname"})
    @GenerateNodeFactory
    public abstract static class GetServByNameNode extends PythonBuiltinNode {
        @TruffleBoundary
        @Specialization(guards = {"isNoValue(protocolName)"})
        Object getServByName(String serviceName, @SuppressWarnings("unused") PNone protocolName) {
            if (services == null) {
                services = parseServices(getContext().getEnv());
            }

            List<Service> portsForService = services.get(serviceName);

            if (portsForService.size() == 0) {
                throw raise(PythonBuiltinClassType.OSError);
            } else {
                return factory().createInt(portsForService.get(0).port);
            }
        }

        @Specialization
        Object getServByName(String serviceName, String protocolName) {
            if (services == null) {
                services = parseServices(getContext().getEnv());
            }
            int port = op(serviceName, protocolName);
            if (port >= 0) {
                return port;
            } else {
                throw raise(PythonBuiltinClassType.OSError);
            }
        }

        @TruffleBoundary
        private static int op(String serviceName, String protocolName) {
            for (Service service : services.get(serviceName)) {
                if (service.protocol.equals(protocolName)) {
                    return service.port;
                }
            }
            return -1;
        }
    }

    @Builtin(name = "getservbyport", parameterNames = {"port", "protocolname"})
    @GenerateNodeFactory
    public abstract static class GetServByPortNode extends PythonBuiltinNode {

        @Specialization(guards = {"isNoValue(protocolName)"})
        Object getServByPort(long port, @SuppressWarnings("unused") PNone protocolName) {
            return getServByPort((int) port, protocolName);
        }

        @Specialization
        Object getServByPort(long port, String protocolName) {
            return getServByPort((int) port, protocolName);
        }

        @Specialization(guards = {"isNoValue(protocolName)"})
        Object getServByPort(PInt port, @SuppressWarnings("unused") PNone protocolName) {
            return getServByPort(port.intValue(), protocolName);
        }

        @Specialization
        Object getServByPort(PInt port, String protocolName) {
            return getServByPort(port.intValue(), protocolName);
        }

        @Specialization(guards = {"isNoValue(protocolName)"})
        Object getServByPort(int port, @SuppressWarnings("unused") PNone protocolName) {
            return getServByPort(port, (String) null);
        }

        @Specialization
        Object getServByPort(int port, String protocolName) {
            if (port < 0 || port > 65535) {
                throw raise(PythonBuiltinClassType.OverflowError);
            }
            String service = searchServicesForPort(getContext().getEnv(), port, protocolName);
            if (service != null) {
                return service;
            }
            throw raise(PythonBuiltinClassType.OSError, ErrorMessages.PORT_PROTO_NOT_FOUND);
        }
    }

    @Builtin(name = "getnameinfo", minNumOfPositionalArgs = 2, numOfPositionalOnlyArgs = 2, parameterNames = {"sockaddr", "flags"})
    @ArgumentClinic(name = "flags", conversion = ArgumentClinic.ClinicConversion.Int)
    @GenerateNodeFactory
    public abstract static class GetNameInfoNode extends PythonBinaryClinicBuiltinNode {
        @Specialization
        Object getNameInfo(VirtualFrame frame, PTuple sockaddr, int flags,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @CachedLibrary(limit = "1") AddrInfoCursorLibrary addrInfoCursorLib,
                        @CachedLibrary(limit = "1") UniversalSockAddrLibrary sockAddrLibrary,
                        @Cached GilNode gil,
                        @Cached SequenceStorageNodes.LenNode lenNode,
                        @Cached SequenceStorageNodes.GetItemScalarNode getItem,
                        @Cached CastToJavaStringNode castAddress,
                        @Cached PyLongAsIntNode asIntNode,
                        @Cached SysModuleBuiltins.AuditNode auditNode,
                        @Cached CastToJavaIntExactNode castToInt,
                        @Cached PConstructAndRaiseNode constructAndRaiseNode) {
            SequenceStorage addr = sockaddr.getSequenceStorage();
            int addrLen = lenNode.execute(addr);
            if (addrLen < 2 || addrLen > 4) {
                throw raise(TypeError, ErrorMessages.ILLEGAL_SOCKET_ADDR_ARG, "getnameinfo()");
            }
            String address;
            int port, flowinfo = 0, scopeid = 0;
            try {
                address = castAddress.execute(getItem.execute(addr, 0));
                port = asIntNode.execute(frame, getItem.execute(addr, 1));
                if (addrLen > 2) {
                    flowinfo = castToInt.execute(getItem.execute(addr, 2));
                }
                if (addrLen > 3) {
                    scopeid = castToInt.execute(getItem.execute(addr, 3));
                }
            } catch (CannotCastException | PException e) {
                throw raise(TypeError, ErrorMessages.ILLEGAL_SOCKET_ADDR_ARG, "getnameinfo()");
            }
            if (flowinfo > 0xfffff) {
                throw raise(OverflowError, "getnameinfo(): flowinfo must be 0-1048575.");
            }

            auditNode.audit("socket.getnameinfo", sockaddr);

            try {
                UniversalSockAddr resolvedAddr;
                int family;
                // TODO getaddrinfo lock?
                gil.release(true);
                try {
                    AddrInfoCursor cursor = posixLib.getaddrinfo(getPosixSupport(), posixLib.createPathFromString(getPosixSupport(), address),
                                    posixLib.createPathFromString(getPosixSupport(), PInt.toString(port)),
                                    AF_UNSPEC.value, SOCK_DGRAM.value, 0, AI_NUMERICHOST.value);
                    try {
                        family = addrInfoCursorLib.getFamily(cursor);
                        resolvedAddr = addrInfoCursorLib.getSockAddr(cursor);
                        if (addrInfoCursorLib.next(cursor)) {
                            throw raise(OSError, "sockaddr resolved to multiple addresses");
                        }
                    } finally {
                        addrInfoCursorLib.release(cursor);
                    }
                } finally {
                    gil.acquire();
                }

                FamilySpecificSockAddr queryAddr;
                if (family == AF_INET.value) {
                    if (addrLen != 2) {
                        throw raise(OSError, "IPv4 sockaddr must be 2 tuple");
                    }
                    queryAddr = new Inet4SockAddr(port, sockAddrLibrary.asInet4SockAddr(resolvedAddr).getAddress());
                } else if (family == AF_INET6.value) {
                    queryAddr = new Inet6SockAddr(port, sockAddrLibrary.asInet6SockAddr(resolvedAddr).getAddress(), flowinfo, scopeid);
                } else {
                    throw raise(OSError, "unknown family");
                }

                Object[] getnameinfo = posixLib.getnameinfo(getPosixSupport(), posixLib.createUniversalSockAddr(getPosixSupport(), queryAddr), flags);
                String host = posixLib.getPathAsString(getPosixSupport(), getnameinfo[0]);
                String service = posixLib.getPathAsString(getPosixSupport(), getnameinfo[1]);
                return factory().createTuple(new Object[]{host, service});
            } catch (GetAddrInfoException e) {
                throw constructAndRaiseNode.executeWithArgsOnly(frame, SocketGAIError, new Object[]{e.getErrorCode(), e.getMessage()});
            }
        }

        @Fallback
        @SuppressWarnings("unused")
        Object error(Object sockaddr, Object flags) {
            throw raise(TypeError, "getnameinfo() argument 1 must be a tuple");
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SocketModuleBuiltinsClinicProviders.GetNameInfoNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "getaddrinfo", minNumOfPositionalArgs = 2, parameterNames = {"host", "port", "family", "type", "proto", "flags"})
    @ArgumentClinic(name = "family", conversion = ArgumentClinic.ClinicConversion.Int, defaultValue = "com.oracle.graal.python.runtime.PosixConstants.AF_UNSPEC.value")
    @ArgumentClinic(name = "type", conversion = ArgumentClinic.ClinicConversion.Int, defaultValue = "0")
    @ArgumentClinic(name = "proto", conversion = ArgumentClinic.ClinicConversion.Int, defaultValue = "0")
    @ArgumentClinic(name = "flags", conversion = ArgumentClinic.ClinicConversion.Int, defaultValue = "0")
    @GenerateNodeFactory
    public abstract static class GetAddrInfoNode extends PythonClinicBuiltinNode {
        @Specialization
        Object getAddrInfo(VirtualFrame frame, Object hostObject, Object portObject, int family, int type, int proto, int flags,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @CachedLibrary(limit = "1") AddrInfoCursorLibrary cursorLib,
                        @Cached("createClassProfile()") ValueProfile profile,
                        @Cached("createIdna()") IdnaFromStringOrBytesConverterNode idna,
                        @Cached PyLongAsLongNode asLongNode,
                        @Cached CastToJavaStringNode castToString,
                        @Cached BytesNodes.ToBytesNode toBytes,
                        @Cached SysModuleBuiltins.AuditNode auditNode,
                        @Cached GilNode gil,
                        @Cached SocketNodes.MakeSockAddrNode makeSockAddrNode,
                        @Cached SequenceStorageNodes.AppendNode appendNode) {
            String host = null;
            if (hostObject != PNone.NONE) {
                host = idna.execute(frame, hostObject);
            }

            String port;
            Object portObjectProfiled = profile.profile(portObject);
            if (PGuards.canBeInteger(portObjectProfiled)) {
                port = PInt.toString(asLongNode.execute(frame, portObjectProfiled));
            } else if (PGuards.isString(portObjectProfiled)) {
                port = castToString.execute(portObjectProfiled);
            } else if (PGuards.isBytes(portObjectProfiled)) {
                port = PythonUtils.newString(toBytes.execute(portObjectProfiled));
            } else {
                throw raise(OSError, "Int or String expected");
            }

            auditNode.audit("socket.getaddrinfo", hostObject, portObjectProfiled, family, type, proto, flags);

            AddrInfoCursor cursor;
            try {
                // TODO getaddrinfo lock
                gil.release(true);
                try {
                    cursor = posixLib.getaddrinfo(getPosixSupport(), posixLib.createPathFromString(getPosixSupport(), host), posixLib.createPathFromString(getPosixSupport(), port),
                                    family, type, proto, flags);
                } finally {
                    gil.acquire();
                }
            } catch (GetAddrInfoException e) {
                throw getConstructAndRaiseNode().executeWithArgsOnly(frame, SocketGAIError, new Object[]{e.getErrorCode(), e.getMessage()});
            }
            try {
                SequenceStorage storage = new ObjectSequenceStorage(5);
                do {
                    Object addr = makeSockAddrNode.execute(frame, cursorLib.getSockAddr(cursor));
                    String canonName = "";
                    if (cursorLib.getCanonName(cursor) != null) {
                        canonName = posixLib.getPathAsString(getPosixSupport(), cursorLib.getCanonName(cursor));
                    }
                    PTuple tuple = factory().createTuple(new Object[]{cursorLib.getFamily(cursor), cursorLib.getSockType(cursor), cursorLib.getProtocol(cursor), canonName, addr});
                    storage = appendNode.execute(storage, tuple, SequenceStorageNodes.ListGeneralizationNode.SUPPLIER);
                } while (cursorLib.next(cursor));
                return factory().createList(storage);
            } finally {
                cursorLib.release(cursor);
            }
        }

        protected static IdnaFromStringOrBytesConverterNode createIdna() {
            return IdnaFromStringOrBytesConverterNode.create("getaddrinfo", 1);
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SocketModuleBuiltinsClinicProviders.GetAddrInfoNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "close", minNumOfPositionalArgs = 1, numOfPositionalOnlyArgs = 1, parameterNames = {"fd"})
    @GenerateNodeFactory
    public abstract static class CloseNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object close(VirtualFrame frame, Object fdObj,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @Cached GilNode gil,
                        @Cached PyLongAsIntNode asIntNode) {
            int fd = asIntNode.execute(frame, fdObj);
            if (fd != PSocket.INVALID_FD) {
                try {
                    gil.release(true);
                    try {
                        posixLib.close(getPosixSupport(), fd);
                    } finally {
                        gil.acquire();
                    }
                } catch (PosixException e) {
                    // CPython ignores ECONNRESET on close
                    if (e.getErrorCode() != OSErrorEnum.ECONNRESET.getNumber()) {
                        throw raiseOSErrorFromPosixException(frame, e);
                    }
                }
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "inet_aton", minNumOfPositionalArgs = 1, numOfPositionalOnlyArgs = 1, parameterNames = {"addr"})
    @ArgumentClinic(name = "addr", conversion = ArgumentClinic.ClinicConversion.String)
    @GenerateNodeFactory
    abstract static class InetAtoNNode extends PythonUnaryClinicBuiltinNode {
        @Specialization
        PBytes doConvert(String addr,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib) {
            try {
                int converted = posixLib.inet_aton(getPosixSupport(), posixLib.createPathFromString(getPosixSupport(), addr));
                return factory().createBytes(new Inet4SockAddr(0, converted).getAddressAsBytes());
            } catch (PosixSupportLibrary.InvalidAddressException e) {
                throw raise(OSError, "illegal IP address string passed to inet_aton");
            }
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SocketModuleBuiltinsClinicProviders.InetAtoNNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "inet_ntoa", minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class InetNtoANode extends PythonUnaryBuiltinNode {
        // TODO buffer API
        @Specialization
        String doGeneric(Object addr,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @Cached("createToBytes()") BytesNodes.ToBytesNode toBytesNode) {
            byte[] bytes = toBytesNode.execute(addr);
            if (bytes.length != 4) {
                throw raise(OSError, "packed IP wrong length for inet_ntoa");
            }
            Object result = posixLib.inet_ntoa(getPosixSupport(), PythonUtils.arrayAccessor.getInt(bytes, 0));
            return posixLib.getPathAsString(getPosixSupport(), result);
        }

        static BytesNodes.ToBytesNode createToBytes() {
            return BytesNodes.ToBytesNode.create(PythonBuiltinClassType.TypeError, "a bytes-like object is required, not '%p'");
        }
    }

    @Builtin(name = "inet_pton", minNumOfPositionalArgs = 2, numOfPositionalOnlyArgs = 2, parameterNames = {"family", "addr"})
    @ArgumentClinic(name = "family", conversion = ArgumentClinic.ClinicConversion.Int)
    @ArgumentClinic(name = "addr", conversion = ArgumentClinic.ClinicConversion.String)
    @GenerateNodeFactory
    abstract static class InetPtoNNode extends PythonBinaryClinicBuiltinNode {
        @Specialization
        PBytes doConvert(VirtualFrame frame, int family, String addr,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib) {
            try {
                byte[] bytes = posixLib.inet_pton(getPosixSupport(), family, posixLib.createPathFromString(getPosixSupport(), addr));
                return factory().createBytes(bytes);
            } catch (PosixException e) {
                throw raiseOSErrorFromPosixException(frame, e);
            } catch (PosixSupportLibrary.InvalidAddressException e) {
                throw raise(OSError, "illegal IP address string passed to inet_pton");
            }
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SocketModuleBuiltinsClinicProviders.InetPtoNNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "inet_ntop", minNumOfPositionalArgs = 2, numOfPositionalOnlyArgs = 2, parameterNames = {"family", "packed_ip"})
    @ArgumentClinic(name = "family", conversion = ArgumentClinic.ClinicConversion.Int)
    @GenerateNodeFactory
    abstract static class InetNtoPNode extends PythonBinaryClinicBuiltinNode {
        // TODO buffer API
        @Specialization
        String doGeneric(VirtualFrame frame, int family, Object obj,
                        @CachedLibrary("getPosixSupport()") PosixSupportLibrary posixLib,
                        @Cached("createToBytes()") BytesNodes.ToBytesNode toBytesNode) {
            byte[] bytes = toBytesNode.execute(obj);
            if (family == AF_INET.value) {
                if (bytes.length != 4) {
                    throw raise(ValueError, "invalid length of packed IP address string");
                }
            } else if (family == AF_INET6.value) {
                if (bytes.length != 6) {
                    throw raise(ValueError, "invalid length of packed IP address string");
                }
            } else {
                throw raise(ValueError, "unknown address family %d", family);
            }
            try {
                Object result = posixLib.inet_ntop(getPosixSupport(), family, bytes);
                return posixLib.getPathAsString(getPosixSupport(), result);
            } catch (PosixException e) {
                throw raiseOSErrorFromPosixException(frame, e);
            }
        }

        static BytesNodes.ToBytesNode createToBytes() {
            return BytesNodes.ToBytesNode.create(PythonBuiltinClassType.TypeError, "a bytes-like object is required, not '%p'");
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SocketModuleBuiltinsClinicProviders.InetNtoPNodeClinicProviderGen.INSTANCE;
        }
    }
}
