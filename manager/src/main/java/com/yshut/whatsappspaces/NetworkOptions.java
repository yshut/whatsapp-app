package com.yshut.whatsappspaces;
import android.content.*;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class NetworkOptions {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("network-options",0);}
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getNoBackupFilesDir(),"network-options.json"));}
 // Read the atomic file every time: the relay lives in a separate process; SharedPreferences caches are not coherent across processes.
 private static JSONObject read(Context c){try{return new JSONObject(new String(file(c).readFully(),StandardCharsets.UTF_8));}catch(FileNotFoundException e){return new JSONObject();}catch(Exception e){try{return new JSONObject().put("residential",true).put("ip-location",true);}catch(Exception impossible){throw new IllegalStateException(impossible);}}}
 static boolean residential(Context c){return read(c).optBoolean("residential",ResidentialConfig.required(c));}
 static boolean location(Context c){return read(c).optBoolean("ip-location",false);}
 static String revision(Context c)throws Exception{return residential(c)?"PROXY:"+ResidentialConfig.revision(c):"VPN_ONLY";}
 static void save(Context c,boolean residential,boolean location)throws IOException{
  AtomicFile a=file(c);FileOutputStream out=null;try{byte[] bytes=new JSONObject().put("residential",residential).put("ip-location",location).toString().getBytes(StandardCharsets.UTF_8);out=a.startWrite();out.write(bytes);a.finishWrite(out);}catch(Exception e){if(out!=null)a.failWrite(out);throw new IOException("设置保存失败");}
 }
 private NetworkOptions(){}
}
