package com.example.mish.iotmug;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class LocalMugsTab extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.connect_tab_local_mugs, container, false);

        context = getActivity().getApplicationContext();
        deviceList = new CopyOnWriteArrayList<>();
        deviceListLayout = rootView.findViewById(R.id.deviceListLayout);

        Button searchForDevices = rootView.findViewById(R.id.buttonSearchForDevices);
        searchForDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList(v);
            }
        });

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
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                for (int i =0; i<255;i++){
                    long startTime = System.currentTimeMillis();
                    try {
                        final InetAddress currentIP = InetAddress.getByName("192.168.1." + i);
                        if (currentIP.isReachable(100)) {
                            final int n = i;
                            final long latency = System.currentTimeMillis() - startTime;
                            final String hostname = currentIP.getCanonicalHostName();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context,  hostname + " is reachable within" + latency + " ms latency", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    catch(UnknownHostException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context,"Error: Unknown Host!",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    catch(IOException d){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context,"IO Exception caught!",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });



        //set up a broadcast receiver deviceScanReceiver to update layout listing all devices on the network once scanning is finished
        //create a threadpool that scans for local network devices by local IP
        //execute the threadpool and send the action that triggers deviceScanReceiver

    }
    public void  ipPinger() {
        for (int i =0; i<255;i++){
            long startTime = System.currentTimeMillis();
            try {
                if (InetAddress.getByName("192.168.1." + i).isReachable(100)) {
                    long latency = System.currentTimeMillis() - startTime;
                    Toast.makeText(context, "192.168.1." + i + " is reachable within" + latency + " ms latency", Toast.LENGTH_SHORT).show();
                }
            }
            catch(UnknownHostException e) {
                Toast.makeText(context,"Error",Toast.LENGTH_SHORT).show();
            }
            catch(IOException d){
                Toast.makeText(context,"Error 2",Toast.LENGTH_SHORT).show();
            }
        }

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

    private void writeToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private Context context;
    private CopyOnWriteArrayList<String> deviceList;
    private BroadcastReceiver deviceScanReceiver;
    private ThreadPoolExecutor deviceScanThreadPool;

    private LinearLayout deviceListLayout;
}
