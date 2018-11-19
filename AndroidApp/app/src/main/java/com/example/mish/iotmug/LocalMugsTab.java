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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalMugsTab extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.connect_tab_local_mugs, container, false);

        context = getActivity().getApplicationContext();
        deviceList = new ArrayList<>();
        deviceListLayout = rootView.findViewById(R.id.deviceListLayout);

        deviceScanThreadPool = new ThreadPoolExecutor(0, MAX_THREADS, 500, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(MAX_THREADS/NUM_IPS_TO_CHECK));

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

        //submit tasks into separate threads that scan for local network devices by local IP in groups of NUM_IPS_TO_CHECK

        List< Future<List<InetAddress>> > threadResults = new ArrayList<>();


        for(int startIpNum = 1; startIpNum < 255; startIpNum+=NUM_IPS_TO_CHECK) {
            Future< List<InetAddress> > reachableIPSubgroup = deviceScanThreadPool.submit(new PingCallable(NUM_IPS_TO_CHECK, startIpNum, context, getActivity()));
            threadResults.add(reachableIPSubgroup);
        }

        for(Future< List<InetAddress> > reachableIPSubgroup : threadResults) {
            //TODO: Combine all thread results into deviceList as an AsyncTask
            //TODO: (so that the UI thread doesn't have to wait for the threads to finish)
        }

        //TODO: render device list into deviceListLayout

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

    private final int MAX_THREADS = 255;
    private final int NUM_IPS_TO_CHECK = 5;

    private Context context;
    private List<String> deviceList;
    private BroadcastReceiver deviceScanReceiver;
    private ThreadPoolExecutor deviceScanThreadPool;

    private LinearLayout deviceListLayout;
}
