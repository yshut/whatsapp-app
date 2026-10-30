package com.yshut.whatsappspaces;
import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import org.json.JSONObject;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

final class ResidentialConfig {
 final String host,user,password,protocol;final int port;
 ResidentialConfig(String h,int p,String u,String pw,String proto){
  host=h.trim();port=p;user=u;password=pw;protocol=proto;
  if(host.isEmpty()||host.length()>253||host.matches(".*[\\s/\\r\\n].*")||port<1||port>65535||user.indexOf(':')>=0||user.contains("\r")||user.contains("\n")||password.contains("\r")||password.contains("\n")||!Arrays.asList("AUTO","SOCKS5","HTTP","HTTPS").contains(protocol))throw new IllegalArgumentException("代理格式不正确");
  if(user.getBytes(StandardCharsets.UTF_8).length>255||password.getBytes(StandardCharsets.UTF_8).length>255)throw new IllegalArgumentException("账号或密码过长");
 }
 static ResidentialConfig parse(String text,String proto){
  String t=text.trim();String host;int cut;
  if(t.startsWith("[")){cut=t.indexOf("]:");if(cut<0)throw new IllegalArgumentException("地址格式不正确");host=t.substring(1,cut);t=t.substring(cut+2);}else{cut=t.indexOf(':');if(cut<1)throw new IllegalArgumentException("请填写 地址:端口:用户名:密码");host=t.substring(0,cut);t=t.substring(cut+1);}
  String[] fields=t.split(":",3);if(fields.length!=3)throw new IllegalArgumentException("请填写 地址:端口:用户名:密码");
  return new ResidentialConfig(host,Integer.parseInt(fields[0]),fields[1],fields[2],proto);
 }
 static File marker(Context c){return new File(c.getNoBackupFilesDir(),"residential-required");}
 static boolean required(Context c){return marker(c).exists();}
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getNoBackupFilesDir(),"residential-config.enc"));}
 private static javax.crypto.SecretKey key()throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);String alias="whatsapp-spaces-residential-v1";
  if(!ks.containsAlias(alias)){KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}
  return (javax.crypto.SecretKey)ks.getKey(alias,null);
 }
 static String revision(Context c)throws Exception{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(file(c).readFully()));}
 static ResidentialConfig load(Context c)throws Exception{
  if(!required(c))return null;
  byte[] packed=file(c).readFully();if(packed.length<29||packed[0]!=1)throw new IOException("代理配置损坏，请重新保存");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOfRange(packed,1,13)));
  JSONObject j=new JSONObject(new String(cipher.doFinal(packed,13,packed.length-13),StandardCharsets.UTF_8));
  return new ResidentialConfig(j.getString("host"),j.getInt("port"),j.getString("user"),j.getString("password"),j.getString("protocol"));
 }
 void save(Context c)throws Exception{
  JSONObject j=new JSONObject();j.put("host",host).put("port",port).put("user",user).put("password",password).put("protocol",protocol);
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());byte[] body=cipher.doFinal(j.toString().getBytes(StandardCharsets.UTF_8));
  if(!marker(c).exists()&&!marker(c).createNewFile())throw new IOException("无法启用住宅代理");
  AtomicFile a=file(c);FileOutputStream out=null;try{out=a.startWrite();out.write(1);out.write(cipher.getIV());out.write(body);a.finishWrite(out);}catch(Exception ex){if(out!=null)a.failWrite(out);throw new IOException("代理配置保存失败");}
 }
}
