package top.niunaijun.blackbox.core.network;
/** Redirects libc TCP connect calls in a guest process; never used by the relay process. */
public final class SocketGate {
 static { System.loadLibrary("blackbox"); }
 public static native boolean install(int ipv4Port,int ipv6Port,byte[] token);
 /** Instrumentation only: verify the nonblocking connect path using a numeric IPv4 endpoint. */
 public static native boolean probeNonblocking(byte[] address,int port);
 private SocketGate(){}
}
