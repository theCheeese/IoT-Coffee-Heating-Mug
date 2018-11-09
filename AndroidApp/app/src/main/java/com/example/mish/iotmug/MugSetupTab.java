package com.example.mish.iotmug;
import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
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
import android.widget.EditText;
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

//ISSUE: Apparently Network[] has many wifi networks, so we need a way to check that the wifi network we are switching to is the one we are checking our connectivity for
//ISSUE: Errors don't pop up on connecting to wifi networks
//ISSUE: Many Dialog boxes are possible. Create a way to check if a dialog is open, and then close it if a new one is requested
//ISSUE: Toast messages firing off multiple times per connection attempt

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

            Button connectButton = wifiListMember.findViewById(R.id.connectButton);
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

        if (wifiConnectivityReceiver == null) {
            wifiConnectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!isConnectedToNetwork()) return;

                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Network[] networkArr = connectivityManager.getAllNetworks();
                        for (Network n : networkArr) {
                            NetworkInfo nInfo = connectivityManager.getNetworkInfo(n);
                            if(nInfo.getTypeName().equalsIgnoreCase("wifi"))    //debug
                                Log.e("Networks", nInfo.getExtraInfo() + ": " + nInfo.getDetailedState().toString()); //debug
                            if (nInfo.getTypeName().equalsIgnoreCase("wifi") &&
                                    isSsidCurrentConnection(nInfo.getExtraInfo()) &&
                                    nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                                Toast.makeText(context, "Connected to " + nInfo.getExtraInfo(), Toast.LENGTH_SHORT).show();
                                //go to Mug Setup View
                                break;
                            } else if (nInfo.getTypeName().equalsIgnoreCase("wifi") &&
                                    supplicantError == WifiManager.ERROR_AUTHENTICATING) {
                                wifiManager.removeNetwork(connection.networkId);
                                Toast.makeText(context, "Authentication Error connecting to " + nInfo.getExtraInfo(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        NetworkInfo[] networkInfoArr = connectivityManager.getAllNetworkInfo();     //deprecated but needed to support
                        for (NetworkInfo nInfo : networkInfoArr) {
                            if (nInfo.getTypeName().equalsIgnoreCase("wifi") &&
                                    isSsidCurrentConnection(nInfo.getExtraInfo()) &&
                                    nInfo.getState() == NetworkInfo.State.CONNECTED) {
                                Toast.makeText(context, "Connected to " + nInfo.getExtraInfo(), Toast.LENGTH_SHORT).show();
                                //go to Mug Setup View
                                break;
                            } else if (nInfo.getTypeName().equalsIgnoreCase("wifi") &&
                                    supplicantError == WifiManager.ERROR_AUTHENTICATING) {
                                wifiManager.removeNetwork(connection.networkId);
                                Toast.makeText(context, "Error connecting to " + nInfo.getExtraInfo(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            getActivity().registerReceiver(wifiConnectivityReceiver, intentFilter);  //should this be an app-level receiver?
        }

        //if the chosen wifi has already been configured, just connect using that
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration configuredWifi : wifiConfigurationList) {
            if(configuredWifi.SSID.equals(connection.SSID)) {
                wifiManager.enableNetwork(configuredWifi.networkId, true);
                return;
            }
        }

        Log.e("Connection", "Connection is a new configuration");

        //else, try to connect with a user-given password
        for(ScanResult wifiConfig : wifiScanList) {
            if(wifiConfig.SSID.equals(currentSsid)) {
                String securityType = wifiConfig.capabilities;
                if(securityType.contains("WPA2")) {
                    Log.e("Connection", "WPA2 security detected");
                    showWPA2Dialog(connection);
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

        Log.e("Connection", "Attempting connection...");

        //attempt a connection to chosen wifi network
        wifiManager.enableNetwork(connection.networkId, true);
    }

    private void showWPA2Dialog(final WifiConfiguration connection) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        LayoutInflater layoutInflater = getLayoutInflater();
        final View passwordDialogView = layoutInflater.inflate(R.layout.dialog_wifi_password, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(passwordDialogView)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText passwordEditText = passwordDialogView.findViewById(R.id.passwordBox);
                        String WPA2Key = passwordEditText.getText().toString();
                        connection.preSharedKey = "\"" + WPA2Key + "\"";

                        if(wifiManager.addNetwork(connection) != -1) {
                            wifiManager.enableNetwork(connection.networkId, true);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
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
    private BroadcastReceiver wifiConnectivityReceiver;
    private List<ScanResult> wifiScanList;

    private LinearLayout wifiList;
}
