package com.example.howzit;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages")
    List<Message> getAll();

    @Query("SELECT * FROM messages WHERE uid IN (:userIds)")
    List<Message> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM messages WHERE uid LIKE :first AND " +
            "owned LIKE :last LIMIT 1")
    Message findByName(String first, String last);

    @Query("SELECT * FROM messages WHERE sender IS :address")
    List<Message> getChat(String address);

    @Insert
    void insertAll(Message... users);

    @Insert(onConflict = 3)
    void insert(Message user);

    @Delete
    void delete(Message user);

    @Query("SELECT * FROM messages ORDER BY uid DESC LIMIT 1;")
    List<Message> getFirst();
}
