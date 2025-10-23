package com.example.expirationtracker_java.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expirationtracker_java.data.entity.RecordEntity;

import java.util.List;

@Dao
public interface RecordDao {
    @Insert
    public void insertRecord(RecordEntity recordEntity);
    @Update
    public void updateRecord(RecordEntity recordEntity);
    @Delete
    // 參數應該帶 id ?
    public void deleteRecord(RecordEntity recordEntity);
    @Query("SELECT * FROM record ORDER BY expiredDate ASC")
    public LiveData<List<RecordEntity>> getAllRecords();
}
