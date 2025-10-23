package com.example.expirationtracker_java.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expirationtracker_java.data.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    public void insertCategory(CategoryEntity categoryEntity);
    @Update
    public void updateCategory(CategoryEntity categoryEntity);
    @Delete
    public void deleteCategory(CategoryEntity categoryEntity);
    @Query("SELECT * FROM category ORDER BY cid ASC")
    public LiveData<List<CategoryEntity>> getAllCategories();
}
