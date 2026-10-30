package top.niunaijun.blackbox.core.system.am;

import android.content.Intent;

import java.util.LinkedList;
import java.util.List;


public class TaskRecord {
    public int id;
    public int userId;
    public String taskAffinity;
    public Intent rootIntent;
    public final List<ActivityRecord> activities = new LinkedList<>();

    public TaskRecord(int id, int userId, String taskAffinity) {
        this.id = id;
        this.userId = userId;
        this.taskAffinity = taskAffinity;
    }

    /** A killed guest can leave task records behind until the system delivers lifecycle cleanup. */
    public void pruneDeadActivities() {
        synchronized(activities) {
            for(ActivityRecord activity:activities) {
                top.niunaijun.blackbox.core.system.ProcessRecord process=activity.processRecord;
                if(!activity.finished && process!=null && (process.bActivityThread==null || !process.bActivityThread.asBinder().isBinderAlive())) activity.finished=true;
            }
        }
    }
    public boolean needNewTask() {
        pruneDeadActivities();
        for (ActivityRecord activity : activities) {
            if (!activity.finished) {
                return false;
            }
        }
        return true;
    }

    public void addTopActivity(ActivityRecord record) {
        activities.add(record);
    }

    public void removeActivity(ActivityRecord record) {
        activities.remove(record);
    }

    public ActivityRecord getTopActivityRecord() {
        pruneDeadActivities();
        for (int i = activities.size() - 1; i >= 0; i--) {
            ActivityRecord activityRecord = activities.get(i);
            if (!activityRecord.finished) {
                return activityRecord;
            }
        }
        return null;
    }
}
