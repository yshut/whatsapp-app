package com.yshut.whatsappspaces;
import android.app.job.*;
public final class SyncJob extends JobService {
 @Override public boolean onStartJob(JobParameters p){if(!SpacesController.prefs(this).getBoolean("autoSync",false))return false;SpacesController.WORK.execute(()->{try{SpacesController.sync(this,false);}catch(Exception e){SpacesController.status(this,"同步暂停："+e.getMessage());}finally{jobFinished(p,false);}});return true;}
 @Override public boolean onStopJob(JobParameters p){return true;}
}