package com.yshut.whatsappspaces;
import android.content.*;
public final class SourceChangedReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){String action=i.getAction();if(Intent.ACTION_PACKAGE_REPLACED.equals(action)){if(i.getData()!=null&&SpacesController.SOURCE.equals(i.getData().getSchemeSpecificPart()))SpacesController.check(c);}else if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){SpacesController.schedule(c);SpacesController.check(c);}}
}