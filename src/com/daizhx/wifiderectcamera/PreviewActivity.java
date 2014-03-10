package com.daizhx.wifiderectcamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.daizhx.wifiderectcamera.PreviewActivityFullScreen.PreviewThread;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Created by daizhx on 1/20/14.
 */
public class PreviewActivity extends Activity {
	public static final String TAG = "daizhx";
    private String mCameraIP;
    private String mPassWord;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    private static MySurfaceView vv;
    private PreviewThread mPreviewThread = null;
    private ProgressDialog progressDialog = null;
    private Handler mHandler;
    private final int MSG_SOCKET_FAIL = 1;
    private final int MSG_REQUEST_PREVIEW = 0;
    private final int MSG_PASSWORD_FAIL = 4;
    private final int MSG_GET_PHOTO = 2;
    private final int MSG_GET_VIDEO = 3;
    private final int MSG_SHOW_PROGRESS = 5;
    private Bitmap ThumbnailBitmap = null;
    private ImageView mThumbnail;
    private String filepath4thumbnail;
    
    private PopupWindow setPopup;
    private String[] previewSizes = new  String[]{"1920x1080","1280x720","960x540","640x360","320x180"};

    private LinearLayout recordBar;
    private TextView tv01,tv02,tv03,tv04,tv05;
    private int hour = 0;
    private int minute = 0;
    private int second = 0;
    private boolean bool;
    private Handler handler = new Handler();
    private Runnable recordTimetask = new Runnable()
    {
        public void run()
        {
            if (bool)
            {
                handler.postDelayed(this, 1000);
                second++;
                if (second < 60)
                {
                    tv05.setText(format(second));
                } else if (second < 3600)
                {
                    minute = second / 60;
                    second = second % 60;
                    tv03.setText(format(minute));
                    tv05.setText(format(second));
                } else
                {
                    hour = second / 3600;
                    minute = (second % 3600) / 60;
                    second = (second % 3600) % 60;
                    tv01.setText(format(hour));
                    tv03.setText(format(minute));
                    tv05.setText(format(second));
                }
            }
        }
    };

    public String format(int i)
    {
        String s = i + "";
        if (s.length() == 1)
        {
            s = "0" + s;
        }
        return s;
    }
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview);
        //vv = (MySurfaceView)findViewById(R.id.preview);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        
        final FrameLayout fl = (FrameLayout)findViewById(R.id.window);
        vv = new MySurfaceView(this, null);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(displayMetrics.widthPixels, (9*displayMetrics.widthPixels)/16, Gravity.CENTER);
        fl.addView(vv, lp);
        mThumbnail = (ImageView)findViewById(R.id.icon_photo);
        mThumbnail.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(PreviewActivity.this, FilesActivity.class));
			}
        });
        
        findViewById(R.id.icon_shutter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                caturePicture();
            }
        });
        final ImageView mVideoBtn = (ImageView)findViewById(R.id.icon_video);
        mVideoBtn.setOnClickListener(new View.OnClickListener() {
            private boolean isVideoing = false;
            @Override
            public void onClick(View view) {
                    if (isVideoing) {
                        stopVideo();
                        isVideoing = false;
                    } else {
                        startVideo();
                        isVideoing = true;
                    }
            }
        });
        //final View view_set = View.inflate(this, R.layout.setting, null);
        final ImageView iconSetting = (ImageView)findViewById(R.id.icon_setting);
        iconSetting.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Context mContext = PreviewActivity.this;
				LayoutInflater mLayoutInflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
				View view_set = mLayoutInflater.inflate(R.layout.setting, null);
				// TODO Auto-generated method stub
				Log.d(TAG, "setting...");
				ListView listView = (ListView)  view_set.findViewById(R.id.list);
				//MyAdapter adapter = new  MyAdapter();
				//String[] s = new  String[]{"1920x1080","1280x720","640x360","320x180"};
				//ArrayAdapter arrayAdapter = new ArrayAdapter(mContext, R.layout.setting_listitem,R.id.text, s);
				MyAdapter adapter = new MyAdapter();
		        listView.setAdapter(adapter);
				//PopupWindow pw = new PopupWindow(view_set, LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		        //PopupWindow pw = new PopupWindow(view_set);
		        view_set.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		        setPopup = new PopupWindow(view_set, view_set.getMeasuredWidth(), LayoutParams.WRAP_CONTENT);
		        setPopup.setFocusable(true); 
		        setPopup.setTouchable(true);
				//popupwindow要有背景图片OutsideTouchable、keyback才有效
		        setPopup.setBackgroundDrawable(mContext.getResources().getDrawable(android.R.color.transparent));
				//触摸popupwindow外部，可以消失。必须设置背景
		        setPopup.setOutsideTouchable(true);
		        setPopup.setAnimationStyle(android.R.style.Animation_Toast);
		        setPopup.update();
		        setPopup.showAtLocation(fl, Gravity.CENTER, 0, 0);
				//pw.showAsDropDown(iconSetting);
				
			}
		});
        recordBar = (LinearLayout)findViewById(R.id.record_bar);
        tv01 = (TextView) findViewById(R.id.mediarecorder2_TextView01);
        tv03 = (TextView) findViewById(R.id.mediarecorder2_TextView03);
        tv05 = (TextView) findViewById(R.id.mediarecorder2_TextView05);

        Intent intent = getIntent();
        mCameraIP = intent.getStringExtra("cameraIP");
        mPassWord = intent.getStringExtra("password");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("connecting...");
        progressDialog.show();

        mHandler = new Handler(){
        	//handle events from preview thread
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                switch(msg.what){
                    case MSG_REQUEST_PREVIEW:
                        requestPreview();
                        break;
                    case MSG_SOCKET_FAIL:
                        progressDialog.dismiss();
                        if(mPreviewThread != null){
                            mPreviewThread.close();
                            mPreviewThread = null;
                        }
                        setResult(1);
                        finish();
                        break;
                    case MSG_PASSWORD_FAIL:
                        //progressDialog.dismiss();
                        if(mPreviewThread != null){
                            mPreviewThread.close();
                            mPreviewThread = null;
                        }
                        setResult(4);
                        finish();
                        break;
                    case MSG_GET_PHOTO:
                    	{
                    	Bitmap thumbnail = getImageThumbnail(filepath4thumbnail, 120, 120);
                    	mThumbnail.setImageBitmap(thumbnail);
                    	progressDialog.dismiss();
                    	String cmd = "copyover";
                    	mPreviewThread.sendCommand(cmd);
                    	}	
                    	break;
                    case MSG_GET_VIDEO:
                    	{
                    	Bitmap thumbnail = getVideoThumbnail(filepath4thumbnail, 120, 120, MediaStore.Images.Thumbnails.MICRO_KIND);
                    	mThumbnail.setImageBitmap(thumbnail);
                    	progressDialog.dismiss();
                    	String cmd = "copyover";
                    	mPreviewThread.sendCommand(cmd);
                    	}
                    	break;
                    case MSG_SHOW_PROGRESS:
                    	progressDialog.setMessage("");
                    	progressDialog.show();
                    	break;
                }
            }

        };
        startPreviewThread();
    }
    
    protected Bitmap getImageThumbnail(String filepath4thumbnail2, int width, int height) {
		// TODO Auto-generated method stub
    	Bitmap bitmap = null;
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inJustDecodeBounds = true;
    	bitmap = BitmapFactory.decodeFile(filepath4thumbnail2, options);
    	options.inJustDecodeBounds = false;
    	int h = options.outHeight;
    	int w = options.outWidth;
    	int ratioH = h/height;
    	int ratioW = w/width;
    	int ratio;
    	if(ratioH < ratioW){
    		ratio = ratioH;
    	}else{
    		ratio = ratioW;
    	}
    	options.inSampleSize = ratio;
    	bitmap = BitmapFactory.decodeFile(filepath4thumbnail2, options);
    	bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}

	protected Bitmap getVideoThumbnail(String filepath4thumbnail2, int width, int height, int kind) {
		// TODO Auto-generated method stub
    	Bitmap bitmap = null;
    	bitmap = ThumbnailUtils.createVideoThumbnail(filepath4thumbnail2, kind);
    	bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub    	
    	super.onStop();
	}
	
	
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
    	if(mPreviewThread != null){
            mPreviewThread.close();
            mPreviewThread = null;
        }
		super.onDestroy();
	}

	private void startPreviewThread(){
        if(mPreviewThread == null){
            mPreviewThread = new PreviewThread();
            mPreviewThread.start();
        }else{
            //should not occurred this case
            progressDialog.dismiss();
            setResult(2);
            finish();
        }

    }
    
	private void requestPreview(){
        String request = "preview#"+mPassWord;
        mPreviewThread.sendCommand(request);
    }

    private void caturePicture(){
        String str = "capture";
        mPreviewThread.sendCommand(str);
    }

    private void startVideo(){
        String str = "video";
        mPreviewThread.sendCommand(str);

        recordBar.setVisibility(View.VISIBLE);
        bool = true;
        handler.postDelayed(recordTimetask, 1000);
    }
    private void stopVideo(){
        String str = "stopvideo";
        mPreviewThread.sendCommand(str);

        bool = false;//stop record
        recordBar.setVisibility(View.GONE);
        second = 0;
        minute = 0;
        hour = 0;
    }
    
    private void setPreviewSize(int position){
    	String cmd = "set#"+previewSizes[position];
    	mPreviewThread.sendCommand(cmd);
    }
    
    public class PreviewThread extends Thread {
        private Socket clientSocket;
        InputStream inputstream;
        OutputStream outputstream;
        byte buf[] = new byte[4048];
        byte[] headBuf = new byte[4];
        int len;
        int offset = 0;
        boolean isPreview = false;//
        FileUtil fileUtil = new FileUtil();

        @Override
        public void run() {
            try
            {
                clientSocket = new Socket(mCameraIP,8988);
                inputstream = clientSocket.getInputStream();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                Log.e(TAG, Log.getStackTraceString(e));
            }
            
            if(clientSocket == null){
            	Message msg = Message.obtain();
                msg.what = MSG_SOCKET_FAIL;
                mHandler.sendMessage(msg);
                return;
            }else{
            	Message msg = Message.obtain();
            	msg.what = MSG_REQUEST_PREVIEW;
            	mHandler.sendMessage(msg);
            	isPreview = true;
            }

            long data_len = 0;
            byte data_type = 0x00;
            long read_len = 0;            
            while(true){
                if(!isPreview)return;
            	data_len = 0;
            	data_type = 0x00;
            	read_len = 0;
            	ByteArrayOutputStream out = new ByteArrayOutputStream();
                //get length
                try {
                    if(inputstream.read(buf,0,9) != -1){
                        //once read data from camera,we know it connected success
                        data_type = buf[0];
                        //password is wrong,and close the connect
                        if(data_type == 0x04){
                            Message msg = Message.obtain();
                            msg.what = MSG_PASSWORD_FAIL;
                            mHandler.sendMessage(msg);
                            return;
                        }
                        byte[] lenbytes = new byte[8];
                        for(int i=1;i<9;i++){
                            lenbytes[i-1] = buf[i];
                        }
                        data_len = bytesToLong(lenbytes);
                    }
                    if(data_type == 0x01){
                        //preview
                    	progressDialog.dismiss();
                        while(read_len < data_len){
                            long num = data_len - read_len;
                            if(num >= 4048){
                                if((len = inputstream.read(buf,0,4048)) != -1){
                                    out.write(buf, 0, len);
                                }
                            }else{
                                if((len = inputstream.read(buf,0,(int)num)) != -1){
                                    out.write(buf, 0, len);
                                }
                            }
                            read_len += len;
                        }
                        //show it on surfaceview immediately
                        vv.PlayVideo(out);
                        out.close();
                    }else{
                        long time = System.currentTimeMillis();
                        File file = null;
                        int msg_what = -1;
                        FileOutputStream fileOutput = null;
                        
                        Message msg = Message.obtain();
                        msg.what = MSG_SHOW_PROGRESS;
                        mHandler.sendMessage(msg);
                        
                        if(data_type == 0x02){
                            file = fileUtil.createFileInSDCard(time+".jpg", "ipcamera");
                            msg_what = MSG_GET_PHOTO;
                        }else if(data_type == 0x03){
                            file = fileUtil.createFileInSDCard(time+".mp4", "ipcamera");
                            msg_what = MSG_GET_VIDEO;
                        }else{
                        	Log.d(TAG, "receive data type error:"+data_type);
                        }
                        fileOutput = new FileOutputStream(file);
                        filepath4thumbnail = file.getAbsolutePath();
                        while(read_len < data_len){
                            long num = data_len - read_len;
                            if(num >= 4048){
                                if((len = inputstream.read(buf,0,4048)) != -1){
                                    fileOutput.write(buf, 0, len);
                                }
                            }else{
                                if((len = inputstream.read(buf,0,(int)num)) != -1){
                                    fileOutput.write(buf, 0, len);
                                }
                            }
                            read_len += len;
                        }
                        //receive end
                        fileOutput.flush();
                        fileOutput.close();
                        Message msg1 = Message.obtain();
                        msg1.what = msg_what;
                        mHandler.sendMessage(msg1);
                    }                    

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void writeData(byte[] outStream){
            try {
                outputstream = clientSocket.getOutputStream();
                outputstream.write(outStream);
                outputstream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        public void sendCommand(String cmd){
            Log.d(TAG, "send cmd:"+cmd);
            byte[] bytes = cmd.getBytes();
            mPreviewThread.writeData(bytes);
        }
        private void close(){
        	isPreview = false;
        	try {
				join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	//we hope the thread is not running when closed the socket, 
        	//maybe we must not to close the socket manually,but we do.
            try {
                if(clientSocket != null)clientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }

        private long bytesToLong(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.put(bytes);
            buffer.flip();//need flip
            return buffer.getLong();
        }

    }
    
    private  class MyAdapter extends BaseAdapter{
        //private String[] s = new  String[]{"1920x1080","1280x720","640x360","320x180"};
        private int temp = -1;

         @Override
        public int getCount() {
            // TODO  Auto-generated method stub
            return previewSizes.length;
         }

        @Override
        public Object getItem(int position)  {
            // TODO Auto-generated method stub
            return  null;
        }

        @Override
        public long getItemId(int  position) {
            // TODO Auto-generated method stub
             return 0;
        }

        @Override
        public View  getView(int position, View convertView, ViewGroup parent) {
             Holder holder;
            if(convertView == null){
                 convertView = PreviewActivity.this.getLayoutInflater().inflate(R.layout.setting_listitem,  null);
                holder = new Holder();
                 convertView.setTag(holder);
            }else{
                holder =  (Holder) convertView.getTag();
            }
            
             holder.textView = (TextView) convertView.findViewById(R.id.text);
             holder.textView.setText(previewSizes[position]);
            
             holder.radioButton = (RadioButton)convertView.findViewById(R.id.radioButton);
             if(holder.radioButton == null){
            	 Log.d(TAG, "radioButton=null");
            	 return convertView;
             }
             Log.d(TAG, "radioButton!=null");
             holder.radioButton.setId(position);
             if((previewSizes[position]).equals("1280x720") && temp == -1){
            	 Log.d(TAG, "temp11="+temp);
            	 holder.radioButton.setChecked(true);
             }
             holder.radioButton.setOnCheckedChangeListener(new OnCheckedChangeListener()  {
               
                @Override
                public  void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                	int position = buttonView.getId();
                	setPreviewSize(position);
                     Log.d(TAG, "..."+isChecked);
                    if(isChecked){
                         if(temp != -1){
                            RadioButton tempButton =  (RadioButton) (buttonView.getRootView()).findViewById(temp);
                             tempButton.setChecked(false);
                         }                        
                        temp =  buttonView.getId();
                        Log.i(TAG,"you are women- -   " +  isChecked + "   " + temp);
                        
                     }
                    setPopup.dismiss();
                }
            });
            return  convertView;
        }
        private class Holder{
             private TextView textView;
            private RadioButton radioButton;
         }
    }
}