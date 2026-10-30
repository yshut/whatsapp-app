package com.yshut.whatsappspaces;

import android.app.Application;
import android.content.*;
import android.os.Build;
import android.util.Log;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;

public final class SpacesApplication extends Application {
 public static volatile String initializationError;
 private boolean relayProcess,requiresResidential;
 private ResidentialService.State residentialState;
 @Override protected void attachBaseContext(Context base){
  super.attachBaseContext(base);
  relayProcess=Application.getProcessName().endsWith(":residential");
  if(relayProcess)return;
  requiresResidential=true; // Every guest uses the VPN-bound relay, with an optional residential hop.
  if(requiresResidential)try{residentialState=ResidentialService.State.read(base);}catch(Exception ignored){}
  try{
   BlackBoxCore.get().doAttachBaseContext(base,new ClientConfiguration(){
    public String getHostPackageName(){return "com.yshut.whatsappspaces";}
    public String getLogSenderChatId(){return null;}
    public boolean isEnableDaemonService(){return false;}
    public boolean isUseVpnNetwork(){return false;}
    public boolean isDisableFlagSecure(){return false;}
   });
   BlackBoxCore.get().addAppLifecycleCallback(new top.niunaijun.blackbox.app.configuration.AppLifecycleCallback(){
    @Override public void beforeBindApplication(Context hostContext){
     // Read before guest filesystem redirection, at bind time rather than process preload time.
     try{residentialState=ResidentialService.State.read(hostContext);}catch(Exception e){residentialState=null;}
    }
    @Override public void afterApplicationOnCreate(String pkg,String process,Application app,int userId){
     app.registerActivityLifecycleCallbacks(new top.niunaijun.blackbox.app.configuration.AppLifecycleCallback(){
      @Override public void onActivityResumed(android.app.Activity activity){
       Intent event=new Intent("com.yshut.whatsappspaces.SPACE_RESUMED").setPackage("com.yshut.whatsappspaces").putExtra("space",userId).putExtra("at",android.os.SystemClock.elapsedRealtime());
       activity.sendBroadcast(event);Log.i("WhatsAppSpaces","space_activity_resumed space="+userId);
      }
     });
    }
    @Override public void beforeCreateApplication(String pkg,String process,Context context,int userId){
     if(!requiresResidential)return;
     boolean installed=false;
     try{if(residentialState!=null)installed=top.niunaijun.blackbox.core.network.SocketGate.install(residentialState.v4,residentialState.v6,residentialState.token);}catch(Throwable ignored){}
     if(!installed){Log.e("WhatsAppSpaces","residential_gate_failed_closed");android.os.Process.killProcess(android.os.Process.myPid());throw new IllegalStateException("Residential network gate unavailable");}
     Log.i("WhatsAppSpaces","residential_tcp_gate_ready space="+userId);
    }
   });
  }catch(Throwable error){initializationError=error.getClass().getSimpleName()+": "+error.getMessage();Log.e("WhatsAppSpaces","engine_attach_failed",error);}
 }
 @Override public void onCreate(){
  super.onCreate();
  if(relayProcess)return;
  if(initializationError!=null)return;
  try{
   BlackBoxCore.get().doCreate();
   if(BlackBoxCore.get().isMainProcess()){
    IntentFilter f=new IntentFilter(Intent.ACTION_PACKAGE_REPLACED);f.addDataScheme("package");
    if(Build.VERSION.SDK_INT>=33)registerReceiver(new SourceChangedReceiver(),f,Context.RECEIVER_EXPORTED);else registerReceiver(new SourceChangedReceiver(),f);
    SpacesController.schedule(this);
    Log.i("WhatsAppSpaces","host_ready");
   }
  }catch(Throwable error){initializationError=error.getClass().getSimpleName()+": "+error.getMessage();Log.e("WhatsAppSpaces","engine_create_failed",error);}
 }
}