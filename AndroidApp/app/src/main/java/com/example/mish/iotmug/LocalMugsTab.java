package com.example.mish.iotmug;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class LocalMugsTab extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.connect_tab_local_mugs, container, false);

        context = getActivity().getApplicationContext();
        deviceList = new ArrayList<>();
        deviceListLayout = rootView.findViewById(R.id.deviceListLayout);

        return rootView;
    }

    public void updateList(View view) {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_WIFI_STATE) !=
                PermissionChecker.PERMISSION_GRANTED) {
            //TODO: show error message stating the app needs permissions to check if phone is connected to a local network to scan
            return;
        }

        //set up a broadcast receiver deviceScanReceiver to update layout listing all devices on the network once scanning is finished
        //create a threadpool that scans for local network devices by local IP
        //execute the threadpool and send the action that triggers deviceScanReceiver
    }

    public void connectToDevice() {
        //get device name and IP from device list layout
        //send HTTP GET Request that asks if device is free and if it is password protected
        //if device is not free, display error message stating device has a user associated with it and return
        //if device is not password protected, return the IP address and name of the mug to the MainActivity for interaction
        //if device is password protected, show dialog box requesting password from user and send an HTTP Request for verification with the mug
        //if incorrect password, show error dialog and return
        //if correct, return the IP address and name of the mug to MainActivity for interaction
    }

    private Context context;
    private List<String> deviceList;
    private BroadcastReceiver deviceScanReceiver;

    private LinearLayout deviceListLayout;
}
