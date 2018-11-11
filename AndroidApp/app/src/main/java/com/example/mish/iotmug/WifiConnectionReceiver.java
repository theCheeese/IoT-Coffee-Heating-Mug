package com.example.mish.iotmug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class WifiConnectionReceiver extends BroadcastReceiver {
    
    public WifiConnectionReceiver(Context context, WifiConfiguration wifiConfiguration) {
        this.context = context;
        wifiConfig = wifiConfiguration;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        isConnected = false;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if(isInitialStickyBroadcast())
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networkArr = connectivityManager.getAllNetworks();

            for (Network n : networkArr) {
                NetworkInfo nInfo = connectivityManager.getNetworkInfo(n);
                Log.e("Networks", nInfo.toString());

                if (nInfo.getTypeName().equalsIgnoreCase("wifi") &&
                        nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    Toast.makeText(context, "Connected to " + nInfo.getExtraInfo(), Toast.LENGTH_SHORT).show();
                    isConnected = true;
                    //go to Mug Setup View
                    return;
                }
            }
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            NetworkInfo[] networkInfoArr = connectivityManager.getAllNetworkInfo();     //deprecated but needed to support
            for (NetworkInfo nInfo : networkInfoArr) {
                Log.e("Networks", nInfo.toString());

                if (nInfo.getTypeName().equalsIgnoreCase("wifi") &&
                        nInfo.getState() == NetworkInfo.State.CONNECTED) {
                    Toast.makeText(context, "Connected to " + nInfo.getExtraInfo(), Toast.LENGTH_SHORT).show();
                    isConnected = true;
                    //go to Mug Setup View
                    return;
                }
            }
        }
    }

    public void setWifiConfiguration(WifiConfiguration wifiConfig) {
        this.wifiConfig = wifiConfig;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void resetIsConnected() {
        isConnected = false;
    }

    private Context context;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private WifiConfiguration wifiConfig;
    private boolean isConnected;
}
