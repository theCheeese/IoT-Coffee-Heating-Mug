package com.example.mish.iotmug;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.getInteger;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void setMugTemp(View view) {
        //send HTTP PUT request setting temperature at ESP

        EditText tempTextBox = (EditText) findViewById(R.id.setTemp);
        final String tempStr = tempTextBox.getText().toString();

        if(validateMugTemp(tempStr) == false) {
            Toast.makeText(this, "Temperature Invalid!", Toast.LENGTH_SHORT).show();
            return;
        }

        // now send tempStr to esp
        final TextView currentTemp = (TextView) findViewById(R.id.currentTemp);
        final TextView batteryLife = (TextView) findViewById(R.id.batteryLife);
        final TextView shutoffText = (TextView) findViewById(R.id.shutoffText);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://IP HERE/temperature/";

        //make PUT request to ESP editing temperature with tempStr
        StringRequest stringRequest = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        findViewById(R.id.refreshButton).performClick();    //refresh state
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        displayError("Error: Please check your internet connection or reset the Mug.");
                    }
        })
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("TEMPERATURE", tempStr);
                return params;
            }
        };

        queue.add(stringRequest);

    }

    public void sendRefreshStateRequest(View view) {
        //send HTTP request requesting the Mug's current state (temperature, battery life)
        final TextView currentTemp = (TextView) findViewById(R.id.currentTemp);
        final TextView batteryLife = (TextView) findViewById(R.id.batteryLife);
        final TextView shutoffText = (TextView) findViewById(R.id.shutoffText);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://IP HERE/state/";

        StringRequest refreshRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    // need to receive and send real number signal
                    public void onResponse(String response) {
                        String[] responseComponents = response.split(" ");
                        currentTemp.setText(responseComponents[0]);
                        batteryLife.setText(responseComponents[1]);

                        if(responseComponents[2] == "0") {
                            shutoffText.setText("Emergency Shutoff Activated");
                            shutoffText.setVisibility(View.INVISIBLE);
                        }
                        if(responseComponents[2] == "1") {
                            shutoffText.setText("Emergency Shutoff Activated");
                            shutoffText.setVisibility(View.VISIBLE);
                        }
                        else {
                            shutoffText.setText("Emergency Shutoff has encountered an error and is disabled. " +
                                    "Please reset the Mug.");
                            shutoffText.setVisibility(View.VISIBLE);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        currentTemp.setText("xx C");
                        batteryLife.setText("xx%");
                        shutoffText.setText("Emergency Shutoff has encountered an error and is disabled. " +
                                "Please check your internet connection and push the shutoff button on the Mug.");
                        shutoffText.setVisibility(View.VISIBLE);
                    }
        });

        queue.add(refreshRequest);
    }

    public void goToConnectScreen(View view) {
        //Send an intent to open the connect screen for connecting to a new Mug
        Intent IPconnect=new Intent(this,ConnectMugActivity.class);
        startActivity (IPconnect);
    }

    public void shutOff(View view) {
        //Send emergency shutoff signal, resettable by a button at the Mug itself for safety

        final TextView shutoffText = (TextView) findViewById(R.id.shutoffText);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://IP HERE/shutoff";
        StringRequest shutoffRequest = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        findViewById(R.id.refreshButton).performClick(); //refresh state
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        shutoffText.setVisibility(View.VISIBLE);
                        shutoffText.setText("Emergency Shutoff Failed, Please Push the Shutoff Button on the Mug!");
                    }
                })
            {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("SHUTOFF", "1");
                    return params;
                }
            };

        queue.add(shutoffRequest);
    }

    public boolean validateMugTemp(String temperature) {
        if(temperature.isEmpty()) return false;
        int tempInt = Integer.parseInt(temperature);
        return tempInt > 0 && tempInt < 70; //temperature must be between 0 and 70
    }

    public void displayError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://IP HERE/shutoff";
        StringRequest shutoffRequest = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        findViewById(R.id.refreshButton).performClick(); //refresh state
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        shutoffText.setVisibility(View.VISIBLE);
                        shutoffText.setText("Emergency Shutoff Failed, Please Push the Shutoff Button on the Mug!");
                    }
                })
            {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("SHUTOFF", "1");
                    return params;
                }
            };

        queue.add(shutoffRequest);
    }

    public boolean validateMugTemp(String temperature) {
        if(temperature.isEmpty()) return false;
        int tempInt = Integer.parseInt(temperature);
        return tempInt > 0 && tempInt < 70; //temperature must be between 0 and 70
    }

    public void displayError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
