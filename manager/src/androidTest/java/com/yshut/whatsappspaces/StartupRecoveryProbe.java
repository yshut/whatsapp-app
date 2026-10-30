package com.yshut.whatsappspaces;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.app.dispatcher.AppServiceDispatcher;
import top.niunaijun.blackbox.core.system.ProcessRecord;
import top.niunaijun.blackbox.core.system.am.ActivityRecord;
import top.niunaijun.blackbox.core.system.am.TaskRecord;
import top.niunaijun.blackbox.fake.frameworks.BlackManager;
import top.niunaijun.blackbox.proxy.record.ProxyServiceRecord;

/** Synthetic failure cases plus read-only validation of existing virtual package metadata. */
final class StartupRecoveryProbe {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static Field field(String name)throws Exception {Field f=BlackManager.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void run(Context context,Bundle result)throws Exception {
  List<Integer> before=new ArrayList<>(SpacesController.spaces());
  check(BActivityThread.getAppConfig()==null,"Probe must run in host process");
  check(ProxyServiceRecord.create(null).mServiceInfo==null,"Null redelivery should be empty");
  check(AppServiceDispatcher.get().onStartCommand(null,0,1)==Service.START_NOT_STICKY,"Null redelivery should stop");
  ServiceInfo info=new ServiceInfo();info.packageName=SpacesController.SOURCE;info.name="test.StaleService";
  Intent stale=new Intent();ProxyServiceRecord.saveStub(stale,new Intent("test.stale"),info,null,1,1);
  check(AppServiceDispatcher.get().onStartCommand(stale,0,1)==Service.START_NOT_STICKY,"Unassigned process must ignore stale service");
  result.putBoolean("staleAndNullServiceIgnored",true);
  BlackManager<?> manager=BlackBoxCore.getBPackageManager();
  ((AtomicBoolean)field("mServiceCreationFailed").get(manager)).set(true);
  field("mLastRetryTime").setLong(manager,System.currentTimeMillis());field("mLastServiceCreationTime").setLong(manager,System.currentTimeMillis());
  manager.clearServiceCache();
  check(!((AtomicBoolean)field("mServiceCreationFailed").get(manager)).get()&&field("mLastRetryTime").getLong(manager)==0&&field("mLastServiceCreationTime").getLong(manager)==0,"Cache reset must permit immediate recovery");
  result.putBoolean("cacheFailureReset",true);
  for(int id:before)check(BlackBoxCore.getBPackageManager().getPackageInfoForBinding(SpacesController.SOURCE,id)!=null,"Real package metadata unavailable for space "+id);
  check(BlackBoxCore.getBPackageManager().getPackageInfoForBinding("invalid.missing.startup.probe",1)==null,"Missing package must not get fabricated metadata");
  check(BlackBoxCore.getBPackageManager().getPackageInfoForBinding(SpacesController.SOURCE,99999)==null,"Missing user must not borrow installed package metadata");
  result.putBoolean("strictPackageMetadata",true);
  TaskRecord task=new TaskRecord(-1,1,"test");ActivityRecord dead=new ActivityRecord();dead.processRecord=new ProcessRecord(new ApplicationInfo(),"test.dead");task.activities.add(dead);
  check(task.needNewTask()&&task.getTopActivityRecord()==null,"Dead process task must be replaced");
  ActivityRecord pending=new ActivityRecord();task.activities.add(pending);
  check(!task.needNewTask()&&task.getTopActivityRecord()==pending,"Unassigned pending activity must remain");
  result.putBoolean("deadTaskRecovery",true);
  check(before.equals(SpacesController.spaces()),"Space inventory changed");result.putBoolean("existingSpacesPreserved",true);
 }
}
