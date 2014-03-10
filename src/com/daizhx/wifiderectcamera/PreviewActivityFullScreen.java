package com.daizhx.wifiderectcamera;

import com.daizhx.wifiderectcamera.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PreviewActivityFullScreen extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    public static final String TAG = "daizhx";
    private String mCameraIP;
    private String mPassWord;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    private static MySurfaceView vv;
    private PreviewThread mPreviewThread = null;
    private ProgressDialog progressDialog = null;
    private Handler mHandler;

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

    /* ���������������*/
    public String format(int i)
    {
        String s = i + "";
        if (s.length() == 1)
        {
            s = "0" + s;
        }
        return s;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //hide the soft input window
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        setContentView(R.layout.activity_preview);


        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        vv = (MySurfaceView)findViewById(R.id.preview);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, vv, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        vv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                caturePicture();
            }
        });
        final Button mVideoBtn = (Button)findViewById(R.id.video);
        mVideoBtn.setOnClickListener(new View.OnClickListener() {
            private boolean isVideoing = false;
            @Override
            public void onClick(View view) {
                    if (isVideoing) {
                        stopVideo();
                        mVideoBtn.setText("video");
                        isVideoing = false;
                    } else {
                        startVideo();
                        mVideoBtn.setText("stopvideo");
                        isVideoing = true;
                    }
            }
        });
        recordBar = (LinearLayout)findViewById(R.id.record_bar);
        tv01 = (TextView) findViewById(R.id.mediarecorder2_TextView01);
        tv03 = (TextView) findViewById(R.id.mediarecorder2_TextView03);
        tv05 = (TextView) findViewById(R.id.mediarecorder2_TextView05);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Log.d(TAG, "displayMetrics="+displayMetrics.widthPixels+"x"+displayMetrics.heightPixels);
        Intent intent = getIntent();
        mCameraIP = intent.getStringExtra("cameraIP");
        mPassWord = intent.getStringExtra("password");


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("connecting...");
        progressDialog.show();

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                switch(msg.what){
                    //socket connected,and then send preview request.
                    case 0:
                        requestPreview();
                        break;
                    //socket connecting fail
                    case 1:
                        progressDialog.dismiss();
                        if(mPreviewThread != null){
                            mPreviewThread.close();
                            mPreviewThread = null;
                        }
                        setResult(1);
                        finish();
                        break;
                    //password is wrong
                    case 4:
                        //progressDialog.dismiss();
                        if(mPreviewThread != null){
                            mPreviewThread.close();
                            mPreviewThread = null;
                        }
                        setResult(4);
                        finish();
                        break;

                }
            }

        };
        startPreviewThread();
    }

    
    
    @Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
        
    @Override
	protected void onStop() {
		// TODO Auto-generated method stub
    	if(mPreviewThread != null){
            mPreviewThread.close();
            mPreviewThread = null;
        }
		super.onStop();
	}
    @Override
    protected void onDestroy() {
        
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
        String request = "preview#"+mPassWord+"#"+displayMetrics.widthPixels+"#"+displayMetrics.heightPixels;
        Log.d(TAG, "REQUEST="+request);
        byte[] bytes = request.getBytes();
        mPreviewThread.writeData(bytes);
    }

    private void caturePicture(){
        String str = "capture";
        byte[] bytes = str.getBytes();
        mPreviewThread.sendCommand(bytes);
    }

    private void startVideo(){
        String str = "video";
        byte[] bytes = str.getBytes();
        mPreviewThread.sendCommand(bytes);

        recordBar.setVisibility(View.VISIBLE);
        bool = true;
        handler.postDelayed(recordTimetask, 1000);
    }
    private void stopVideo(){
        String str = "stopVideo";
        byte[] bytes = str.getBytes();
        mPreviewThread.sendCommand(bytes);

        bool = false;//stop record
        recordBar.setVisibility(View.GONE);
        second = 0;
        minute = 0;
        hour = 0;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
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
            Message msg = Message.obtain();
            if(clientSocket == null){
                msg.what = 1;
                mHandler.sendMessage(msg);
                return;
            }
            msg.what = 0;
            mHandler.sendMessage(msg);
            isPreview = true;
            Log.d("daizhx", "socket connected");

            //the second method to read frame data
            long data_len = 0;
            byte data_type = 0x00;
            long read_len = 0;
            
            FileOutputStream fileOutput = null;
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
                        progressDialog.dismiss();
                        data_type = buf[0];
                        //password is wrong,and close the connect
                        if(data_type == 0x04){
                            Message msg1 = Message.obtain();
                            msg1.what = 4;
                            mHandler.sendMessage(msg1);
                            return;
                        }
                        byte[] lenbytes = new byte[8];
                        for(int i=1;i<9;i++){
                            lenbytes[i-1] = buf[i];
                        }
                        data_len = bytesToLong(lenbytes);
                        Log.d(TAG, "data type="+data_type);
                        Log.d(TAG, "jpg_len="+data_len);
                    }
                    if(data_type == 0x01){
                        //preview
                    	Log.d(TAG, "read a frame-start:"+System.currentTimeMillis());
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
                    }else{
                        long time = System.currentTimeMillis();

                        if(data_type == 0x02){
                            File file = fileUtil.createFileInSDCard(time+".jpg", "ipcamera");
                            fileOutput = new FileOutputStream(file);
                        }else if(data_type == 0x03){
                            File file = fileUtil.createFileInSDCard(time+".mp4", "ipcamera");
                            fileOutput = new FileOutputStream(file);
                        }
                        //pd= new ProgressDialog(PreviewActivity.this);
                        //pd.show();

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
                    }

                    //data receive endly
                    if(data_type == 0x01){
                        vv.PlayVideo(out);
                        out.close();
                        Log.d(TAG, "read a frame-end:"+System.currentTimeMillis());
                    }else{
                        fileOutput.flush();
                        fileOutput.close();
                        fileOutput = null;
                        //pd.dismiss();

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
        public void sendCommand(byte[] bytes){
            Log.d(TAG, "send cmd");
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
}
