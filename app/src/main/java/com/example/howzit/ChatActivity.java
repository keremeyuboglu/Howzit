package com.example.howzit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener,WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    private static final String TAG = "Chat Activity";

    static private List<Message> Messages;
    static private MessageAdapter adapter;
    static private WifiP2pInfo info;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;
    static private ChatClient client;
    private ServerSocket serverSocket;
    private Socket socket;
    private ChatBroadcastReceiver receiver;
    private String name;
    private final IntentFilter intentFilter = new IntentFilter();
    InetAddress groupOwner = null;
    private int PORT = 8000;
    BufferedReader fromGroupOwner;
    PrintWriter toGroupOwner;
    static private AppDatabase db;
    public static RecyclerView rvMessages;
    public static String ownAddress;
    public String partnerName;
    public boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        // get db
        db = AppDatabase.getInstance(getApplicationContext());
        // Setup the Actionbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Set send Button listener
        ImageButton send = (ImageButton) findViewById(R.id.send);
        send.setOnClickListener(this);

        // get info from userlistactivity
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        info = (WifiP2pInfo) bundle.get("info");
        name= bundle.getString("name");

        // get your own mac address
        ownAddress = getMacAddr();
        initialize();

        // init db
        db = AppDatabase.getInstance(getApplicationContext());
        // init messages later to be changed
        Messages = db.messageDao().getFirst();
        rvMessages= findViewById(R.id.chat);
        client = null;

        // socket initialization

        //force portrait view
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // CHANGE THIS TO NAMES LATER
        if(info.isGroupOwner)
            getSupportActionBar().setTitle("Group Owner");
        else
            getSupportActionBar().setTitle("Client");

        // wifi broadcast stuff

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
        receiver = new ChatBroadcastReceiver(manager, channel, this);
        adapter = new MessageAdapter(Messages, getApplicationContext(), db);
        adapter.notifyDataSetChanged();
        rvMessages.setAdapter(adapter);
        rvMessages.setLayoutManager(new LinearLayoutManager(getApplicationContext()));


        // on keyboard change do not change textview
        if (Build.VERSION.SDK_INT >= 11) {
            rvMessages.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v,
                                           int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bottom < oldBottom) {
                        rvMessages.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rvMessages.smoothScrollToPosition(
                                            rvMessages.getAdapter().getItemCount() - 1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


                            }
                        }, 100);
                    }
                }
            });
        }
    }


    // socket init
    public void initialize (){

        Log.d("Chat", "is GroupOwner?" + Boolean.toString(info.isGroupOwner));
        if(client != null) {    client = null;}
        if(info.isGroupOwner){
            //wait for clients to make contact and add them to a list
            try {
                serverSocket = new ServerSocket(PORT);
            }catch(Exception e){
                Log.d("Chat", "afterServerSocket: "+ e.getMessage());

            }
            getClientInfo.execute();

        }else{
            //make contact with group owner
            try{
                connectToOwner.execute();


            }catch(Exception e){
                Log.d("Chat", "notGroupOwner "+ e.getMessage());

                e.printStackTrace();
            }

        }
    }

    AsyncTask<Void,Void,Void> connectToOwner = new AsyncTask<Void, Void, Void>()
    {
        @Override
        protected Void doInBackground(Void... voids)
        {
            InetAddress groupOwner = info.groupOwnerAddress;
            socket = new Socket();
            try
            {
                socket.connect(new InetSocketAddress(groupOwner.getHostAddress(), PORT));
                fromGroupOwner = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                toGroupOwner = new PrintWriter(socket.getOutputStream(), true);
                toGroupOwner.println(ownAddress);
                Log.d("Chat","Name sent to Owner");
                partnerName=fromGroupOwner.readLine();
                if(partnerName != null) {
                    Messages.clear();
                    Messages.addAll(db.messageDao().getChat(partnerName));
                    updateOnMain();
                }
                Log.d("Chat",partnerName);

            }
            catch (IOException e)
            {
                Log.e("Chat","Exception in connectToOwner" + e.getMessage());
                e.printStackTrace();
                finish(); // if cant connect to socket get out
            }
            return null;

        }
        @Override
        protected void onPostExecute(Void v){ listenToGroupOwner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); }
    };


    // mac address func
    public static String getMacAddr() {
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
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    // as group owner get client's mac address and populate rv
    AsyncTask<Void,Void,Void> getClientInfo = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {

            try{
                Log.d("Chat","waiting for client");
                Socket clientSocket = serverSocket.accept();
                Log.d("Chat","Client found");
                BufferedReader dataIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter dataOut = new PrintWriter(clientSocket.getOutputStream(),true);
                partnerName = dataIn.readLine();
                Messages.clear();
                Messages.addAll(db.messageDao().getChat(partnerName));
                updateOnMain();
                dataOut.println(ownAddress);
                ChatClient client = new ChatClient(partnerName, dataIn, dataOut);
                client.startListening();
                ChatActivity.client = client;
                Log.d("Chat", "client added");
            }catch(Exception e){
                Log.d("Chat","Exception in getClientInfo" + e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

    };

    AsyncTask<Void,Void,Void> listenToGroupOwner = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {
            try{
                Log.d("Chat","Started listening to GroupOwner");
                while(true){
                    String data;
                    if((data = fromGroupOwner.readLine()) != null){
                        Log.d("Chat","Calling receive");
                        ChatMessage chatMessage = new ChatMessage(data);
                        Message dbMessage = new Message();
                        dbMessage.owned = false;
                        dbMessage.text = chatMessage.getText();
                        dbMessage.time = chatMessage.getTime();
                        dbMessage.sender = chatMessage.getSender();
                        if(dbMessage.text != null) {
                            db.messageDao().insert(dbMessage);
                            Messages.add(dbMessage);
                            updateOnMain2();
                        }
                    }
                }
            }catch(Exception e){
                Log.d("Chat","Exception in listenToGroupOwner" + e.getMessage());
                e.printStackTrace();
                finish();
            }
            return null;
        }
    };


    // send listener, creates messages and sends it to server
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.send) {
            //get the text
            EditText editText = (EditText) findViewById(R.id.editText);
            String text = editText.getText().toString();
            //get current time ( I used the deprecated Class Time to ensure backwards compatability)
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            String minute;
            if (today.minute < 10) {
                minute = "0" + String.valueOf(today.minute);
            } else {
                minute = String.valueOf(today.minute);
            }
            String time = today.hour + ":" + minute;
            //add message to the list
            ChatMessage chatMessage = new ChatMessage(text.toString(), true, ownAddress, time);
            Message message = new Message();
            message.owned = chatMessage.isOwned();
            message.sender = partnerName;
            message.time = chatMessage.getTime();
            message.text = chatMessage.getText();
            Messages.add(message);
            updateOnMain2();
            db.messageDao().insert(message);
            editText.setText("");
            AsyncTask<ChatMessage,Void,Void> sendMessage = new AsyncTask<ChatMessage, Void, Void>() {

                @Override
                protected Void doInBackground(ChatMessage... chatMessage)
                {
                    Log.d("Chat","Sending Message");
                    if(info.isGroupOwner){
                        PrintWriter dataOut = client.getDataOut();
                        dataOut.println(chatMessage[0].getJSONString());
                        Log.d("Chat","Group Owner sent Message");

                    } else {
                        toGroupOwner.println(chatMessage[0].getJSONString());
                        Log.d("Chat","Client sent message");
                    }
                    return null;

                }

            };
            Log.d("Chat","Executing sendMessage");
            sendMessage.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,chatMessage);
        }
    }


    // top navigation go back
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                try{
                    if(info.isGroupOwner) {
                        serverSocket.close();
                    }else{
                        socket.close();
                    }
                }catch(Exception e){  }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds our add_people button to the action bar
        //getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // receive message and add to db
    public static void receive(String data){
        ChatMessage chatMessage = new ChatMessage(data);
        Log.d("Chat","received something");
        Log.d("Chat",chatMessage.getText());
        Message message = new Message();
        message.owned =  false;
        message.sender = chatMessage.getSender();
        message.time = chatMessage.getTime();
        message.text = chatMessage.getText();
        db.messageDao().insert(message);
        Messages.add(message);
        updateOnMain2();
    }

    // on destroy remove your connection(socket and wifi direct) with the other for consistency
    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        }
        try{
            if(info.isGroupOwner) {
                serverSocket.close();
            }else{
                socket.close();
            }
        }catch(Exception e){

        }

    }


    // RECYCLERVIEW HANDLER FOR ASYNC TAKS

    public static Handler UIHandler = new Handler(Looper.getMainLooper());
    public static void updateOnMain() {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("Chat","notifying adapter");
                adapter.notifyDataSetChanged();
                try {
                    rvMessages.smoothScrollToPosition(rvMessages.getAdapter().getItemCount() - 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("Chat","adapter notifyed");
            }
        });
    }

    public static Handler UIHandler2 = new Handler(Looper.getMainLooper());
    public static void updateOnMain2() {
        UIHandler2.post(new Runnable() {
            @Override
            public void run() {
                Log.d("Chat","notifying adapter");
                adapter.notifyItemInserted(adapter.getItemCount());
                rvMessages.smoothScrollToPosition(rvMessages.getAdapter().getItemCount() - 1);
                Log.d("Chat","adapter notifyed");
            }
        });
    }


    // FOR CONSISTENCY check if group is dissolved if it's go back to other page
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (!wifiP2pInfo.groupFormed) {
            if(info.isGroupOwner) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG,"CONNECTION LOST");
            finish();
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        if(!isInitialized) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    if (info.isGroupOwner) {
                        Collection<WifiP2pDevice> client = wifiP2pGroup.getClientList();
                        WifiP2pDevice mclient = client.iterator().next();
                        getSupportActionBar().setTitle(mclient.deviceName);
                    } else {
                        WifiP2pDevice boss = wifiP2pGroup.getOwner();
                        getSupportActionBar().setTitle(boss.deviceName);
                    }
                }
            }, 500);

            isInitialized = true;
        }
    }

    public void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}