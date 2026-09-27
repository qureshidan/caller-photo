package local.callerphoto;

import android.Manifest;
import android.app.Notification;
import android.accessibilityservice.AccessibilityService;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.telecom.TelecomManager;
import android.view.*;
import android.view.accessibility.*;
import java.util.*;
import java.util.concurrent.*;

public class PhotoService extends AccessibilityService {
    static PhotoService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private PhotoPanel overlay;
    private boolean inCall,shown,loading,stopped;
    private int generation,idleTicks;
    private long nextLookup;
    private String notificationCaller;
    private long notificationAt;
    private final Runnable tick=new Runnable(){public void run(){
        if(stopped)return;
        try{inspect();}catch(Exception e){hide();}
        handler.postDelayed(this,700);
    }};
    @Override protected void onServiceConnected(){instance=this;handler.post(tick);}
    void checkSoon(){if(!stopped){handler.removeCallbacks(tick);handler.post(tick);}}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event.getEventType()==AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                ("com.android.incallui".contentEquals(event.getPackageName()==null?"":event.getPackageName()) ||
                 "com.android.contacts".contentEquals(event.getPackageName()==null?"":event.getPackageName())) &&
                event.getParcelableData() instanceof Notification){
            Notification n=(Notification)event.getParcelableData();
            if(Notification.CATEGORY_CALL.equals(n.category)){
                CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
                if(title!=null){notificationCaller=title.toString().trim();notificationAt=SystemClock.uptimeMillis();}
            }
        }
        // Polling is also needed because some Xiaomi call-state changes have no UI event.
        if(!stopped){handler.removeCallbacks(tick);handler.postDelayed(tick,150);}
    }
    private void inspect(){
        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED ||
           checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){hide();return;}
        boolean active=getSystemService(TelecomManager.class).isInCall();
        if(!active){
            hide();
            if(++idleTicks>=2 && inCall){inCall=false;shown=false;generation++;notificationCaller=null;}
            return;
        }
        idleTicks=0;
        if(!inCall){inCall=true;shown=false;generation++;}
        if(!getSharedPreferences("MainActivity",0).getBoolean("enabled",true)){hide();return;}
            if(shown || loading || SystemClock.uptimeMillis()<nextLookup)return;
            List<String> candidates=callCandidates();
            if(candidates.isEmpty())return;
            loading=true;nextLookup=SystemClock.uptimeMillis()+1600;
            int token=generation;
            worker.execute(()->{
                Photos.Contact found=null; Bitmap bitmap=null;
                try{for(String s:candidates){found=Photos.match(this,s);if(found!=null){bitmap=Photos.bitmap(this,found);if(bitmap!=null)break;}}}catch(Exception ignored){}
                Photos.Contact contact=found;Bitmap photo=bitmap;
                handler.post(()->{
                    loading=false;
                    if(stopped || token!=generation || shown || !inCall || photo==null || contact==null)return;
                    if(!getSharedPreferences("MainActivity",0).getBoolean("enabled",true))return;
                    try{
                        if(!getSystemService(TelecomManager.class).isInCall())return;
                        List<String> currentNames=callCandidates();
                        if(Collections.disjoint(candidates,currentNames))return;
                        show(photo,contact.name);
                    }catch(Exception ignored){hide();}
                });
            });
    }
    private List<String> callCandidates(){
        List<String> candidates=new ArrayList<>();
        if(CallNotifications.instance!=null)candidates.addAll(CallNotifications.instance.candidates());
        for(AccessibilityWindowInfo window:getWindows()){
            // Floating incoming-call banners are not the active application window.
            if(window.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY)continue;
            AccessibilityNodeInfo root=window.getRoot();
            if(root==null)continue;
            try{
                if("com.android.incallui".contentEquals(root.getPackageName()==null?"":root.getPackageName()))collect(root,candidates,0);
            }finally{root.recycle();}
        }
        if(notificationCaller!=null && SystemClock.uptimeMillis()-notificationAt<15000 && !candidates.contains(notificationCaller))candidates.add(notificationCaller);
        return candidates;
    }
    private void collect(AccessibilityNodeInfo node,List<String> items,int depth){
        if(depth>28 || items.size()>12)return;
        String id=node.getViewIdResourceName();
        if(id!=null){String shortId=id.substring(id.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
            if(Arrays.asList("primary_caller_name","primary_call_name","caller_name",
                    "call_name","call_display_name","caller_active_name","name",
                    "phone_number","caller_number","labelandnumber","primary_number",
                    "txt_displayname","multi_ring_call_name").contains(shortId)){
                CharSequence text=node.getText();
                if(text!=null && node.isVisibleToUser()){String s=text.toString().trim();if(!s.isEmpty() && !items.contains(s))items.add(s);}
            }
        }
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null){try{collect(child,items,depth+1);}finally{child.recycle();}}
        }
    }
    private void show(Bitmap bitmap,String name){
        if(overlay!=null)return;
        overlay=new PhotoPanel(this,bitmap,name,false,()->{
            hide();
            try{if(getSystemService(TelecomManager.class).isInCall() && CallNotifications.instance!=null)CallNotifications.instance.openCallScreen();}catch(SecurityException ignored){}
        });
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;
        p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        try{getSystemService(WindowManager.class).addView(overlay,p);shown=true;android.util.Log.i("CallerPhoto","Photo overlay shown");}
        catch(Exception e){overlay=null;}
    }
    void hide(){if(overlay!=null){try{getSystemService(WindowManager.class).removeView(overlay);}catch(Exception ignored){}overlay=null;}}
    @Override public void onInterrupt(){hide();}
    @Override public void onDestroy(){stopped=true;generation++;handler.removeCallbacksAndMessages(null);worker.shutdownNow();hide();instance=null;super.onDestroy();}
}
