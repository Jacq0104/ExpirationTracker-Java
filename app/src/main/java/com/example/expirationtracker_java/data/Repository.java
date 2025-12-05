package com.example.expirationtracker_java.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.expirationtracker_java.data.dao.CategoryDao;
import com.example.expirationtracker_java.data.dao.RecordDao;
import com.example.expirationtracker_java.data.entity.CategoryEntity;
import com.example.expirationtracker_java.data.entity.RecordEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {
    private final RecordDao recordDao;
    private final CategoryDao categoryDao;
    private final ExecutorService executor;

    public Repository(Application application) {
        Database db = Database.getDatabase(application);
        recordDao = db.recordDao();
        categoryDao = db.categoryDao();
        executor = Executors.newSingleThreadExecutor();
    }

    // Record
    public LiveData<List<RecordEntity>> getAllRecord() {
        return recordDao.getAllRecords();
    }
    public void insertRecord(RecordEntity record) {
        executor.execute(() -> recordDao.insertRecord(record));
    }
    public void deleteRecord(RecordEntity record) {
        executor.execute(() -> recordDao.deleteRecord(record));
    }

    // Category
    public LiveData<List<CategoryEntity>> getAllCategories() {
        return categoryDao.getAllCategories();
    }
    public void insertCategory(CategoryEntity category) {
        executor.execute(() -> categoryDao.insertCategory(category));
    }
    public void deleteCategory(CategoryEntity category) {
        executor.execute(() -> categoryDao.deleteCategory(category));
    }

    // get a record by id
    public LiveData<RecordEntity> getRecordById(int id) {
        return recordDao.getById(id);
    }

    // update Record
    public void updateRecord(RecordEntity r) {
        executor.execute(() -> recordDao.update(r));
    }

}
