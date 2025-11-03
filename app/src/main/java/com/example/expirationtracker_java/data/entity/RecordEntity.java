package com.example.expirationtracker_java.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "record")
public class RecordEntity {
    @PrimaryKey(autoGenerate = true)
    public int rid;
    public int cid;
    public String title;
    public String note;
    //    room 不能存時間戳記
    public String expiredDate;
    public String imagePath;
}
