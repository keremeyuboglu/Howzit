package com.example.howzit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import pub.devrel.easypermissions.EasyPermissions;

public class UserListActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {
    public static final String TAG = "UserListActivity";
    WifiP2pManager.Channel channel;
    WifiP2pManager manager;
    WifiBroadcastReceiver receiver;
    DeviceAdapter adapter;
    WifiP2pInfo minfo;
    private final IntentFilter intentFilter = new IntentFilter();
    private boolean isWifiP2pEnabled;
    Button button;
    Button btn2;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 15;
    public WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    public String othersAddress;
    private AppDatabase mydb;
    private List<Contact> myContacts;

    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    private boolean checkAndRequestPermissions() {
        int perm1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int perm2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        int perm3 = ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE);
        int perm4 = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (perm1 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (perm2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (perm3 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (perm4 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.INTERNET);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (checkAndRequestPermissions()) {
            // carry on the normal flow, as the case of  permissions  granted.
        }

        mydb = AppDatabase.getInstance(getApplicationContext());
        initialize_system(mydb.contactDao());

        myContacts = mydb.contactDao().getAll();
        // TODO: get list of users connected in connect screen pass them to recyclerview adapter check mac addresses between them, if match show user in rv



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        final Button btnScan = (Button) findViewById(R.id.button);
        RecyclerView rvContacts = (RecyclerView) findViewById(R.id.user_list_rv);


        final Button btnConnect = findViewById(R.id.button2);
        Context c = this;
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(c, ConnectActivity.class);
                startActivity(i);
            }
        });

        final Button btnCustomize = findViewById(R.id.customize);
        btnCustomize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(c, MainActivity.class);
              /*  WifiP2pDevice myDevice =(WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                i.putExtra("device name",)*/
                startActivity(i);
            }
        });


        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null)
            Log.d("peerdiscovery good", "FUCCCCCCCCCCCC");



        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiBroadcastReceiver(manager, channel, this, myContacts);


        if (manager != null && channel != null) {
            manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && manager != null && receiver != null) {
                        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.w(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.w(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }

        adapter = new DeviceAdapter(receiver.peers2, minfo, new DeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Log.d("item click", String.valueOf(position));
                WifiP2pDevice device = receiver.peers2.get(position);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                        Toast.makeText(UserListActivity.this, "Connecting to the contact, please wait",
                                Toast.LENGTH_SHORT).show();
                        othersAddress = config.deviceAddress;

                        //  ontoChat();

                      /*  Intent i = new Intent(getApplicationContext(),ChatActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);

                       */
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(UserListActivity.this, "Connect failed. Retry." + reason,
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        // Attach the adapter to the recyclerview to populate items
        rvContacts.setAdapter(adapter);
        // Set layout manager to position the items
        rvContacts.setLayoutManager(new LinearLayoutManager(getApplicationContext()));


        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank. Code for peer discovery goes in the
                // onReceive method, detailed below.

                Log.d("peerdiscovery good", "peerdisc");
            }

            @Override
            public void onFailure(int reasonCode) {

                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                Log.d("peerdiscovery bad", "peerdisc bad");
                Toast.makeText(UserListActivity.this, "Problem Finding Users, retry later",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Code for when the discovery initiation is successful goes here.
                        // No services have actually been discovered yet, so this method
                        // can often be left blank. Code for peer discovery goes in the
                        // onReceive method, detailed below.
                        Log.d("REFRESH", "NEW PEER DISCOVERY");
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(int reasonCode) {

                        // Code for when the discovery initiation fails goes here.
                        // Alert the user that something went wrong.
                        Log.d("peerdiscovery bad", "peerdisc bad" + reasonCode);
                    }
                });
            }
        });
    }


    // onConnectionInfoAvailable gets called when 2 devices connect to each other
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "pre connect " + Boolean.toString(info.groupFormed));
        if (info.groupFormed) {
            Intent intent = new Intent(UserListActivity.this, ChatActivity.class);
            intent.putExtra("info", info);
            intent.putExtra("name", othersAddress);
            startActivityForResult(intent, 1);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (manager != null && channel != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        if (group != null && manager != null && receiver != null) {
                            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                                @Override
                                public void onSuccess() {
                                    Log.w(TAG, "removeGroup onSuccess -");
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.w(TAG, "removeGroup onFailure -" + reason);
                                }
                            });
                        }
                    }
                });
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                // Code for when the discovery initiation is successful goes here.
                                // No services have actually been discovered yet, so this method
                                // can often be left blank. Code for peer discovery goes in the
                                // onReceive method, detailed below.

                                Log.d("peerdiscovery good", "peerdisc");
                            }

                            @Override
                            public void onFailure(int reasonCode) {

                                // Code for when the discovery initiation fails goes here.
                                // Alert the user that something went wrong.
                                Log.d("peerdiscovery bad", "peerdisc bad");
                            }
                        });
                    }
                }, 3000);
            }
        }
    }
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
        myContacts = mydb.contactDao().getAll();
        receiver.setContacts(myContacts);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }



    private void initialize_system(ContactDao d){

        if(d.getAll().size() != 0)
            return;


        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey secretKey;
        keyGenerator.init(256);
        secretKey = keyGenerator.generateKey();


        String encodedKey = android.util.Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);


        Contact c = new Contact();

        c.mac_address = getMacAddr();
        c.key = encodedKey;
        c.isUser = true;
        d.insert(c);


    }

    private String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }


}