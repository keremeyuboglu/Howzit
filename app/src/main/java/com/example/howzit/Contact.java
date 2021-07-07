package com.example.howzit;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "contacts")
public class Contact {

    @ColumnInfo(name = "isUser", defaultValue = "0")
    public boolean isUser;

    @ColumnInfo(name = "encryption_key")
    public String key;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "mac_address")
    public String mac_address;

    @ColumnInfo(name = "local_ip")
    public String local_ip;
}
