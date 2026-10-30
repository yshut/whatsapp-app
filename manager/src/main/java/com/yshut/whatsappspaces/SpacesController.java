package com.yshut.whatsappspaces;

import android.app.job.*;
import android.content.*;
import android.content.pm.*;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import top.niunaijun.blackbox.core.env.BEnvironment;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.user.BUserInfo;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public final class SpacesController {
 public static final String SOURCE="com.whatsapp";
 public static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 private static final int JOB=74301;
 public static SharedPreferences prefs(Context c){return c.getSharedPreferences("spaces",0);}
 public static String fingerprint(PackageInfo p){return p.getLongVersionCode()+"@"+p.lastUpdateTime+"@"+p.applicationInfo.sourceDir;}
 public static PackageInfo source(Context c)throws Exception{
  PackageInfo p=c.getPackageManager().getPackageInfo(SOURCE,PackageManager.GET_SIGNING_CERTIFICATES);
  if(p.applicationInfo==null||!new File(p.applicationInfo.sourceDir).isFile())throw new Exception("请先安装可用的原版 WhatsApp");
  List<String> signers=new ArrayList<>();
  if(p.signingInfo==null)throw new Exception("无法核验原版 WhatsApp 签名");
  for(Signature signature:p.signingInfo.getApkContentsSigners()){
   byte[] hash=MessageDigest.getInstance("SHA-256").digest(signature.toByteArray());StringBuilder value=new StringBuilder();for(byte b:hash)value.append(String.format(Locale.ROOT,"%02x",b&255));signers.add(value.toString());
  }
  if(signers.isEmpty())throw new Exception("原版 WhatsApp 没有可核验签名");
  Collections.sort(signers);String current=signers.toString(),known=prefs(c).getString("sourceSigner",null);
  if(known!=null&&!known.equals(current))throw new Exception("原版 WhatsApp 签名已变化，更新已暂停");
  if(known==null)prefs(c).edit().putString("sourceSigner",current).apply();
  return p;
 }
 public static synchronized List<Integer> spaces(){
  List<Integer> out=new ArrayList<>();
  for(BUserInfo user:BlackBoxCore.get().getUsers())if(user.id>=1&&user.id<=32&&BlackBoxCore.get().isInstalled(SOURCE,user.id))out.add(user.id);
  for(int id=1;id<=32;id++)if(deleting(BlackBoxCore.getContext(),id)&&!out.contains(id))out.add(id);
  Collections.sort(out);return out;
 }
 public static String name(Context c,int id){return prefs(c).getString("spaceName."+id,"WhatsApp "+id);}
 public static boolean deleting(Context c,int id){return prefs(c).getBoolean("deleting."+id,false);}
 public static String validName(String raw){String value=raw==null?"":raw.trim();if(value.isEmpty()||value.codePointCount(0,value.length())>24)throw new IllegalArgumentException("请输入 1–24 个字符的名称");for(int i=0;i<value.length();){int code=value.codePointAt(i);if(Character.isISOControl(code)||Character.getType(code)==Character.FORMAT)throw new IllegalArgumentException("名称不能包含换行或隐藏控制字符");i+=Character.charCount(code);}return value;}
 private static void requireSpace(Context c,int id)throws Exception{ready();if(id<1||id>32||deleting(c,id)||!BlackBoxCore.get().isInstalled(SOURCE,id))throw new IOException("该空间已删除或正在删除，请刷新列表");}
 public static synchronized String generation(Context c,int id){String key="spaceGeneration."+id,value=prefs(c).getString(key,null);if(value==null){value=UUID.randomUUID().toString();if(!prefs(c).edit().putString(key,value).commit())throw new IllegalStateException("空间标识保存失败");}return value;}
 public static boolean shortcutValid(Context c,int id,String generation){if(id<1||id>32||deleting(c,id)||!BlackBoxCore.get().isInstalled(SOURCE,id))return false;String current=prefs(c).getString("spaceGeneration."+id,null);return generation==null?!prefs(c).getBoolean("requireGeneration."+id,false):generation.equals(current);}
 public static String shortcutId(Context c,int id){return "whatsapp-space-"+id+"-"+generation(c,id);}
 public static Intent shortcutIntent(Context c,int id){return new Intent(c,MainActivity.class).setAction(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("open_space",id).putExtra("space_generation",generation(c,id));}
 private static List<ShortcutInfo> shortcuts(Context c,int id){ShortcutManager m=c.getSystemService(ShortcutManager.class);List<ShortcutInfo> list=new ArrayList<>();if(m==null)return list;Map<String,ShortcutInfo> all=new HashMap<>();for(ShortcutInfo item:m.getPinnedShortcuts())all.put(item.getId(),item);for(ShortcutInfo item:m.getDynamicShortcuts())all.put(item.getId(),item);for(ShortcutInfo item:all.values()){Intent intent=item.getIntent();if(intent!=null&&intent.getComponent()!=null&&c.getPackageName().equals(intent.getComponent().getPackageName())&&intent.getIntExtra("open_space",-1)==id)list.add(item);}return list;}
 public static synchronized void rename(Context c,int id,String raw)throws Exception{
  requireSpace(c,id);String value=validName(raw);if(!prefs(c).edit().putString("spaceName."+id,value).commit())throw new IOException("名称保存失败，请重试");boolean updated=true;
  try{List<ShortcutInfo> changes=new ArrayList<>();for(ShortcutInfo item:shortcuts(c,id))if(shortcutValid(c,id,item.getIntent().getStringExtra("space_generation")))changes.add(new ShortcutInfo.Builder(c,item.getId()).setShortLabel(value).setLongLabel(value).setIntent(shortcutIntent(c,id)).build());if(!changes.isEmpty())updated=c.getSystemService(ShortcutManager.class).updateShortcuts(changes);}catch(Exception e){updated=false;}
  status(c,updated?"名称已保存":"名称已保存；桌面名称未更新，请重新添加快捷方式");
 }
 public static synchronized void remove(Context c,int id)throws Exception{
  ready();if(id<1||id>32||(!deleting(c,id)&&!BlackBoxCore.get().isInstalled(SOURCE,id)))throw new IOException("该空间不存在，请刷新列表");
  if(!prefs(c).edit().putBoolean("deleting."+id,true).putBoolean("requireGeneration."+id,true).commit())throw new IOException("无法保存删除状态，操作已取消");
  boolean shortcutDone=true;try{List<String> ids=new ArrayList<>();for(ShortcutInfo item:shortcuts(c,id))ids.add(item.getId());if(!ids.isEmpty()){ShortcutManager m=c.getSystemService(ShortcutManager.class);m.disableShortcuts(ids,"这个空间已删除，请从多开空间重新选择");m.removeDynamicShortcuts(ids);}}catch(Exception e){shortcutDone=false;}
  BlackBoxCore.get().stopPackage(SOURCE,id);BlackBoxCore.get().deleteUser(id);
  boolean userRemains=false;for(BUserInfo u:BlackBoxCore.get().getUsers())if(u.id==id)userRemains=true;
  if(userRemains||BlackBoxCore.get().isInstalled(SOURCE,id)||BEnvironment.getUserDir(id).exists()||BEnvironment.getDeDataDir(SOURCE,id).exists()||BEnvironment.getExternalUserDir(id).exists())throw new IOException("空间清理尚未完成，可在菜单中再次删除");
  top.niunaijun.blackbox.fake.frameworks.BLocationManager.get().setPattern(id,SOURCE,0);
  if(!prefs(c).edit().remove("spaceName."+id).remove("spaceGeneration."+id).remove("deleting."+id).commit())throw new IOException("删除状态保存失败，请再次删除以完成清理");
  status(c,shortcutDone?"空间已删除，本地数据已清除":"空间已删除；旧桌面图标可能仍显示，可手动移除");
 }
 private static void ready()throws Exception{if(SpacesApplication.initializationError!=null)throw new Exception("分身引擎初始化失败："+SpacesApplication.initializationError);}
 private static void install(int id,long version)throws Exception{
  InstallResult result=BlackBoxCore.get().installPackageAsUser(SOURCE,id);
  if(result==null||!result.success||!SOURCE.equals(result.packageName)||!BlackBoxCore.get().isInstalled(SOURCE,id))throw new Exception("分身创建失败："+(result==null?"引擎无返回":result.msg));
  PackageInfo imported=BlackBoxCore.getBPackageManager().getPackageInfo(SOURCE,0,id);
  if(imported==null||imported.getLongVersionCode()!=version)throw new Exception("分身版本核验失败");
 }
 public static synchronized int add(Context c)throws Exception{
  ready();PackageInfo source=source(c);List<Integer> existing=spaces();
  if(!existing.isEmpty()&&!fingerprint(source).equals(prefs(c).getString("synced",null)))sync(c,true);
  Set<Integer> used=new HashSet<>(existing);for(BUserInfo user:BlackBoxCore.get().getUsers())used.add(user.id);
  int next=1;while(used.contains(next)&&next<=32)next++;
  if(next>32)throw new Exception("当前版本最多创建 32 个空间");
  String before=fingerprint(source);
  if(!prefs(c).edit().remove("spaceName."+next).putString("spaceGeneration."+next,UUID.randomUUID().toString()).commit())throw new IOException("空间信息保存失败");
  install(next,source.getLongVersionCode());
  if(!before.equals(fingerprint(source(c))))throw new Exception("创建期间原应用发生更新，请点同步更新后再打开分身");
  prefs(c).edit().putString("synced",before).apply();status(c,"已创建 WhatsApp "+next+"，请打开并登录");
  Log.i("WhatsAppSpaces","space_created id="+next+" version="+source.getLongVersionCode());return next;
 }
 public static synchronized boolean sync(Context c,boolean force)throws Exception{
  ready();PackageInfo source=source(c);String before=fingerprint(source);List<Integer> ids=spaces();ids.removeIf(id->deleting(c,id));
  if(ids.isEmpty())return false;
  if(!force&&before.equals(prefs(c).getString("synced",null)))return false;
  for(int id:ids)BlackBoxCore.get().stopPackage(SOURCE,id);
  for(int id:ids)install(id,source.getLongVersionCode());
  if(!before.equals(fingerprint(source(c))))throw new Exception("同步期间原应用再次更新，等待下一次同步");
  prefs(c).edit().putString("synced",before).apply();status(c,"已同步 "+ids.size()+" 个空间到 "+source.versionName);
  Log.i("WhatsAppSpaces","spaces_synced count="+ids.size()+" version="+source.getLongVersionCode());return true;
 }
 public static boolean open(int id){return id>=1&&id<=32&&!deleting(BlackBoxCore.getContext(),id)&&BlackBoxCore.get().isInstalled(SOURCE,id)&&BlackBoxCore.get().launchApk(SOURCE,id);}
 public static void status(Context c,String message){prefs(c).edit().putString("status",message).apply();}
 public static void check(Context c){if(!prefs(c).getBoolean("autoSync",false))return;WORK.execute(()->{try{sync(c,false);}catch(Exception e){status(c,"同步暂停："+e.getMessage());Log.w("WhatsAppSpaces","sync_failed "+e.getClass().getSimpleName());}});}
 public static void schedule(Context c){((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(new JobInfo.Builder(JOB,new ComponentName(c,SyncJob.class)).setPeriodic(15*60*1000L).setPersisted(true).build());}
 private SpacesController(){}
}