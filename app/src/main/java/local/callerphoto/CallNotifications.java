package local.callerphoto;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Person;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.*;

/** Notification access is system-wide; discard unrelated packages before reading extras. */
public class CallNotifications extends NotificationListenerService {
    static CallNotifications instance;
    private final Map<String,StatusBarNotification> calls=new LinkedHashMap<>();
    private boolean isCall(StatusBarNotification sbn){
        return ("com.android.incallui".equals(sbn.getPackageName()) ||
                "com.android.contacts".equals(sbn.getPackageName())) &&
                Notification.CATEGORY_CALL.equals(sbn.getNotification().category);
    }
    @Override public void onListenerConnected(){
        instance=this;calls.clear();
        try{StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification s:active)if(isCall(s))calls.put(s.getKey(),s);}catch(SecurityException ignored){}
        wake();
    }
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(!isCall(sbn))return;
        calls.put(sbn.getKey(),sbn);
        android.util.Log.i("CallerPhoto","Xiaomi call notification received");wake();
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn){if(calls.remove(sbn.getKey())!=null)wake();}
    static void reconnectIfAllowed(Context context){
        ComponentName component=new ComponentName(context,CallNotifications.class);
        if(instance==null && context.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(component)){
            try{requestRebind(component);}catch(RuntimeException e){android.util.Log.w("CallerPhoto","Notification reconnect unavailable",e);}
        }
    }
    @Override public void onListenerDisconnected(){instance=null;calls.clear();reconnectIfAllowed(this);}
    @Override public void onDestroy(){instance=null;calls.clear();super.onDestroy();}
    private void wake(){if(PhotoService.instance!=null)PhotoService.instance.checkSoon();}
    List<String> candidates(){
        List<String> result=new ArrayList<>();
        for(StatusBarNotification sbn:calls.values()){
            Notification n=sbn.getNotification();
            Person person=n.extras.getParcelable("android.callPerson");
            if(person!=null){
                String uri=person.getUri();if(uri!=null && uri.startsWith("tel:"))result.add(android.net.Uri.decode(uri.substring(4)));
                if(person.getName()!=null)result.add(person.getName().toString());
            }
            CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
            if(title!=null)result.add(title.toString().trim());
        }
        return result;
    }
    void openCallScreen(){
        for(StatusBarNotification sbn:calls.values()){
            PendingIntent intent=sbn.getNotification().contentIntent;
            if(intent!=null){try{intent.send();}catch(PendingIntent.CanceledException ignored){}return;}
        }
    }
}
