package com.yshut.whatsappspaces;
import android.content.*;
import android.os.Bundle;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import top.niunaijun.blackbox.core.network.SocketGate;

final class NetworkProbe {
 static void require(boolean v,String msg)throws Exception{if(!v)throw new Exception(msg);}
 static void configure(Context c,Bundle result)throws Exception{
  File staging=new File(c.getFilesDir(),"residential-import.json");
  try{org.json.JSONObject j=new org.json.JSONObject(new String(Files.readAllBytes(staging.toPath()),StandardCharsets.UTF_8));ResidentialConfig cfg=new ResidentialConfig(j.getString("host"),j.getInt("port"),j.getString("user"),j.getString("password"),j.optString("protocol","AUTO"));NetworkSettingsActivity.apply(c,cfg);result.putBoolean("encryptedSaved",true);}finally{Files.deleteIfExists(staging.toPath());}
 }
 static void currentLocation(Context c,Bundle result)throws Exception{
  boolean use=NetworkOptions.residential(c);result.putBoolean("residentialEnabled",use);result.putBoolean("ipLocationEnabled",NetworkOptions.location(c));
  IpLocation point=IpLocation.lookup(ResidentialTransport.vpn(c),use?ResidentialConfig.load(c):null);
  result.putString("exitIp",point.ip);result.putString("city",point.city);result.putString("country",point.country);result.putDouble("latitude",point.latitude);result.putDouble("longitude",point.longitude);
  String saved=NetworkOptions.prefs(c).getString("last-location",null);
  if(saved!=null){org.json.JSONObject j=new org.json.JSONObject(saved);result.putString("lastSavedCity",j.optString("city"));result.putString("lastSavedIp",j.optString("ip"));
   top.niunaijun.blackbox.fake.frameworks.BLocationManager m=top.niunaijun.blackbox.fake.frameworks.BLocationManager.get();
   for(int id:SpacesController.spaces()){top.niunaijun.blackbox.entity.location.BLocation loc=m.getLocation(id,SpacesController.SOURCE);result.putBoolean("space_"+id+"_matchesSavedLocation",m.getPattern(id,SpacesController.SOURCE)==2&&loc!=null&&Math.abs(loc.getLatitude()-j.getDouble("latitude"))<0.000001&&Math.abs(loc.getLongitude()-j.getDouble("longitude"))<0.000001);}
  }
  result.putBoolean("settingsChangedByProbe",false);
 }
 static void status(Context c,Bundle result)throws Exception{
  ResidentialConfig cfg=ResidentialConfig.load(c);result.putBoolean("proxyConfigured",cfg!=null);if(cfg!=null)result.putString("protocol",cfg.protocol);
  try{ResidentialTransport.vpn(c);result.putBoolean("vpnAvailableToHost",true);}catch(IOException expected){result.putBoolean("vpnAvailableToHost",false);}
 }
 static void detect(Context c,Bundle result)throws Exception{
  ResidentialConfig cfg=ResidentialConfig.load(c);if(cfg==null)throw new IOException("Proxy not configured");android.net.Network vpn=ResidentialTransport.vpn(c);
  for(String protocol:new String[]{"SOCKS5","HTTP","HTTPS"}){
   ResidentialConfig trial=new ResidentialConfig(cfg.host,cfg.port,cfg.user,cfg.password,protocol);
   try{String ip=ResidentialTransport.test(vpn,trial);NetworkSettingsActivity.apply(c,trial);result.putString("protocol",protocol);result.putString("egressIp",ip);return;}catch(IOException ignored){}
  }
  throw new IOException("No supported authenticated proxy protocol completed the VPN-bound HTTPS probe");
 }
 static void diagnose(Context c,Bundle result)throws Exception{
  ResidentialConfig cfg=ResidentialConfig.load(c);android.net.Network n=ResidentialTransport.vpn(c);result.putString("vpn","present");
  InetAddress[] addresses=n.getAllByName(cfg.host);result.putInt("proxyAddressCount",addresses.length);
  try(Socket s=new Socket()){n.bindSocket(s);result.putString("vpnSocketBind","PASS");s.connect(new InetSocketAddress(addresses[0],cfg.port),6000);result.putString("proxyTcp","PASS");}catch(Exception e){result.putString("proxyTcp",e.getClass().getSimpleName());}
  try(Socket s=new Socket()){InetAddress[] baseline=n.getAllByName("api.ipify.org");n.bindSocket(s);s.connect(new InetSocketAddress(baseline[0],443),6000);s.setSoTimeout(6000);try(javax.net.ssl.SSLSocket tls=ResidentialTransport.tls(s,"api.ipify.org",443)){result.putString("vpnBaselineTls","PASS");}}catch(Exception e){result.putString("vpnBaselineTls",e.getClass().getSimpleName());}
  for(String protocol:new String[]{"SOCKS5","HTTP","HTTPS"}){
   ResidentialConfig trial=new ResidentialConfig(cfg.host,cfg.port,cfg.user,cfg.password,protocol);
   try(Socket probe=ResidentialTransport.tunnel(n,trial,"www.cloudflare.com",443)){result.putString("negotiation_"+protocol,"PASS");}catch(Exception e){result.putString("negotiation_"+protocol,e.getClass().getSimpleName());}
   try{String ip=ResidentialTransport.test(n,trial);result.putString("attempt_"+protocol,"PASS");NetworkSettingsActivity.apply(c,trial);result.putString("protocol",protocol);result.putString("egressIp",ip);return;}
   catch(Exception e){result.putString("attempt_"+protocol,e.getClass().getSimpleName()+": "+(e instanceof ResidentialTransport.ProxyFailure?e.getMessage():"connection_or_tls_failed"));}
  }
  result.putString("chain","UNVERIFIED");
 }
 static void lookupVpnLocation(Context c,Bundle result)throws Exception{
  IpLocation point=IpLocation.lookup(ResidentialTransport.vpn(c),null);result.putString("vpnEgressIp",point.ip);result.putString("country",point.country);result.putString("region",point.region);result.putString("city",point.city);result.putDouble("latitude",point.latitude);result.putDouble("longitude",point.longitude);result.putBoolean("locationApplied",false);
 }
 static void options(Context context,Bundle result)throws Exception{
  Context scoped=new ContextWrapper(context){@Override public File getNoBackupFilesDir(){File f=new File(super.getNoBackupFilesDir(),"options-probe");f.mkdirs();return f;}@Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("options-probe-"+name,mode);}};
  try{
   ResidentialConfig cfg=new ResidentialConfig("proxy.example.test",443,"fixture-user","fixture-password","SOCKS5");cfg.save(scoped);NetworkOptions.save(scoped,true,false);require(NetworkOptions.residential(scoped),"Proxy toggle did not enable");
   NetworkOptions.save(scoped,false,true);require(!NetworkOptions.residential(scoped)&&NetworkOptions.location(scoped),"VPN-only / location toggle failed");require("fixture-password".equals(ResidentialConfig.load(scoped).password),"Disabling proxy removed credentials");require("VPN_ONLY".equals(NetworkOptions.revision(scoped)),"VPN-only relay revision invalid");
   NetworkOptions.save(scoped,true,false);require("fixture-user".equals(ResidentialConfig.load(scoped).user),"Re-enabling lost profile");result.putBoolean("togglePreservesCredentials",true);
  }finally{NetworkOptions.prefs(scoped).edit().clear().commit();for(File file:scoped.getNoBackupFilesDir().listFiles())Files.delete(file.toPath());Files.delete(scoped.getNoBackupFilesDir().toPath());}
  IpLocation point=IpLocation.parse(new org.json.JSONObject("{\"success\":true,\"ip\":\"203.0.113.9\",\"country\":\"Example\",\"city\":\"Fixture\",\"latitude\":35.25,\"longitude\":-110.5}"));require(point.latitude==35.25&&point.longitude==-110.5,"IP coordinate mapping failed");
  boolean invalid=false;try{IpLocation.parse(new org.json.JSONObject("{\"success\":false}"));}catch(IOException e){invalid=true;}require(invalid,"Provider error accepted");
  invalid=false;try{new IpLocation("203.0.113.9","","","",91,0);}catch(IOException e){invalid=true;}require(invalid,"Invalid latitude accepted");
  top.niunaijun.blackbox.entity.location.BLocation original=new top.niunaijun.blackbox.entity.location.BLocation(35.25,-110.5);original.setAccuracy(100000f);android.os.Parcel parcel=android.os.Parcel.obtain();original.writeToParcel(parcel,0);parcel.setDataPosition(0);top.niunaijun.blackbox.entity.location.BLocation copy=top.niunaijun.blackbox.entity.location.BLocation.CREATOR.createFromParcel(parcel);parcel.recycle();require(copy.getAccuracy()==100000f&&copy.getLatitude()==35.25&&copy.getLongitude()==-110.5,"Location parcel mismatch");require(copy.convert2SystemLocation().getElapsedRealtimeNanos()>0,"Location lacks elapsed timestamp");
  top.niunaijun.blackbox.fake.frameworks.BLocationManager locations=top.niunaijun.blackbox.fake.frameworks.BLocationManager.get();String probePackage="com.yshut.whatsappspaces.locationprobe";
  try{locations.setLocation(31,probePackage,original);locations.setPattern(31,probePackage,2);top.niunaijun.blackbox.entity.location.BLocation read=locations.getLocation(31,probePackage);require(read!=null&&read.getLatitude()==35.25&&read.getLongitude()==-110.5&&read.getAccuracy()==100000f,"Virtual engine location readback failed");}finally{locations.setPattern(31,probePackage,0);}
  result.putBoolean("locationParserAndParcel",true);result.putBoolean("virtualLocationReadback",true);
  require("abc".equals(ResidentialTransport.body(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nContent-Length: 3\r\n\r\nabc".getBytes(StandardCharsets.US_ASCII)))),"HTTP content-length body failed");
  require("abcde".equals(ResidentialTransport.body(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)))),"HTTP chunked body failed");result.putBoolean("httpBodyFraming",true);
 }
 static void fixture(Context context,Bundle result)throws Exception{
  Context scoped=new ContextWrapper(context){@Override public File getNoBackupFilesDir(){File f=new File(super.getNoBackupFilesDir(),"network-probe");f.mkdirs();return f;}};
  ResidentialConfig dummy=new ResidentialConfig("proxy.example.test",443,"fixture-user","fixture-password","SOCKS5");dummy.save(scoped);
  require("fixture-password".equals(ResidentialConfig.load(scoped).password),"Encrypted config roundtrip failed");
  require(!new String(Files.readAllBytes(new File(scoped.getNoBackupFilesDir(),"residential-config.enc").toPath()),StandardCharsets.ISO_8859_1).contains("fixture-password"),"Credential persisted in plaintext");
  for(File f:scoped.getNoBackupFilesDir().listFiles())Files.delete(f.toPath());Files.delete(scoped.getNoBackupFilesDir().toPath());
  result.putBoolean("encryptedRoundtrip",true);
  protocolFixture(dummy,false);protocolFixture(dummy,true);httpFixture(dummy);result.putBoolean("proxyProtocolFixtures",true);
  try{ResidentialTransport.vpn(context);result.putString("vpnGate","VPN_PRESENT");}catch(IOException expected){result.putString("vpnGate","REFUSED_WITHOUT_VPN");}
  byte[] token=new byte[32];Arrays.fill(token,(byte)42);
  ExecutorService executor=Executors.newCachedThreadPool();
  byte[] lo6=new byte[16];lo6[15]=1;
  try(ServerSocket four=new ServerSocket(0,16,InetAddress.getByAddress(new byte[]{127,0,0,1}));ServerSocket six=new ServerSocket(0,16,InetAddress.getByAddress(lo6))){
   four.setSoTimeout(4000);six.setSoTimeout(4000);
   require(SocketGate.install(four.getLocalPort(),six.getLocalPort(),token),"Native socket hooks unavailable");
   BlockingQueue<String> targets=new LinkedBlockingQueue<>();
   executor.execute(()->acceptLoop(four,token,targets));executor.execute(()->acceptLoop(six,token,targets));
   try(Socket client=new Socket()){client.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte)203,0,113,9}),443),2000);client.setSoTimeout(2000);client.getOutputStream().write(77);require(client.getInputStream().read()==78,"IPv4 payload failed");}
   require("203.0.113.9".equals(targets.poll(5,TimeUnit.SECONDS)),"IPv4 header incorrect");

   try(Socket client=new Socket()){client.connect(new InetSocketAddress(InetAddress.getByName("2001:db8::9"),443),2000);client.setSoTimeout(2000);client.getOutputStream().write(77);require(client.getInputStream().read()==78,"IPv6 payload failed");}
   require(InetAddress.getByName("2001:db8::9").getHostAddress().equals(targets.poll(5,TimeUnit.SECONDS)),"IPv6 header incorrect");

   require(SocketGate.probeNonblocking(new byte[]{(byte)203,0,113,9},443),"Nonblocking mode not preserved");require("203.0.113.9".equals(targets.poll(5,TimeUnit.SECONDS)),"Nonblocking header incorrect");
   boolean denied=false;try(DatagramSocket udp=new DatagramSocket()){udp.send(new DatagramPacket(new byte[]{1},1,InetAddress.getByAddress(new byte[]{(byte)203,0,113,9}),443));}catch(IOException expected){denied=true;}
   require(denied,"UDP was not blocked");result.putBoolean("udpBlocked",true);result.putBoolean("ipv4TcpRedirected",true);result.putBoolean("ipv6TcpRedirected",true);result.putBoolean("nonblockingPreserved",true);
  }finally{executor.shutdownNow();}
  boolean unavailable=false;try(Socket client=new Socket()){client.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte)203,0,113,9}),443),1000);}catch(IOException expected){unavailable=true;}
  require(unavailable,"Missing relay did not fail closed");result.putBoolean("relayAbsentBlocked",true);
 }
 static void acceptLoop(ServerSocket listener,byte[] token,BlockingQueue<String> targets){
  while(!listener.isClosed()){try(Socket accepted=listener.accept()){accepted.setSoTimeout(2500);byte[] h=new byte[55];new DataInputStream(accepted.getInputStream()).readFully(h);
   require(Arrays.equals(token,Arrays.copyOfRange(h,4,36)),"Token mismatch");require(h[0]=='R'&&h[1]=='S'&&h[2]=='P'&&h[3]=='1'&&(h[36]==1||h[36]==4)&&h[53]==1&&(h[54]&255)==187,"Target header mismatch");
   InetAddress target=InetAddress.getByAddress(Arrays.copyOfRange(h,37,h[36]==1?41:53));targets.add(target.getHostAddress());
   int data=accepted.getInputStream().read();if(data!=-1){require(data==77,"Guest payload mismatch");accepted.getOutputStream().write(78);}
  }catch(Exception e){if(!listener.isClosed())targets.offer("ERROR");break;}}
 }
 static void protocolFixture(ResidentialConfig cfg,boolean reject)throws Exception{
  ExecutorService executor=Executors.newSingleThreadExecutor();
  try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){
   Future<Boolean> answer=executor.submit(()->{try(Socket s=server.accept()){s.setSoTimeout(2000);DataInputStream in=new DataInputStream(s.getInputStream());OutputStream out=s.getOutputStream();byte[] hello=new byte[3];in.readFully(hello);require(Arrays.equals(hello,new byte[]{5,1,2}),"SOCKS auth not required");out.write(new byte[]{5,2});
    require(in.readUnsignedByte()==1,"Auth version");byte[] user=new byte[in.readUnsignedByte()];in.readFully(user);byte[] pass=new byte[in.readUnsignedByte()];in.readFully(pass);require(new String(user,StandardCharsets.UTF_8).equals(cfg.user)&&new String(pass,StandardCharsets.UTF_8).equals(cfg.password),"Auth values incorrect");out.write(new byte[]{1,(byte)(reject?1:0)});if(reject)return true;
    require(in.readUnsignedByte()==5&&in.readUnsignedByte()==1&&in.readUnsignedByte()==0&&in.readUnsignedByte()==3,"SOCKS connect type");byte[] host=new byte[in.readUnsignedByte()];in.readFully(host);require(new String(host,StandardCharsets.US_ASCII).equals("example.test")&&in.readUnsignedShort()==443,"SOCKS target");for(byte b:new byte[]{5,0,0,1,127,0,0,1,0,80}){out.write(b);out.flush();}return true;}});
   boolean refused=false;try(Socket client=new Socket(InetAddress.getLoopbackAddress(),server.getLocalPort())){client.setSoTimeout(3000);try{ResidentialTransport.socks(client,cfg,"example.test",443);}catch(ResidentialTransport.ProxyFailure e){refused=true;}}
   require(refused==reject,"SOCKS auth rejection incorrect");require(answer.get(5,TimeUnit.SECONDS),"SOCKS fixture failed");
  }finally{executor.shutdownNow();}
 }
 static void httpFixture(ResidentialConfig cfg)throws Exception{
  ExecutorService executor=Executors.newSingleThreadExecutor();try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){
   Future<Boolean> answer=executor.submit(()->{try(Socket s=server.accept()){s.setSoTimeout(2000);String request=ResidentialTransport.readHeader(s.getInputStream());require(request.startsWith("CONNECT example.test:443 HTTP/1.1"),"HTTP target mismatch");require(request.contains("Proxy-Authorization: Basic "+Base64.getEncoder().encodeToString((cfg.user+":"+cfg.password).getBytes(StandardCharsets.UTF_8))),"HTTP auth absent");s.getOutputStream().write("HTTP/1.1 200 Connection established\r\n\r\nX".getBytes(StandardCharsets.US_ASCII));return true;}});
   try(Socket client=new Socket(InetAddress.getLoopbackAddress(),server.getLocalPort())){client.setSoTimeout(3000);ResidentialTransport.http(client,cfg,"example.test",443);require(client.getInputStream().read()=='X',"HTTP handshake consumed target bytes");}require(answer.get(5,TimeUnit.SECONDS),"HTTP fixture failed");
  }finally{executor.shutdownNow();}
 }
}
