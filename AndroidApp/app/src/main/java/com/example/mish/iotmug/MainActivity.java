package com.example.mish.iotmug;

import android.content.Intent;
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

import static java.lang.Integer.getInteger;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    //-------------------------------------------------------------------------------
    public void setMugTemp(View view) {
        //send HTTP request setting temperature at ESP

        EditText edittext= (EditText) findViewById(R.id.editTemp);
        String tempStr = edittext.getText().toString();
        Toast.makeText(this,"Set temperature " + tempStr, Toast.LENGTH_SHORT).show();
        // now send tempStr to esp
        final TextView mTextView = (TextView) findViewById(R.id.setTemp);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="SET IP HERE/temperature/";

        //make PUT request to ESP editing temperature with tempStr
        StringRequest stringRequest=new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    // need to receive and send real number signal
                    public void onResponse(String response) {
                        mTextView.setText(response);
                    }
                },  new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText("Error");
            }
        });
        queue.add(stringRequest);

    }
    //------------------------------------------------------------------------------
    public void sendRefreshStateRequest(View view) {
        //send HTTP request requesting the Mug's current state (temperature, battery life)
        final TextView mTextView = (TextView) findViewById(R.id.currentTemp);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="SET IP HERE TOO/state/";

        StringRequest stringRequest=new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    // need to receive and send real number signal
                    public void onResponse(String response) {
                        mTextView.setText(response);
                    }
                },  new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText("Error");
            }
        });

        queue.add(stringRequest);
    }

    public void goToConnectScreen(View view) {
        //Send an intent to open the connect screen for connecting to the Mug
    }
    public void shutOff(View view) {
        // send signal we're all going to die turn it off
    }




}
