package com.example.expirationtracker_java.data;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.expirationtracker_java.data.dao.CategoryDao;
import com.example.expirationtracker_java.data.dao.RecordDao;
import com.example.expirationtracker_java.data.entity.CategoryEntity;
import com.example.expirationtracker_java.data.entity.RecordEntity;

@androidx.room.Database(entities = {RecordEntity.class, CategoryEntity.class}, version = 2, exportSchema = false)
public abstract class Database extends RoomDatabase {
    private static volatile Database INSTANCE;
    public abstract RecordDao recordDao();
    public abstract CategoryDao categoryDao();

    public static Database getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            Database.class, "expiration_db"
                    )
                            // 開發階段(不用migration)：版本變化(升級)就整個 DB 砍掉重建
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
