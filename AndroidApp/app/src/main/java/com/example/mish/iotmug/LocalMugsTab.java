package com.example.mish.iotmug;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//TODO: When the hardware for the Mug prototype is finished, use an HTTP request instead of pings so that we can find out if the local device is a Mug or not
//TODO: Speed up the local device search
//TODO: List host names of the Mugs instead of the bare IP
//TODO: Develop error dialogs for missing app permissions

//The tab for searching for Mugs on the local network. Mug must be connected to a wifi AP prior to being visible here
//This fragment returns the chosen IP to the MainActivity so that the user can connect to and control their chosen Mug

public class LocalMugsTab extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.connect_tab_local_mugs, container, false);

        context = getActivity().getApplicationContext();
        deviceList = new ArrayList<>();
        deviceListLayout = rootView.findViewById(R.id.deviceListLayout);

        deviceScanThreadPool = new ThreadPoolExecutor(NUM_IPS_TO_CHECK, MAX_THREADS, 500, TimeUnit.MILLISECONDS,
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

        deviceList.clear();

        //submit tasks into separate threads that scan for local network devices by local IP in groups of NUM_IPS_TO_CHECK
        final List< Future<List<InetAddress>> > threadResults = new ArrayList<>();

        //TODO: this does not start every single thread at the same time, the last two threads are executed sequentially. Find out why so that this can be sped up
        for(int startIpNum = 1; startIpNum < 255; startIpNum+=NUM_IPS_TO_CHECK) {
            Future< List<InetAddress> > futureReachableIPSubgroup = deviceScanThreadPool.submit(new PingCallable(NUM_IPS_TO_CHECK, startIpNum, context, getActivity()));
            threadResults.add(futureReachableIPSubgroup);
        }
        //TODO: figure out how to combine both these tasks into one thread-safely
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for(Future< List<InetAddress> > futureReachableIPSubgroup : threadResults) {
                        List<InetAddress> reachableIPSubgroup = futureReachableIPSubgroup.get();
                        if (!reachableIPSubgroup.isEmpty()) {
                            deviceList.addAll(reachableIPSubgroup);
                        }
                    }
                }
                catch(InterruptedException e) {
                    Log.e("Interrupted Exception", e.getMessage());
                    e.printStackTrace();
                }
                catch(ExecutionException e) {
                    Log.e("Execution Exception", e.getMessage());
                    e.printStackTrace();
                }
                if(getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderDeviceList();
                        }
                    });
                }
            }
        });
    }

    private void renderDeviceList() {
        deviceListLayout.removeAllViews();
        if(deviceList.isEmpty()) {
            Toast.makeText(context, "No devices found on this network!", Toast.LENGTH_SHORT).show();
        }
        for(InetAddress device : deviceList) {
            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout deviceListMember = (LinearLayout) layoutInflater.inflate(R.layout.connect_tab_member_mug_selection, null);
            deviceListLayout.addView(deviceListMember);

            TextView deviceListMemberId = deviceListMember.findViewById(R.id.id);
            deviceListMemberId.setText(device.getHostAddress());

            Button connectButton = deviceListMember.findViewById(R.id.connectButton);
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewGroup deviceView = (ViewGroup) v.getParent();
                    TextView IPView = deviceView.findViewById(R.id.id);
                    selectedIP = IPView.getText().toString();
                    returnActivityResult();
                }
            });
        }
    }

    public void returnActivityResult() {
        Toast.makeText(context, selectedIP + " selected", Toast.LENGTH_LONG).show();
        Intent returnData = new Intent();
        returnData.putExtra("result_ip", selectedIP);
        getActivity().setResult(Activity.RESULT_OK, returnData);
        getActivity().finish();
    }

    private final int MAX_THREADS = 300;
    private final int NUM_IPS_TO_CHECK = 15;

    private Context context;
    private List<InetAddress> deviceList;
    private ThreadPoolExecutor deviceScanThreadPool;
    private String selectedIP;

    private LinearLayout deviceListLayout;
}
