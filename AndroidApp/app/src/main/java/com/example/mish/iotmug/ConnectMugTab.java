package com.example.mish.iotmug;
import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

//TODO: modify layout to display available access points
//TODO: filter SSIDs of the available access points to only Mugs

public class ConnectMugTab extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.connect_tab_access_points, container, false);

        wifiScanList = new ArrayList<>();
        ssidList = new ArrayList<>();

        Button refreshButton = (Button) rootView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList(v);
            }
        });

        return rootView;
    }

    public void updateList(View view) {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_WIFI_STATE) !=
                PermissionChecker.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CHANGE_WIFI_STATE) !=
                PermissionChecker.PERMISSION_GRANTED) {
            //show error message stating the app needs permissions to search for and connect to mugs
            return;
        }

        final WifiManager wifiService = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanList.clear();
        ssidList.clear();

        if(wifiScanReceiver == null) {
            wifiScanReceiver = new BroadcastReceiver() {  //define a broadcast receiver for wifi scan update intents
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean scanSuccess = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);

                    if (scanSuccess) {
                        Log.e("scan results", "scan success");
                        wifiScanList = wifiService.getScanResults();
                        updateWifiListLayout();                             //on scan success, update Layout
                    } else
                        Log.e("scan results", "unsuccessful scan");
                    return;
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();                     //filter intents to trigger broadcast receiver upon
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);  //newly available scan results
        getContext().registerReceiver(wifiScanReceiver, intentFilter);      //start listening

        boolean successfulStartScan = wifiService.startScan();              //start the scan

        if(!successfulStartScan) {
            Log.e("scan results", "unsuccessful start scan");
            return;
        }
    }

    private void updateWifiListLayout() {
        if(wifiScanList == null) return;

        for(ScanResult wifiConfig : wifiScanList) {
            ssidList.add(wifiConfig.SSID);
            Log.e("SSID", wifiConfig.SSID);
        }
    }

    private BroadcastReceiver wifiScanReceiver;
    private List<ScanResult> wifiScanList;
    private List<String> ssidList;
}
