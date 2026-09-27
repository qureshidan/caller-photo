package local.callerphoto;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.provider.ContactsContract;
import java.io.*;
import java.util.*;

final class Photos {
    static final class Contact { String id,name,photo; Contact(String i,String n,String p){id=i;name=n;photo=p;} }
    static Contact read(Context c,Uri uri){
        try(Cursor cur=c.getContentResolver().query(uri,new String[]{"_id","display_name","photo_uri"},null,null,null)){
            if(cur!=null && cur.moveToFirst())return new Contact(cur.getString(0),cur.getString(1),cur.getString(2));
        }catch(Exception ignored){} return null;
    }
    static Contact match(Context c,String text){
        if(text==null || text.length()<2 || text.length()>120)return null;
        text=text.trim();
        if(text.matches("[+()\\d\\s.\\-]{5,}")){
            Uri u=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(text));
            try(Cursor cur=c.getContentResolver().query(u,new String[]{"_id","display_name","photo_uri"},null,null,null)){
                if(cur!=null && cur.moveToFirst())return new Contact(cur.getString(0),cur.getString(1),cur.getString(2));
            }catch(Exception ignored){}
        }
        try(Cursor cur=c.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{"_id","display_name","photo_uri"},"display_name = ?",new String[]{text},null)){
            // Never guess between contacts with identical names.
            if(cur!=null && cur.getCount()==1 && cur.moveToFirst())return new Contact(cur.getString(0),cur.getString(1),cur.getString(2));
        }catch(Exception ignored){}return null;
    }
    static Bitmap bitmap(Context c,Contact contact){
        if(contact==null)return null;
        File custom=new File(c.getFilesDir(),"contact_"+contact.id+".jpg");
        try{
            if(custom.exists())return decode(ImageDecoder.createSource(custom));
            if(contact.photo!=null)return decode(ImageDecoder.createSource(c.getContentResolver(),Uri.parse(contact.photo)));
        }catch(Exception ignored){}return null;
    }
    static Bitmap decode(ImageDecoder.Source source)throws IOException {
        return ImageDecoder.decodeBitmap(source,(decoder,info,src)->{
            int w=info.getSize().getWidth(),h=info.getSize().getHeight();
            float scale=Math.min(1f,1800f/Math.max(w,h));
            decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }
}
