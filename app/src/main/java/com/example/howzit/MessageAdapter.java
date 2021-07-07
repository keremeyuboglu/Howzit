package com.example.howzit;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> dbMessages;
    private DeviceAdapter.OnItemClickListener onItemClickListener; // Global scope
    private AppDatabase db;
    private Context context;
    // Pass in the contact array into the constructor
    public MessageAdapter(List<Message> messages, Context context, AppDatabase db) {
        dbMessages = messages;
        this.context = context;
        this.db = db;
    }


    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.activity_chat_list_rv, parent, false);

        // Return a new holder instance
        MessageAdapter.ViewHolder viewHolder = new MessageAdapter.ViewHolder(context, contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Message chatMessage = dbMessages.get(position);
        // according to who send it inflate layout
        if (chatMessage.owned)
            holder.setGravity(Gravity.END);
        else
            holder.setGravity(Gravity.START);
        // Set item views based on your views and data model
        TextView textView = holder.nameTextView;
        textView.setText(chatMessage.text);
        TextView textView2 = holder.timeStamp;
        textView2.setText(chatMessage.time);
    }

    @Override
    public int getItemCount() {
        return dbMessages.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public RelativeLayout chatBox;
        public TextView nameTextView;
        public TextView timeStamp;
        public Context context;
        public ImageButton button;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);
            this.context = context;
            nameTextView = (TextView) itemView.findViewById(R.id.msg);
            timeStamp = (TextView) itemView.findViewById(R.id.time_stamp);
            chatBox = (RelativeLayout) itemView.findViewById(R.id.chat_bubble);
            button = (ImageButton) itemView.findViewById(R.id.chat_delete_btn); // delete buttton
            button.setVisibility(GONE); // make it invisible
            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
            button.setOnClickListener(this);

        }
        public void setGravity(int gravity) {
            chatBox.setGravity(gravity);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition(); // gets item position
            Log.d(TAG,"IT WORKS");
            if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
              //  WifiP2pDevice device = mMessages.get(position);
            }
            // if on click on delete picture delete it from db and view
            if(view == button) {
                db.messageDao().delete(dbMessages.get(position));
                dbMessages.remove(position);
                notifyItemRemoved(position);
            }
        }

        // on persistent click to message show delete button for 2 secs
        @Override
        public boolean onLongClick(View view) {
            int position = getAdapterPosition();
            button.setVisibility(View.VISIBLE);
            button.postDelayed(new Runnable() {
                @Override
                public void run() {
                    button.setVisibility(GONE);
                }
            }, 2000); // where 1000 is equal to 1 sec (1 * 1000)
            Log.d(TAG, String.valueOf(position));
            return false;
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(int position);
    }

}