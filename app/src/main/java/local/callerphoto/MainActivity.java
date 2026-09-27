package local.callerphoto;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private LinearLayout page; private String contactId, contactName;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null){contactId=state.getString("id");contactName=state.getString("name");}render();}
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("id",contactId);b.putString("name",contactName);}
    @Override protected void onResume(){super.onResume();CallNotifications.reconnectIfAllowed(this);render();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private void text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(9),0,dp(9));page.addView(t);}
    private void button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setPadding(dp(12),dp(12),dp(12),dp(12));page.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());}
    private boolean allowed(){return checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED;}
    private void render(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff101e25);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(48),dp(24),dp(40));scroll.addView(page);setContentView(scroll);
        text("CALLER PHOTO",12,0xff87e5ca);text("A familiar face.\nYour usual phone.",32,Color.WHITE);
        text("Full-screen contact photos over your Xiaomi call screen. Swipe up to reveal the normal call controls.",16,0xffbacbd0);
        boolean enabled=getPreferences(0).getBoolean("enabled",true);
        text(PhotoService.instance!=null && CallNotifications.instance!=null && allowed()?(enabled?"● Ready for a phone-call test":"● Photo overlay paused"):"○ Setup needed",16,0xff87e5ca);
        String accessibility=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(PhotoService.instance==null && accessibility!=null && accessibility.contains(new ComponentName(this,PhotoService.class).flattenToString())){
            text("Android has stopped the photo service. In Accessibility → Downloaded apps → Caller Photo, turn it off and back on. Also allow background autostart and set battery use to No restrictions.",15,0xffffcf8a);
        }
        if(CallNotifications.instance==null && getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(this,CallNotifications.class))){
            text("Call-notification access is enabled; requesting reconnection. If it stays disconnected, turn that access off and back on.",15,0xffffcf8a);
        }
        button(allowed()?"1  ·  Permissions granted":"1  ·  Allow contacts and call state",()->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.READ_PHONE_STATE},10));
        button(PhotoService.instance!=null?"2  ·  Accessibility connected":"2  ·  Enable Caller Photo accessibility",()->new AlertDialog.Builder(this).setTitle("Match the person on your call screen")
            .setMessage("Caller Photo uses Accessibility to read the name or number on the Xiaomi call screen and show a photo over it. Contacts and call-state access let it find the photo and remove it when the call ends.\n\nIt does not press call controls, record audio, or access the internet. On the next screen, select Downloaded apps → Caller Photo and enable it.")
            .setPositiveButton("Open settings",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Cancel",null).show());
        button(CallNotifications.instance!=null?"3  ·  Call notifications connected":"3  ·  Enable detection over other apps",()->new AlertDialog.Builder(this).setTitle("Detect calls while using other apps")
            .setMessage("Xiaomi's small call banner hides its caller details from Accessibility. Notification access lets Caller Photo match the name in the incoming-call notification to your saved photo.\n\nAndroid grants access to notifications from all apps. Caller Photo discards other apps' notifications and only processes Xiaomi call notifications locally. It does not store notification history or use the internet.")
            .setPositiveButton("Open settings",(d,w)->{
                Intent settings=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(this,CallNotifications.class).flattenToString());
                try{startActivity(settings);}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
            }).setNegativeButton("Cancel",null).show());
        button("Try the swipe-up preview",()->preview(null,"Alex"));
        text("YOUR PHOTOS",12,0xff87e5ca);
        text("Existing contact photos are used automatically. Add a sharper portrait below without changing the photo in Contacts. Contacts with no photo keep the normal call screen.",15,0xffbacbd0);
        button("Choose a contact → set a photo",()->{
            if(!allowed()){requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.READ_PHONE_STATE},10);return;}
            startActivityForResult(new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI),20);
        });
        button("Preview a contact's photo",()->{
            if(!allowed()){toast("Allow contacts first");return;}
            startActivityForResult(new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI),22);
        });
        button(enabled?"Pause photo overlay":"Resume photo overlay",()->{getPreferences(0).edit().putBoolean("enabled",!enabled).apply();if(PhotoService.instance!=null)PhotoService.instance.hide();render();});
        button("Keep working in the background",()->new AlertDialog.Builder(this).setTitle("Protect Caller Photo from cleanup")
            .setMessage("On Xiaomi, enable Background autostart and set Battery → No restrictions for Caller Photo. If your cleaner still stops it, lock Caller Photo in the recent-apps screen.\n\nThese settings let it detect calls while you use other apps. Force stop or disabling Accessibility will still stop it.")
            .setPositiveButton("App settings",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
            .setNeutralButton("Background autostart",(d,w)->{
                try{startActivity(new Intent().setComponent(new ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity")));}
                catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
            }).setNegativeButton("Done",null).show());
        text("Swiping reveals the controls; it does not answer or end a call. Normal cellular calls only. Lock-screen and call-waiting behaviour need testing on this phone.",13,0xffbacbd0);
    }
    private void preview(Bitmap photo,String name){setContentView(new PhotoPanel(this,photo,name,true,this::render));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);render();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK || data==null || data.getData()==null)return;
        if(request==20 || request==22){
            Photos.Contact contact=Photos.read(this,data.getData());if(contact==null){toast("Could not read this contact");return;}
            if(request==22){worker.execute(()->{Bitmap bitmap=Photos.bitmap(this,contact);runOnUiThread(()->{if(bitmap==null)toast("No photo yet. Choose a contact → set a photo first.");else preview(bitmap,contact.name);});});return;}
            contactId=contact.id;contactName=contact.name;
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("image/*");pick.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(pick,21);
        } else if(request==21 && contactId!=null){
            String id=contactId,name=contactName;Uri uri=data.getData();
            worker.execute(()->{try{
                Bitmap bitmap=Photos.decode(ImageDecoder.createSource(getContentResolver(),uri));
                File target=new File(getFilesDir(),"contact_"+id+".jpg");
                File tmp=new File(getFilesDir(),"contact_"+id+".tmp");
                try(FileOutputStream out=new FileOutputStream(tmp)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,93,out))throw new IOException();}
                if(!tmp.renameTo(target))throw new IOException("Unable to save photo");
                runOnUiThread(()->{toast("Photo saved for "+name);preview(bitmap,name);});
            }catch(Exception e){runOnUiThread(()->toast("Could not load that image. Try a different photo."));}});
        }
    }
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
