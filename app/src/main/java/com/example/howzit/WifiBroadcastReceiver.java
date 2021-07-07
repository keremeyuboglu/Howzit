package com.example.howzit;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private UserListActivity mActivity;
    public List<WifiP2pDevice> peers2 = new ArrayList<WifiP2pDevice>();
    private WifiP2pManager.PeerListListener myPeerListListener;
    private boolean initalized = false;
    private List<Contact> contactList;

    public WifiBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, UserListActivity mActivity, List<Contact> list) {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;
        this.contactList = list;
    }

    public void setContacts(List<Contact> list){
        contactList = list;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("hallalsd",action);

        if(!initalized){
            if (mManager != null) {
                mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        peers2.clear();
                        Log.d("hallalsd",String.format("PeerListListener: %d peers available, updating device list", peers.getDeviceList().size()));



                        for (  WifiP2pDevice peer : peers.getDeviceList()) {
                            WifiP2pDevice device=peer;
                            //here get the device info
                            String deviceaddr= device.deviceAddress;
                            String name= device.deviceName;
                            Log.d("deviceaddr:",deviceaddr);
                            Log.d("name:",name);
                            char[] myNameChars = deviceaddr.toCharArray();
                            if(myNameChars[1] == 'a')
                                myNameChars[1] = '8';
                            else if(myNameChars[1] == 'b')
                                myNameChars[1] = '9';
                            else
                                myNameChars[1] -=  2;
                            deviceaddr =  String.valueOf(myNameChars);

                            Contact curr_contact;
                            boolean notincontacts = true;
                            for(Contact c : contactList)
                            {
                                if(deviceaddr.equals(c.mac_address))
                                {
                                    notincontacts = false;
                                    curr_contact = c;
                                    break;
                                }
                            }

                            if(notincontacts)
                            {
                                continue;
                            }

                            peers2.add(device);
                        }

                        // DO WHATEVER YOU WANT HERE
                        // YOU CAN GET ACCESS TO ALL THE DEVICES YOU FOUND FROM peers OBJECT

                    }
                });
            }
            initalized = true;

        }


        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mActivity.setIsWifiP2pEnabled(true);
            } else {
                mActivity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                Log.d("WifiBroadcastReceiver","no manager");
            }
            if (mManager != null) {
                mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        peers2.clear();
                        Log.d("WifiBroadcastReceiver  ",String.format("PeerListListener: %d peers available, updating device list", peers.getDeviceList().size()));
                        for (  WifiP2pDevice peer : peers.getDeviceList()) {
                            WifiP2pDevice device=peer;
                            //here get the device info
                            String deviceaddr= device.deviceAddress;
                            String name= device.deviceName;
                            Log.d("deviceaddr:",deviceaddr);
                            Log.d("name:",name);
                            char[] myNameChars = deviceaddr.toCharArray();
                            if(myNameChars[1] == 'a')
                                myNameChars[1] = '8';
                            else if(myNameChars[1] == 'b')
                                myNameChars[1] = '9';
                            else
                                myNameChars[1] -=  2;
                            deviceaddr =  String.valueOf(myNameChars);

                            Contact curr_contact;
                            boolean notincontacts = true;
                            for(Contact c : contactList)
                            {
                                if(deviceaddr.equals(c.mac_address))
                                {
                                    notincontacts = false;
                                    curr_contact = c;
                                    break;
                                }
                            }

                            if(notincontacts)
                            {
                                continue;
                            }

                            peers2.add(device);/*
                            if(device.deviceAddress.equals("somedevice")){
                                Toast.makeText(ctx, "Server  Name "+device.deviceName,Toast.LENGTH_LONG).show();
                                WifiP2pConfig config = new WifiP2pConfig();
                                config.deviceAddress = device.deviceAddress;
                            }*/

                        }
                        mActivity.adapter.notifyDataSetChanged(); // notify rv
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {



            // DEVICE CONNECTED

            // TODO
            /*
            GO TO CHAT ACTIVITY WITH THIS INFO



             */


            /*

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d("WifiBroadcast:","did connect" + networkInfo.getDetailedState());
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
/*
                Intent i = new Intent(context,ChatActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } else {
                // It's a disconnect
                Log.d("WifiBroadcast:","couldn't connect");
            }


        */
            mManager.requestConnectionInfo(mChannel, (WifiP2pManager.ConnectionInfoListener) mActivity);
            // Connection state changed! We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {


        }
    }


}


