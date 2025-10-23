package com.example.expirationtracker_java.data.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class CategoryWithRecords {
    @Embedded public CategoryEntity category;
    @Relation(
            parentColumn = "cid",
            entityColumn = "cid"
    )
    public List<RecordEntity> recordEntityList;
}
