package com.example.mish.iotmug;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class PingCallable implements Callable<List<InetAddress>> {
    public PingCallable(int numIPsToCheck, int startIpNum, Context context, Activity currentActivity) {
        this.startIpNum = startIpNum;
        this.numIPsToCheck = numIPsToCheck;
        this.context = context;
        this.currentActivity = currentActivity;
    }

    @Override
    public List<InetAddress> call() {
        List<InetAddress> reachableIPs = new ArrayList<>();

        Log.e("PingCallable", "Called on startIP " + startIpNum + " and " + numIPsToCheck + " IPs to check");

        for (int i = startIpNum; (i < startIpNum + numIPsToCheck) && (i < 255); i++) {
            try {
                final InetAddress currentIP = InetAddress.getByName("192.168.1." + i);
                if (currentIP.isReachable(300)) {
                    reachableIPs.add(currentIP);
                    Log.e("PingCallable", "Found reachable IP on " + currentIP.getHostAddress());
                }
            } catch (UnknownHostException e) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Error: Unknown Host!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException d) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "IO Exception caught!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return reachableIPs;
    }

    private final Context context;
    private final Activity currentActivity;
    private final int startIpNum;
    private final int numIPsToCheck;
}
