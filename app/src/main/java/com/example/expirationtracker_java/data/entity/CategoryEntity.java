package com.example.expirationtracker_java.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int cid;

    public String cname;
    // icon 放 drawable id
    public int iconId;
    public boolean createdViaUser;

    // ✅ 加上這個建構子：建立資料時能直接指定名稱
    public CategoryEntity(String cname) {
        this.cname = cname;
        this.iconId = 0;          // 預設沒圖示
        this.createdViaUser = false; // 預設不是使用者新增
    }

    // ✅ Room 需要一個空建構子（用來自動還原資料）
    public CategoryEntity() {
    }

    public String getName() {
        return cname;
    }
}
