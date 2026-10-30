package com.yshut.whatsappspaces;
import android.content.Context;
import android.net.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ResidentialTransport {
 static Network vpn(Context c)throws IOException{
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);Network n=cm.getActiveNetwork();NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);
  if(caps==null||!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))throw new IOException("请连接 v2rayNG，并将多开空间纳入 VPN 代理范围");return n;
 }
 static Socket gateway(Network network,ResidentialConfig cfg)throws IOException{
  InetAddress[] addresses=network.getAllByName(cfg.host);Socket result=null;
  for(InetAddress address:addresses){Socket s=new Socket();try{network.bindSocket(s);s.connect(new InetSocketAddress(address,cfg.port),8000);s.setSoTimeout(10000);s.setTcpNoDelay(true);result=s;break;}catch(IOException e){try{s.close();}catch(IOException ignored){}}}
  if(result==null)throw new IOException("经 VPN 无法连接住宅代理");
  if("HTTPS".equals(cfg.protocol)){try{return tls(result,cfg.host,cfg.port);}catch(IOException e){try{result.close();}catch(Exception ignored){}throw new IOException("住宅代理 TLS 连接失败");}}
  return result;
 }
 static SSLSocket tls(Socket s,String host,int port)throws IOException{
  SSLSocket tls=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(s,host,port,true);
  SSLParameters params=tls.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");tls.setSSLParameters(params);tls.setSoTimeout(10000);tls.startHandshake();return tls;
 }
 static Socket tunnel(Network n,ResidentialConfig cfg,String target,int port)throws IOException{
  if("AUTO".equals(cfg.protocol))throw new IOException("请先检测代理协议并保存");
  Socket s=gateway(n,cfg);try{if("SOCKS5".equals(cfg.protocol))socks(s,cfg,target,port);else http(s,cfg,target,port);return s;}catch(Exception error){try{s.close();}catch(Exception ignored){}throw new IOException(error instanceof ProxyFailure?error.getMessage():"住宅代理协议或连接失败");}
 }
 static final class ProxyFailure extends IOException{ProxyFailure(String message){super(message);}}
 static void socks(Socket s,ResidentialConfig c,String target,int port)throws IOException{
  DataInputStream in=new DataInputStream(s.getInputStream());OutputStream out=s.getOutputStream();
  // Require username/password authentication when credentials were supplied.
  boolean auth=!c.user.isEmpty()||!c.password.isEmpty();out.write(new byte[]{5,1,(byte)(auth?2:0)});out.flush();
  if(in.readUnsignedByte()!=5||in.readUnsignedByte()!=(auth?2:0))throw new ProxyFailure("SOCKS5 握手失败");
  if(auth){byte[] u=c.user.getBytes(StandardCharsets.UTF_8),p=c.password.getBytes(StandardCharsets.UTF_8);out.write(1);out.write(u.length);out.write(u);out.write(p.length);out.write(p);out.flush();if(in.readUnsignedByte()!=1||in.readUnsignedByte()!=0)throw new ProxyFailure("住宅代理认证失败，请检查账号密码");}
  out.write(new byte[]{5,1,0});byte[] domain=target.getBytes(StandardCharsets.US_ASCII);
  if(target.indexOf(':')>=0){out.write(4);out.write(InetAddress.getByName(target).getAddress());}
  else if(target.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")){out.write(1);out.write(InetAddress.getByName(target).getAddress());}
  else{if(domain.length>255)throw new ProxyFailure("目标域名过长");out.write(3);out.write(domain.length);out.write(domain);}
  out.write(port>>8);out.write(port&255);out.flush();
  int version=in.readUnsignedByte(),reply=in.readUnsignedByte(),reserved=in.readUnsignedByte(),type=in.readUnsignedByte();
  if(version!=5||reply!=0||reserved!=0)throw new ProxyFailure("住宅代理拒绝目标连接（代码 "+reply+"）");
  int len=type==1?4:type==4?16:type==3?in.readUnsignedByte():-1;if(len<0)throw new ProxyFailure("SOCKS5 响应无效");byte[] rest=new byte[len+2];in.readFully(rest);
 }
 static void http(Socket s,ResidentialConfig c,String target,int port)throws IOException{
  String authority=(target.indexOf(':')>=0?"["+target+"]":target)+":"+port;
  String auth=Base64.getEncoder().encodeToString((c.user+":"+c.password).getBytes(StandardCharsets.UTF_8));
  String request="CONNECT "+authority+" HTTP/1.1\r\nHost: "+authority+"\r\nProxy-Authorization: Basic "+auth+"\r\n\r\n";
  s.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));s.getOutputStream().flush();
  String header=readHeader(s.getInputStream());String first=header.split("\r\n",2)[0];
  if(!first.matches("HTTP/1\\.[01] 200(?: .*)?"))throw new ProxyFailure(first.contains(" 407 ")?"住宅代理认证失败，请检查账号密码":"HTTP 住宅代理拒绝目标连接");
 }
 static String readHeader(InputStream in)throws IOException{
  ByteArrayOutputStream out=new ByteArrayOutputStream();int state=0;byte[] end={13,10,13,10};
  while(out.size()<16384){int b=in.read();if(b<0)throw new EOFException();out.write(b);state=b==end[state]?state+1:b==13?1:0;if(state==4)return out.toString("US-ASCII");}throw new IOException("代理响应头过长");
 }
 static Socket direct(Network n,String host,int port)throws IOException{
  InetAddress[] addresses=n.getAllByName(host);
  for(InetAddress address:addresses){Socket s=new Socket();try{n.bindSocket(s);s.connect(new InetSocketAddress(address,port),8000);s.setSoTimeout(10000);s.setTcpNoDelay(true);return s;}catch(IOException e){try{s.close();}catch(IOException ignored){}}}
  throw new IOException("VPN 连接目标失败");
 }
 static String fetch(Network n,ResidentialConfig cfg,String host,String path)throws IOException{
  try(Socket stream=cfg==null?direct(n,host,443):tunnel(n,cfg,host,443);SSLSocket tls=tls(stream,host,443)){
   tls.getOutputStream().write(("GET "+path+" HTTP/1.1\r\nHost: "+host+"\r\nAccept: application/json, text/plain\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));tls.getOutputStream().flush();return body(tls.getInputStream());
  }
 }
 static String body(InputStream in)throws IOException{
  String header=readHeader(in);String[] lines=header.split("\r\n");if(!lines[0].matches("HTTP/1\\.[01] 200(?: .*)?"))throw new IOException("出口查询服务暂不可用");
  Map<String,String> headers=new HashMap<>();for(int i=1;i<lines.length;i++){int colon=lines[i].indexOf(':');if(colon>0)headers.put(lines[i].substring(0,colon).toLowerCase(Locale.ROOT),lines[i].substring(colon+1).trim());}
  if(headers.containsKey("content-encoding")&&!"identity".equalsIgnoreCase(headers.get("content-encoding")))throw new IOException("不支持压缩响应");
  ByteArrayOutputStream result=new ByteArrayOutputStream();String transfer=headers.get("transfer-encoding");
  if(transfer!=null){
   if(!"chunked".equalsIgnoreCase(transfer))throw new IOException("响应传输格式无效");
   while(true){String line=line(in);int length;try{length=Integer.parseInt(line.split(";",2)[0].trim(),16);}catch(Exception e){throw new IOException("分块响应无效");}
    if(length<0||result.size()+length>65536)throw new IOException("响应过长");if(length==0){int trailerBytes=0;while(true){String trailer=line(in);if(trailer.isEmpty())break;trailerBytes+=trailer.length();if(trailerBytes>16384)throw new IOException("响应尾部过长");}break;}
    readCount(in,result,length);if(in.read()!=13||in.read()!=10)throw new IOException("分块结束无效");
   }
  }else if(headers.containsKey("content-length")){
   int length;try{length=Integer.parseInt(headers.get("content-length"));}catch(Exception e){throw new IOException("响应长度无效");}
   if(length<0||length>65536)throw new IOException("响应过长");readCount(in,result,length);
  }else{int next;while((next=in.read())!=-1){if(result.size()>=65536)throw new IOException("响应过长");result.write(next);}}
  return result.toString("UTF-8");
 }
 private static void readCount(InputStream in,OutputStream out,int length)throws IOException{byte[] bytes=new byte[4096];while(length>0){int count=in.read(bytes,0,Math.min(length,bytes.length));if(count<0)throw new EOFException();out.write(bytes,0,count);length-=count;}}
 private static String line(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();while(out.size()<4096){int next=in.read();if(next<0)throw new EOFException();if(next==13){if(in.read()!=10)throw new IOException("响应行无效");return out.toString("US-ASCII");}out.write(next);}throw new IOException("响应行过长");}
 static String test(Network n,ResidentialConfig cfg)throws IOException{
  String ip=null;try{ip=fetch(n,cfg,"api.ipify.org","/").trim();}catch(IOException ignored){}
  if(ip!=null&&ip.matches("[0-9a-fA-F:.]{3,64}"))return ip;
  String trace=fetch(n,cfg,"www.cloudflare.com","/cdn-cgi/trace");for(String line:trace.split("\\n"))if(line.startsWith("ip=")){String found=line.substring(3).trim();if(found.matches("[0-9a-fA-F:.]{3,64}"))return found;}throw new IOException("出口检测响应无效");
 }
 private ResidentialTransport(){}
}
