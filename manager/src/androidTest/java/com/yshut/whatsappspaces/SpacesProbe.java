package com.yshut.whatsappspaces;
import android.app.Instrumentation;
import android.os.Bundle;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import top.niunaijun.blackbox.core.env.BEnvironment;

/** Device probe: operates only on fresh spaces owned by this test application. */
public final class SpacesProbe extends Instrumentation {
 private Bundle options;
 @Override public void onCreate(Bundle args){options=args==null?new Bundle():args;start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  String action=options.getString("action","inspect");
  if("startup-recovery".equals(action))StartupRecoveryProbe.run(getTargetContext(),result);
  if("stop-space".equals(action)){int id=options.getInt("space",1);if(!SpacesController.spaces().contains(id))throw new IllegalArgumentException("Unknown space");top.niunaijun.blackbox.BlackBoxCore.get().stopPackage(SpacesController.SOURCE,id);result.putInt("stoppedSpace",id);}
  if("space-management".equals(action))SpaceManagementProbe.run(getTargetContext(),result);
  if("ui-controls".equals(action))UiCapture.controls(this,result);
  if("ui-capture".equals(action))UiCapture.run(this,result);
  if("network-current-location".equals(action))NetworkProbe.currentLocation(getTargetContext(),result);
  if("network-vpn-location".equals(action))NetworkProbe.lookupVpnLocation(getTargetContext(),result);
  if("network-options".equals(action))NetworkProbe.options(getTargetContext(),result);
  if("network-fixture".equals(action))NetworkProbe.fixture(getTargetContext(),result);
  if("network-configure".equals(action))NetworkProbe.configure(getTargetContext(),result);
  if("network-diagnose".equals(action))NetworkProbe.diagnose(getTargetContext(),result);
  if("network-status".equals(action))NetworkProbe.status(getTargetContext(),result);
  if("network-detect".equals(action))NetworkProbe.detect(getTargetContext(),result);
  if("create-two".equals(action)){
   List<Integer> prior=SpacesController.spaces();if(!prior.isEmpty())throw new Exception("Probe requires an empty test container; existing users are left untouched");
   int first=SpacesController.add(getTargetContext()),second=SpacesController.add(getTargetContext());
   File one=BEnvironment.getDataDir(SpacesController.SOURCE,first),two=BEnvironment.getDataDir(SpacesController.SOURCE,second);
   if(one.getCanonicalPath().equals(two.getCanonicalPath()))throw new Exception("Space directories overlap");
   File markerOne=new File(one,".spaces-probe-marker"),markerTwo=new File(two,".spaces-probe-marker");
   Files.write(markerOne.toPath(),("space-"+first).getBytes(StandardCharsets.UTF_8));Files.write(markerTwo.toPath(),("space-"+second).getBytes(StandardCharsets.UTF_8));
   SpacesController.sync(getTargetContext(),true);
   if(!("space-"+first).equals(new String(Files.readAllBytes(markerOne.toPath()),StandardCharsets.UTF_8))||!("space-"+second).equals(new String(Files.readAllBytes(markerTwo.toPath()),StandardCharsets.UTF_8)))throw new Exception("Refresh did not preserve test markers");
   result.putBoolean("distinctDirectories",true);result.putBoolean("refreshPreservedMarkers",true);result.putString("spaceIds",first+","+second);
  }
  result.putString("spaces",SpacesController.spaces().toString());result.putString("initializationError",SpacesApplication.initializationError);result.putString("status","PASS");finish(-1,result);
 }catch(Throwable error){result.putString("status","FAIL");result.putString("error",error.getClass().getSimpleName()+": "+error.getMessage());android.util.Log.e("WhatsAppSpaces","probe_failed",error);finish(0,result);}}
}