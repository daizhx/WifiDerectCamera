package com.daizhx.wifiderectcamera;

import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MyView extends View implements Runnable{
    public static final String TAG = "daizhx";
    Bitmap mBitQQ  = null;

    Paint mPaint = null;

    Bitmap  mSCBitmap = null;

    Bitmap VideoBit ;
    int width, height;


    public MyView(Context context){
    	super(context);
        Log.d(TAG, "myview 111");
    }
    public MyView(Context context, AttributeSet attrs){
    	super(context, attrs);
        Log.d(TAG, "myview 222");
    }
    
    public MyView(Context context, AttributeSet attrs,int defStyle){
    	super(context, attrs, defStyle);
        Log.d(TAG, "myview 333");
    }
    public MyView(Context context, int w, int h) {
        super(context);
        setFocusable(true);
        width = w;
        height = h;
        Log.d(TAG, "myview 444:"+w+"x"+h);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG, "onMeasure:"+width+"x"+height);
    }

    public void PlayVideo(ByteArrayOutputStream outputstream)
    {
        //decode jpeg to bitmap
        Bitmap srcBitmap = BitmapFactory.decodeByteArray(outputstream.toByteArray(), 0, outputstream.size());
        if(srcBitmap != null){
            int w=srcBitmap.getWidth();
            int h=srcBitmap.getHeight();

            width = getWidth();
            height = getHeight();
            float widthScale = ((float)height)/w;
            float heightScale = ((float)width)/h;
            Matrix matrix = new Matrix();

            matrix.postRotate(90);
            matrix.postScale(heightScale, widthScale);

            VideoBit = Bitmap.createBitmap(srcBitmap, 0, 0, w, h, matrix, true);
        }
        Log.d(TAG, "read a frame----:"+System.currentTimeMillis());
        //SaveJPEG(outputstream.toByteArray());
        new Thread(this).start();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(VideoBit != null){
            canvas.drawBitmap(VideoBit, 0, 0, null);
        }
        else{
            canvas.drawColor(0xff0000ff);
        }
    }


    public void run()
    {
        postInvalidate();
    }
}
