package com.example.expirationtracker_java.data.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public interface CategoryAndRecords {
    @Transaction
    @Query("SELECT * FROM record WHERE cid = :cid")
    CategoryAndRecords getRecordsOfCategoryById(int cid);
}
