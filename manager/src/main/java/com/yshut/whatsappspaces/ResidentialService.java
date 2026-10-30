package com.yshut.whatsappspaces;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** Runs outside the virtualization engine, with real ConnectivityManager / VPN network binding. */
public final class ResidentialService extends Service {
 private ServerSocket v4,v6;private ResidentialConfig config;private byte[] token;
 private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
 private final Semaphore slots=new Semaphore(32);
 private final ExecutorService workers=Executors.newCachedThreadPool();
 private volatile boolean running;
 private ConnectivityManager.NetworkCallback callback;
 static File stateFile(Context c){return new File(c.getNoBackupFilesDir(),"residential-runtime.json");}
 static final class State{
  final int v4,v6;final byte[] token;final String revision;
  State(int a,int b,byte[] t,String r){v4=a;v6=b;token=t;revision=r;}
  static State read(Context c)throws Exception{JSONObject j=new JSONObject(new String(new AtomicFile(stateFile(c)).readFully(),java.nio.charset.StandardCharsets.UTF_8));return new State(j.getInt("v4"),j.getInt("v6"),Base64.getDecoder().decode(j.getString("token")),j.getString("revision"));}
 }
 @Override public void onCreate(){
  super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("residential","分身住宅代理",NotificationManager.IMPORTANCE_LOW));
  Notification n=new Notification.Builder(this,"residential").setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle("分身住宅代理").setContentText("TCP 经 VPN 联网；住宅代理按保存的设置启用").setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,NetworkSettingsActivity.class),PendingIntent.FLAG_IMMUTABLE)).build();startForeground(74302,n);
  try{
   config=NetworkOptions.residential(this)?ResidentialConfig.load(this):null;if(NetworkOptions.residential(this)&&(config==null||"AUTO".equals(config.protocol)))throw new IOException();
   token=new byte[32];new SecureRandom().nextBytes(token);
   v4=new ServerSocket(0,32,InetAddress.getByAddress(new byte[]{127,0,0,1}));
   byte[] loop6=new byte[16];loop6[15]=1;v6=new ServerSocket(0,32,InetAddress.getByAddress(loop6));running=true;
   callback=new ConnectivityManager.NetworkCallback(){@Override public void onLost(Network network){closeConnections();}@Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){if(!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))closeConnections();}};
   getSystemService(ConnectivityManager.class).registerDefaultNetworkCallback(callback);
   workers.execute(()->accept(v4));workers.execute(()->accept(v6));
   JSONObject j=new JSONObject().put("v4",v4.getLocalPort()).put("v6",v6.getLocalPort()).put("token",Base64.getEncoder().encodeToString(token)).put("revision",NetworkOptions.revision(this));
   AtomicFile a=new AtomicFile(stateFile(this));FileOutputStream out=null;try{out=a.startWrite();out.write(j.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));a.finishWrite(out);}catch(Exception e){if(out!=null)a.failWrite(out);throw e;}
  }catch(Exception e){android.util.Log.e("WhatsAppSpaces","residential_service_unavailable");stopSelf();}
 }
 @Override public int onStartCommand(Intent i,int f,int id){return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
 private void accept(ServerSocket listener){
  while(running){try{Socket client=listener.accept();if(!slots.tryAcquire()){client.close();continue;}sockets.add(client);try{workers.execute(()->session(client));}catch(RejectedExecutionException e){sockets.remove(client);client.close();slots.release();}}catch(IOException e){break;}}
 }
 private void session(Socket client){
  Socket remote=null;
  try{
   client.setSoTimeout(5000);client.setTcpNoDelay(true);byte[] h=new byte[55];new DataInputStream(client.getInputStream()).readFully(h);
   if(h[0]!='R'||h[1]!='S'||h[2]!='P'||h[3]!='1'||!MessageDigest.isEqual(token,Arrays.copyOfRange(h,4,36)))return;
   if(h[36]==0){client.getOutputStream().write(new byte[]{'O','K'});return;}
   if(h[36]!=1&&h[36]!=4)return;int port=((h[53]&255)<<8)|(h[54]&255);if(port==0)return;
   InetAddress target=InetAddress.getByAddress(Arrays.copyOfRange(h,37,h[36]==1?41:53));
   if(target.isAnyLocalAddress()||target.isLoopbackAddress()||target.isLinkLocalAddress()||target.isMulticastAddress())return;
   Network vpn=ResidentialTransport.vpn(this);remote=config==null?ResidentialTransport.direct(vpn,target.getHostAddress(),port):ResidentialTransport.tunnel(vpn,config,target.getHostAddress(),port);sockets.add(remote);
   // Close a socket opened across a VPN transition; it is already bound to the original network.
   if(!vpn.equals(ResidentialTransport.vpn(this)))return;
   client.setSoTimeout(0);remote.setSoTimeout(0);Socket upstream=remote;
   workers.execute(()->{try{copy(client.getInputStream(),upstream.getOutputStream());upstream.shutdownOutput();}catch(IOException e){close(client);close(upstream);}});
   copy(remote.getInputStream(),client.getOutputStream());client.shutdownOutput();
  }catch(Exception e){/* No addresses, credentials or raw exception messages in logs. */}
  finally{sockets.remove(client);close(client);if(remote!=null){sockets.remove(remote);close(remote);}slots.release();}
 }
 private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
 private static void close(Socket s){try{s.close();}catch(Exception ignored){}}
 private void closeConnections(){for(Socket s:sockets)close(s);}
 @Override public void onDestroy(){running=false;closeConnections();try{if(v4!=null)v4.close();if(v6!=null)v6.close();}catch(Exception ignored){}workers.shutdownNow();if(callback!=null)try{getSystemService(ConnectivityManager.class).unregisterNetworkCallback(callback);}catch(Exception ignored){}new AtomicFile(stateFile(this)).delete();super.onDestroy();}
 static void stop(Context c){c.stopService(new Intent(c,ResidentialService.class));}
 static void prepare(Context c)throws Exception{
  if(NetworkOptions.residential(c)){ResidentialConfig cfg=ResidentialConfig.load(c);if(cfg==null||"AUTO".equals(cfg.protocol))throw new IOException("请先检测住宅代理协议，或在设置中切换为仅使用 VPN");}
  ResidentialTransport.vpn(c);c.startForegroundService(new Intent(c,ResidentialService.class));
  for(int i=0;i<30;i++){try{State st=State.read(c);if(!st.revision.equals(NetworkOptions.revision(c)))throw new IOException();try(Socket check=new Socket()){check.connect(new InetSocketAddress("127.0.0.1",st.v4),200);check.setSoTimeout(300);byte[] h=new byte[55];System.arraycopy(new byte[]{'R','S','P','1'},0,h,0,4);System.arraycopy(st.token,0,h,4,32);check.getOutputStream().write(h);if(check.getInputStream().read()=='O'&&check.getInputStream().read()=='K')return;}}catch(Exception ignored){}Thread.sleep(100);}
  throw new IOException("住宅代理转发服务未就绪，已阻止打开分身");
 }
}
