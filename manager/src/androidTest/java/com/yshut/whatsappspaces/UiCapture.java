package com.yshut.whatsappspaces;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
/** Captures only this application's own view tree, excluding system UI and other windows. */
final class UiCapture {
 static void run(Instrumentation probe,Bundle result)throws Exception{
  Context c=probe.getTargetContext();Activity home=probe.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  // Drain the app worker used by its asynchronous home refresh before capturing.
  SpacesController.WORK.submit(()->{}).get(15,java.util.concurrent.TimeUnit.SECONDS);probe.waitForIdleSync();
  capture(probe,home,"ui-home.png");result.putBoolean("homeCaptured",true);
  Activity settings=probe.startActivitySync(new Intent(c,NetworkSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));probe.waitForIdleSync();capture(probe,settings,"ui-network.png");result.putBoolean("networkCaptured",true);
  probe.runOnMainSync(()->settings.finish());result.putBoolean("settingsChanged",false);
 }
 static void controls(Instrumentation probe,Bundle result)throws Exception{
  Context c=probe.getTargetContext();boolean prior=NetworkOptions.residential(c),geo=NetworkOptions.location(c);ResidentialConfig config=ResidentialConfig.load(c);if(config==null)throw new IOException("Saved proxy required for toggle check");
  Activity activity=probe.startActivitySync(new Intent(c,NetworkSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));probe.waitForIdleSync();
  Switch toggle=(Switch)field(activity,"residential");Button save=(Button)field(activity,"save"),test=(Button)field(activity,"test");
  try{
   probe.runOnMainSync(()->{toggle.setChecked(false);save.performClick();});waitWork(probe,activity);
   if(NetworkOptions.residential(c)||NetworkOptions.location(c)!=geo)throw new IOException("VPN mode was not saved");
   ResidentialConfig after=ResidentialConfig.load(c);if(after==null||!config.host.equals(after.host)||!config.user.equals(after.user)||!config.password.equals(after.password))throw new IOException("Saved profile changed");result.putBoolean("vpnOnlySaveAndCredentials",true);
   capture(probe,activity,"ui-vpn-only.png");
   probe.runOnMainSync(test::performClick);waitWork(probe,activity);String vpnText=((TextView)field(activity,"status")).getText().toString();result.putBoolean("vpnOnlyDetection",vpnText.contains("检测通过"));
   probe.runOnMainSync(()->{toggle.setChecked(prior);save.performClick();});waitWork(probe,activity);
   if(NetworkOptions.residential(c)!=prior||NetworkOptions.location(c)!=geo)throw new IOException("Original options not restored");result.putBoolean("originalOptionsRestored",true);
   probe.runOnMainSync(test::performClick);waitWork(probe,activity);String currentText=((TextView)field(activity,"status")).getText().toString();result.putBoolean("originalModeDetection",currentText.contains("检测通过"));
   capture(probe,activity,"ui-network-tested.png");
   probe.runOnMainSync(()->{ViewGroup content=activity.findViewById(android.R.id.content);((ScrollView)content.getChildAt(0)).fullScroll(View.FOCUS_DOWN);});probe.waitForIdleSync();capture(probe,activity,"ui-network-bottom.png");
  }finally{
   if(NetworkOptions.residential(c)!=prior||NetworkOptions.location(c)!=geo)NetworkSettingsActivity.apply(c,config,prior,geo,null);
   probe.runOnMainSync(activity::finish);
  }
 }
 static Object field(Object target,String name)throws Exception{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 static void waitWork(Instrumentation probe,Activity activity)throws Exception{SpacesController.WORK.submit(()->{}).get(90,java.util.concurrent.TimeUnit.SECONDS);probe.waitForIdleSync();if((Boolean)field(activity,"busy"))throw new IOException("UI still busy");}
 static void capture(Instrumentation probe,Activity activity,String name)throws Exception{
  AtomicReference<Exception> failure=new AtomicReference<>();probe.runOnMainSync(()->{try{
   View content=activity.findViewById(android.R.id.content);if(content==null||content.getWidth()<1||content.getHeight()<1)throw new IOException("Own content not laid out");
   Bitmap image=Bitmap.createBitmap(content.getWidth(),content.getHeight(),Bitmap.Config.ARGB_8888);content.draw(new Canvas(image));try(FileOutputStream out=new FileOutputStream(new File(activity.getFilesDir(),name))){if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG failed");}finally{image.recycle();}
  }catch(Exception e){failure.set(e);}});if(failure.get()!=null)throw failure.get();
 }
}
