package com.example.mish.iotmug;
import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

//TODO: find a way to save the AP list after switching to another app and back
//TODO: develop transition to MugServerSetupActivity
//TODO: develop error dialogs for denied app permissions
//TODO: show error on receiving password less than 8 characters in length

//ISSUE: Duplicate Dialog boxes are currently possible. Create a way to check if a dialog is open, and then close it if a new one is requested

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
                updateAPList(v);
            }
        });

        return rootView;
    }

    public void updateAPList(View view) {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_WIFI_STATE) !=
                PermissionChecker.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CHANGE_WIFI_STATE) !=
                PermissionChecker.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_NETWORK_STATE) !=
                PermissionChecker.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CHANGE_NETWORK_STATE) !=
                PermissionChecker.PERMISSION_GRANTED) {
            //TODO: show error message stating the app needs permissions to search for and connect to Wifi access points
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
                    if(scanSuccess && wifiScanList != null) {
                        wifiScanList = wifiManager.getScanResults();

                        wifiList.removeAllViews();
                        for(ScanResult wifiConfig : wifiScanList) {
                            if(wifiConfig.SSID.equals("")) {
                                return; //skip hidden SSIDs
                            }
                            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            LinearLayout wifiListMember = (LinearLayout) layoutInflater.inflate(R.layout.connect_tab_member_mug_selection, null);
                            wifiList.addView(wifiListMember);

                            TextView wifiListMemberSsid = wifiListMember.findViewById(R.id.id);
                            wifiListMemberSsid.setText(wifiConfig.SSID);
                            Button connectButton = wifiListMember.findViewById(R.id.connectButton);
                            connectButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    buildWifiConfiguration(v);
                                }
                            });
                        }                             //on scan success, update Layout
                    }
                    else {
                        Log.e("scan results", "unsuccessful scan");
                    }
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

    public void buildWifiConfiguration(View view) {
        checkAndEnableWifi();

        //get wifi ssid from the viewgroup button belongs to
        ViewGroup wifiLayout = (ViewGroup) view.getParent();
        TextView ssidView = wifiLayout.findViewById(R.id.id);
        String currentSsid = ssidView.getText().toString();

        //TODO: check if connection is a mug
        //if not a mug, continue
        //else set boolean isMug true, continue as usual, and then open a WebView in another Activity to associate the mug to user's personal wifi access point

        if(isSsidCurrentConnection("\"" + currentSsid + "\"")) {
            Toast.makeText(context, "Already Connected to \"" + currentSsid + "\"", Toast.LENGTH_SHORT).show();
            return;
        }

        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiConfiguration connection = new WifiConfiguration();
        connection.SSID = "\"" + currentSsid + "\"";

        //if the chosen wifi has already been configured, just connect using that
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration configuredWifi : wifiConfigurationList) {
            if(configuredWifi.SSID.equals(connection.SSID)) {
                if (wifiConnectivityReceiver == null) {
                    wifiConnectivityReceiver = new WifiConnectionReceiver(context);
                }

                wifiConnectivityReceiver.setSSID(configuredWifi.SSID);
                wifiConnectivityReceiver.setNetId(configuredWifi.networkId);
                context.registerReceiver(wifiConnectivityReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                wifiManager.enableNetwork(configuredWifi.networkId, true);
                Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();
                return;
            }
        }

        //else, try to connect with a user-given password
        for(ScanResult wifiConfig : wifiScanList) {
            if(wifiConfig.SSID.equals(currentSsid)) {
                String securityType = wifiConfig.capabilities;
                if(securityType.contains("WPA2")) {
                    //finish building the wifi configuration
                    connection.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    connection.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    connection.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    connection.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    connection.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    connection.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    connection.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    connection.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    connection.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                    //show password dialog
                    LayoutInflater layoutInflater = getLayoutInflater();
                    final View passwordDialogView = layoutInflater.inflate(R.layout.dialog_wifi_password, null);
                    new AlertDialog.Builder(getActivity()).setView(passwordDialogView)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    EditText passwordEditText = passwordDialogView.findViewById(R.id.passwordBox);
                                    String WPA2Key = passwordEditText.getText().toString();
                                    if(WPA2Key.length() < 8) {
                                        Toast.makeText(context, "Password must be at least 8 characters in length",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    connection.preSharedKey = "\"" + WPA2Key + "\"";
                                    attemptConnection(connection);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    return;
                                }
                            })
                    .create()
                    .show();
                }
                else if(securityType.contains("WEP") ||
                        securityType.contains("EAP")) {     //probably need to check for more security types
                    Log.e("Connection Info", wifiConfig.toString());
                    Toast.makeText(getContext(),
                            "Connection error: wrong connection encryption type (only WPA2 is used in mugs)",
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    connection.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    attemptConnection(connection);
                }
            }
        }
    }

    private void attemptConnection(final WifiConfiguration connection) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiConnectivityReceiver == null) {
            wifiConnectivityReceiver = new WifiConnectionReceiver(context);
        }

        int netId = wifiManager.addNetwork(connection);
        wifiConnectivityReceiver.setSSID(connection.SSID);
        wifiConnectivityReceiver.setNetId(netId);
        context.registerReceiver(wifiConnectivityReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
        wifiManager.enableNetwork(netId, true);
        Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();
    }

    private boolean isSsidCurrentConnection(String SSID) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentConnection = wifiManager.getConnectionInfo();
        if(currentConnection != null &&
                currentConnection.getSSID().equals(SSID))
            return true;
        else return false;
    }

    private void checkAndEnableWifi() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    private void openMugSetupActivity() {
        //TODO: create a new activity that shows a web view of the Mug's setup page so that the user can connect it to the local network
    }

    private Context context;
    private BroadcastReceiver wifiScanReceiver;
    private WifiConnectionReceiver wifiConnectivityReceiver;
    private List<ScanResult> wifiScanList;

    private LinearLayout wifiList;
}
