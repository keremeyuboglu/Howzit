package com.example.howzit;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// RECYCLERVIEW ADAPTER
public class DeviceAdapter extends
        RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<WifiP2pDevice> mDevices;
    private OnItemClickListener onItemClickListener; // Global scope
    private WifiP2pInfo mInfo;

    // Pass in the contact array into the constructor
    public DeviceAdapter(List<WifiP2pDevice> devices, WifiP2pInfo info, OnItemClickListener onItemClickListener) {
        mDevices = devices;
        mInfo = info;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.activity_user_list_rv, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(context, contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WifiP2pDevice device = mDevices.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.nameTextView;
        textView.setText(device.deviceName);
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameTextView;
        public Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);
            this.context = context;
            nameTextView = (TextView) itemView.findViewById(R.id.user);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClickListener.onItemClicked(getAdapterPosition());
                }
            });
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition(); // gets item position
            if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                WifiP2pDevice device = mDevices.get(position);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(int position);
    }
}