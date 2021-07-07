package com.example.howzit;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "owned")
    public Boolean owned;

    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "time")
    public String time;

}
