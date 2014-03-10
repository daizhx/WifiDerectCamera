package com.daizhx.wifiderectcamera;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Inflater;

public class MainActivity extends Activity implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener{
    public static final String TAG = "daizhx";
    private ListView mListView;
    private TextView mNoPeesText;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private List<String> peersName = new ArrayList<String>();
    private ArrayAdapter listAdapter;

    private BroadcastReceiver receiver = null;
    private boolean isWifiP2pEnabled = false;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pConfig mWifiP2pConfig;
    private WifiP2pDevice mWifiP2pGroupOwnerDevice;

    private WifiP2pInfo info;
    private String HostInetAddress = null;//group owner ip

    ProgressDialog progressDialog = null;
    private boolean connectFlag = false;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    private boolean isWiFiActive() {
        ConnectivityManager connectivity = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo[] infos = connectivity.getAllNetworkInfo();
                    if (infos != null) {
                        for(NetworkInfo ni : infos){
                            if(ni.getTypeName().equals("WIFI") && ni.isConnected()){
                                return true;
                            }
                        }
            }
        }
        return false;
    }


    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                progressDialog.setMessage("connect...");
                progressDialog.show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //check the wifi state
        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }

        setContentView(R.layout.activity_main);
        mListView = (ListView)findViewById(R.id.list);
        mNoPeesText = (TextView)findViewById(R.id.hintText);

        listAdapter = new ArrayAdapter(this, R.layout.row_devices,R.id.device_name,peersName);
        mListView.setAdapter(listAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //set a wifip2pconfig used by building connect
                if(info != null && info.groupFormed){
                    PopupSlectionWindow();
                }else{
                    mWifiP2pConfig = new WifiP2pConfig();
                    mWifiP2pGroupOwnerDevice = peers.get(i);
                    mWifiP2pConfig.deviceAddress = mWifiP2pGroupOwnerDevice.deviceAddress;
                    mWifiP2pConfig.wps.setup = WpsInfo.PBC;
                    connect(mWifiP2pConfig);
                    connectFlag = true;
                }
            }
        });

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        if(manager == null || channel == null){
            finish();
        }

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


    }

    @Override
    public void onResume() {
        super.onResume();

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        findPeers();

    }

    private void findPeers(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(getString(R.string.action_search));
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                progressDialog.show();
                //start a timer for time limit
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();

                        }
                    }
                }, 5 * 1000);
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(MainActivity.this, "Discovery Failed : " + i,
                        Toast.LENGTH_SHORT).show();
                //test code
                //PopupSlectionWindow();
                //Log.d(TAG, "show dialog");
            }
        });
    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peersName.clear();
        peers.addAll(peerList.getDeviceList());
        for(WifiP2pDevice device:peers){
            peersName.add(device.deviceName);
        }
        listAdapter.notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(MainActivity.TAG, "No devices found");
            mNoPeesText.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);

        }else{
            mListView.setVisibility(View.VISIBLE);
            mNoPeesText.setVisibility(View.GONE);
        }



    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_search:
                findPeers();
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        this.info = wifiP2pInfo;

        if (info.groupFormed && info.isGroupOwner) {
            //can not work in this state
            Log.d(TAG,"no...I can not be a group owner");
        }else if (info.groupFormed){
            HostInetAddress = info.groupOwnerAddress.getHostAddress();
            if(connectFlag){
                PopupSlectionWindow();
                connectFlag = false;
            }
        }
    }

    private void PopupSlectionWindow(){
        View pwInput = View.inflate(this, R.layout.dialog_view, null);
        new AlertDialog.Builder(this).setTitle(R.string.password_hint)
                .setView(pwInput)
                .setPositiveButton(R.string.positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AlertDialog ad = (AlertDialog)dialogInterface;
                        EditText pwText = (EditText)ad.findViewById(R.id.pwInput);
                        String pw = pwText.getText().toString();
                        Log.d(TAG, "pw="+pw);
                        Intent intent = new Intent("com.daizhx.action.PREVIEW");
                        intent.putExtra("password", pw);
                        intent.putExtra("cameraIP", HostInetAddress);
                        startActivityForResult(intent, 1);//1 just >= 0,no meaning
                    }
                })
                .setNegativeButton(R.string.negtive_btn, null)
                .show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "preiview activity finished:"+requestCode+","+resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
