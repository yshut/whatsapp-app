package com.yshut.whatsappspaces;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;

/** Deletes only disposable spaces created by this invocation. Never selects an existing space for deletion. */
final class SpaceManagementProbe {
 static void require(boolean value,String text)throws Exception{if(!value)throw new Exception(text);}
 static void run(Context c,Bundle out)throws Exception{
  List<Integer> prior=SpacesController.spaces();String synced=SpacesController.prefs(c).getString("synced",null);
  require(prior.isEmpty()||SpacesController.fingerprint(SpacesController.source(c)).equals(synced),"Source changed: skip probe to avoid syncing existing spaces");
  Map<Integer,String> names=new HashMap<>();for(int id:prior)names.put(id,SpacesController.name(c,id));String status=SpacesController.prefs(c).getString("status","");
  int temporary=-1;String oldToken=null;String shortcut=null;ShortcutManager manager=c.getSystemService(ShortcutManager.class);
  try{
   boolean rejected=false;try{SpacesController.validName(" \n ");}catch(IllegalArgumentException e){rejected=true;}require(rejected,"Empty name accepted");rejected=false;try{SpacesController.validName("x".repeat(25));}catch(IllegalArgumentException e){rejected=true;}require(rejected,"Long name accepted");rejected=false;try{SpacesController.validName("one\ntwo");}catch(IllegalArgumentException e){rejected=true;}require(rejected,"Newline accepted");out.putBoolean("nameValidation",true);
   temporary=SpacesController.add(c);require(!prior.contains(temporary),"Refusing existing space");int first=temporary;oldToken=SpacesController.generation(c,first);shortcut=SpacesController.shortcutId(c,first);
   File marker=new File(BEnvironment.getDataDir(SpacesController.SOURCE,first),".management-probe");Files.write(marker.toPath(),new byte[]{17,23});
   File external=new File(BEnvironment.getExternalUserDir(first),".management-probe");external.getParentFile().mkdirs();Files.write(external.toPath(),new byte[]{17,23});
   if(manager!=null)require(manager.addDynamicShortcuts(Collections.singletonList(new ShortcutInfo.Builder(c,shortcut).setShortLabel("Probe").setIntent(SpacesController.shortcutIntent(c,first)).build())),"Dynamic shortcut setup failed");
   SpacesController.rename(c,first," 临时测试空间 ");require("临时测试空间".equals(SpacesController.name(c,first)),"Rename not saved");require(marker.exists(),"Rename touched data");
   if(manager!=null){boolean found=false;for(ShortcutInfo item:manager.getDynamicShortcuts())if(item.getId().equals(shortcut))found="临时测试空间".contentEquals(item.getShortLabel());require(found,"Shortcut rename not applied");}out.putBoolean("renameAndShortcutUpdate",true);
   SpacesController.remove(c,first);temporary=-1;require(!marker.exists()&&!external.exists()&&!SpacesController.spaces().contains(first),"Delete did not remove data");require(!SpacesController.shortcutValid(c,first,oldToken),"Deleted shortcut accepted");out.putBoolean("deleteScopedData",true);
   temporary=SpacesController.add(c);require(!prior.contains(temporary),"Refusing existing space");require(temporary==first,"Expected freed slot reuse");require(!oldToken.equals(SpacesController.generation(c,temporary)),"Generation reused");require(!SpacesController.shortcutValid(c,temporary,oldToken)&&!SpacesController.shortcutValid(c,temporary,null),"Stale shortcut targets replacement");require(("WhatsApp "+temporary).equals(SpacesController.name(c,temporary)),"Old name leaked to replacement");out.putBoolean("staleShortcutsRejected",true);
   SpacesController.remove(c,temporary);temporary=-1;
  }finally{
   if(temporary>0&&!prior.contains(temporary))SpacesController.remove(c,temporary);
   if(manager!=null&&shortcut!=null)manager.removeDynamicShortcuts(Collections.singletonList(shortcut));
   SpacesController.status(c,status);
  }
  require(prior.equals(SpacesController.spaces()),"Existing space list changed");for(int id:prior){require(names.get(id).equals(SpacesController.name(c,id)),"Existing name changed");require(BEnvironment.getDataDir(SpacesController.SOURCE,id).isDirectory(),"Existing data directory missing");}out.putBoolean("existingSpacesPreserved",true);
 }
}
