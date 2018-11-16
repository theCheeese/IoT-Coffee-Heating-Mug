package com.example.mish.iotmug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class WifiConnectionReceiver extends BroadcastReceiver {
    
    public WifiConnectionReceiver(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if(isInitialStickyBroadcast())
            return;

        if(intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
            SupplicantState supplicantState = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            switch(supplicantState) {
                case ASSOCIATED:
                    //check if we're associated with the network we wanna connect to
                    WifiInfo currentConnection = wifiManager.getConnectionInfo();
                    String currentSSID = currentConnection.getSSID();
                    if(SSID.equals(currentSSID)) {
                        Toast.makeText(context, "Connected to " + SSID, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case DISCONNECTED:
                    Toast.makeText(context, "Error connecting to " + SSID, Toast.LENGTH_SHORT).show();
                    wifiManager.removeNetwork(netId);
                    break;
            }
        }

    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public void setNetId(int netId) {
        this.netId = netId;
    }

    private Context context;
    private WifiManager wifiManager;
    private String SSID;
    private int netId;
}
