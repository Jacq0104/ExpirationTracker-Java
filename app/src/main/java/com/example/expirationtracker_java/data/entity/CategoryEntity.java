package com.example.expirationtracker_java.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int cid;
    public String cname;
    //    將icon放到res/drawable用id對應就好
    public int iconId;
    public boolean createdViaUser;
}
