package com.daizhx.wifiderectcamera;

import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;



public class MySurfaceView extends SurfaceView implements Callback {
	private SurfaceHolder holder;
	private int mWidth, mHeight;

	public MySurfaceView(Context context, AttributeSet attrs) {	
		super(context, attrs);
		// TODO Auto-generated constructor stub
		holder = getHolder();
		holder.addCallback(this);
		Log.d("daizhx", "MySurfaceView construct");
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		mWidth = width;
		mHeight = height;
		Log.d("daizhx", "surfaceChanged:width="+width+",height="+height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("daizhx", "surfaceCreated");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("daizhx", "surfaceDestroyed");
	}

	
	 public void PlayVideo(ByteArrayOutputStream outputstream)
	 {
		 //decode jpeg to bitmap
		 Canvas c= null;
		 Bitmap VideoBit = null;
	     Bitmap srcBitmap = BitmapFactory.decodeByteArray(outputstream.toByteArray(), 0, outputstream.size());
	     
	     if(srcBitmap != null){
	            int w=srcBitmap.getWidth();
	            int h=srcBitmap.getHeight();

	            float widthScale = ((float)mWidth)/w;
	            float heightScale = ((float)mHeight)/h;
	            Matrix matrix = new Matrix();
	            	
	            //matrix.postRotate(90);
	            //if(heightScale >= widthScale){
	            //	matrix.postScale(heightScale, heightScale);
	            //}else{
	            //	matrix.postScale(widthScale, widthScale);
	            //}

	            matrix.postScale(widthScale, widthScale);
	            VideoBit = Bitmap.createBitmap(srcBitmap, 0, 0, w, h, matrix, true);
	        }
	        
	     c = holder.lockCanvas();
	     if(c == null)return;
	     if(VideoBit != null){
	    	 c.drawBitmap(VideoBit, 0, 0, null);
	     }else{
	    	 c.drawColor(0xff0000ff);
	     }
	     holder.unlockCanvasAndPost(c);
	 }
	
}
