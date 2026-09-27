package local.callerphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

final class PhotoPanel extends FrameLayout {
    private float downX, downY;
    private boolean closing;
    private final Runnable dismiss;
    PhotoPanel(Context c, Bitmap photo, String name, boolean preview, Runnable dismiss) {
        super(c); this.dismiss = dismiss;
        setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff24565c,0xff14282f,0xff080e16}));
        if (photo != null) {
            ImageView image = new ImageView(c); image.setImageBitmap(photo);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            addView(image,new LayoutParams(-1,-1));
        }
        View arrow = new View(c) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(0x88ffffff); paint.setStrokeWidth(dp(2));
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setShadowLayer(dp(2),0,dp(1),0x66000000);
                float x=getWidth()/2f,y=getHeight()/2f;
                canvas.drawLine(x,y+dp(12),x,y-dp(12),paint);
                canvas.drawLine(x,y-dp(12),x-dp(9),y-dp(3),paint);
                canvas.drawLine(x,y-dp(12),x+dp(9),y-dp(3),paint);
            }
        };
        arrow.setContentDescription("Show call controls");
        arrow.setOnClickListener(v->close());
        LayoutParams position=new LayoutParams(dp(56),dp(56),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        position.bottomMargin=dp(48);addView(arrow,position);
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downY=e.getRawY();downX=e.getRawX();}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
            float dy=e.getRawY()-downY;
            if(dy < -dp(12) && Math.abs(dy)>Math.abs(e.getRawX()-downX)){
                setTranslationY(dy); setAlpha(Math.max(.25f,1+dy/Math.max(1,getHeight())));
            }
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL){
            if(e.getActionMasked()==MotionEvent.ACTION_UP && downY-e.getRawY()>dp(80) && downY-e.getRawY()>Math.abs(e.getRawX()-downX)){
                close();return true;
            }
            if(!closing) animate().translationY(0).alpha(1).setDuration(160).start();
        }
        super.dispatchTouchEvent(e);return true;
    }
    private void close(){if(closing)return;closing=true;animate().translationY(-getHeight()).alpha(0).setDuration(220).withEndAction(dismiss).start();}
}
