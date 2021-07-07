package com.example.howzit;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.Method;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public WifiP2pManager mManager;
    public WifiP2pManager.Channel mChannel;
    public EditText mText;
    private String TAG = "Customize";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mButton = (Button) findViewById(R.id.button3);
        mText = findViewById(R.id.editName);
        mButton.setOnClickListener((View.OnClickListener) this);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
      //  WifiP2pDevice device = (WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
     //   String thisDeviceName = device.deviceName;
        mChannel = mManager.initialize(this, getMainLooper(), null);

    }



    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button3) {
            try {
                Method m = mManager.getClass().getMethod("setDeviceName", new Class[]{WifiP2pManager.Channel.class, String.class,
                        WifiP2pManager.ActionListener.class});
                m.invoke(mManager, mChannel, mText.getText().toString(), new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Name change successful.");
                        Toast.makeText(MainActivity.this, "Changed name to " + mText.getText().toString(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "name change failed: " + reason);
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, "No such method");
            }
        }
    }
}