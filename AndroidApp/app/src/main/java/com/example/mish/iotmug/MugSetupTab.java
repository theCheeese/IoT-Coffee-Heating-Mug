package com.example.mish.iotmug;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.text.Layout;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

//TODO: filter SSIDs of the available access points to only Mugs
//TODO: connect phone to selected network and display setup dialog/page
//TODO: develop wifi network password authentication dialog
//TODO: develop setup dialog/page
//TODO; develop error dialogs for wifi network password authentication errors
//TODO: develop error dialogs for denied app permissions

public class MugSetupTab extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.connect_tab_mug_setup, container, false);

        wifiScanList = new ArrayList<>();
        wifiList = rootView.findViewById(R.id.wifiList);
        context = getActivity().getApplicationContext();

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
                PermissionChecker.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_NETWORK_STATE) !=
                PermissionChecker.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CHANGE_NETWORK_STATE) !=
                PermissionChecker.PERMISSION_GRANTED) {
            //TODO: show error message stating the app needs permissions to search for and connect to mugs
            return;
        }

        final WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanList.clear();
        checkAndEnableWifi();

        //set up BroadcastReceiver wifiScanReceiver to update the wifi network list layout after finishing scan
        if(wifiScanReceiver == null) {
            wifiScanReceiver = new BroadcastReceiver() {  //define a broadcast receiver for wifi scan update intents
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean scanSuccess = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);

                    if (scanSuccess) {
                        Log.e("scan results", "scan success");
                        wifiScanList = wifiManager.getScanResults();
                        updateWifiListLayout();                             //on scan success, update Layout
                    } else
                        Log.e("scan results", "unsuccessful scan");
                    return;
                }
            };
            IntentFilter intentFilter = new IntentFilter();                     //filter intents to trigger broadcast receiver upon
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);  //newly available scan results
            getContext().registerReceiver(wifiScanReceiver, intentFilter);      //start listening
        }

        boolean successfulStartScan = wifiManager.startScan();              //start the scan

        if(!successfulStartScan) {
            Log.e("scan results", "unsuccessful start scan");
            return;
        }
    }

    private void updateWifiListLayout() {
        if(wifiScanList == null) return;

        wifiList.removeAllViews();

        for(ScanResult wifiConfig : wifiScanList) {
            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout wifiListMember = (LinearLayout) layoutInflater.inflate(R.layout.connect_tab_member_mug_access_point, null);
            wifiList.addView(wifiListMember);

            TextView wifiListMemberSsid = wifiListMember.findViewById(R.id.ssid);
            wifiListMemberSsid.setText(wifiConfig.SSID);

            Button connectButton = wifiList.findViewById(R.id.connectButton);
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectToMug(v);
                }
            });
        }

    }

    public void connectToMug(View view) {

        checkAndEnableWifi();

        //get wifi ssid from the viewgroup button belongs to
        ViewGroup wifiLayout = (ViewGroup) view.getParent();
        TextView ssidView = wifiLayout.findViewById(R.id.ssid);
        String currentSsid = ssidView.getText().toString();

        //TODO: check if connection is a mug
        //if not a mug, display an error and return

        //set up a broadcast receiver to listen for wifi connections being made
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiConfiguration connection = new WifiConfiguration();
        connection.SSID = "\"" + currentSsid + "\"";

        if(isSsidCurrentConnection(connection.SSID)) {
            Toast.makeText(context, "Already Connected to " + connection.SSID, Toast.LENGTH_SHORT).show();
            return;
        }

        BroadcastReceiver wifiConnectionListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!isConnectedToNetwork()) return;

                if(isSsidCurrentConnection(connection.SSID)) {
                    boolean newWifiConnection = true;
                    for(WifiConfiguration configuredWifi : wifiManager.getConfiguredNetworks()) {
                        if(configuredWifi.SSID.equals(connection.SSID))
                            newWifiConnection = false;
                    }

                    if(newWifiConnection)
                        wifiManager.addNetwork(connection);
                    Log.e("ConnectionNotification", "Made successful wifi connection");
                    Toast.makeText(context, "Connected to " + connection.SSID, Toast.LENGTH_SHORT).show();
                    getActivity().unregisterReceiver(this); //prevent duplicate broadcast receivers from executing
                    //go to setup dialog or page
                }
                else {
                    Toast.makeText(context, "Error connecting to " + connection.SSID, Toast.LENGTH_SHORT).show();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getActivity().registerReceiver(wifiConnectionListener, intentFilter);  //should this be an app-level receiver?

        //if the chosen wifi is already configured, just connect
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration configuredWifi : wifiConfigurationList) {
            if(configuredWifi.SSID.equals(connection.SSID)) {
                wifiManager.enableNetwork(configuredWifi.networkId, true);
                return;
            }
        }

        //else, try to connect with a user-given password
        for(ScanResult wifiConfig : wifiScanList) {
            if(wifiConfig.SSID.equals(connection.SSID)) {
                String securityType = wifiConfig.capabilities;
                if(securityType.contains("WPA2")) {
                    connection.preSharedKey = "\"" + getWPA2Key() + "\"";
                }
                else if(securityType.contains("Open")) {
                    connection.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                else {
                    Log.e("Connection Info", wifiConfig.toString());
                    Log.e("Connection", "Invalid connection encryption type (mugs are configured only for WPA2 keys)");
                    Toast.makeText(getContext(),
                            "Connection error: wrong connection encryption type (only WPA2 is used in mugs)",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        //attempt a connection to chosen wifi network
        wifiManager.enableNetwork(connection.networkId, true);
        //TODO: find a way to notify user if password authentication failed
    }

    private String getWPA2Key() {
        String WPA2Key = "";
        return WPA2Key;
    }

    private boolean isSsidCurrentConnection(String SSID) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentConnection = wifiManager.getConnectionInfo();
        if(currentConnection != null && currentConnection.getSSID().equals(SSID))
            return true;
        else return false;
    }

    private boolean isConnectedToNetwork() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentConnection = wifiManager.getConnectionInfo();
        if(currentConnection != null && currentConnection.getNetworkId() == -1)
            return false;
        else return true;
    }

    private void checkAndEnableWifi() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    private Context context;
    private BroadcastReceiver wifiScanReceiver;
    private List<ScanResult> wifiScanList;

    private LinearLayout wifiList;
}
