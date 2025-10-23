package com.example.expirationtracker_java;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import com.example.expirationtracker_java.data.Repository;
import com.example.expirationtracker_java.data.entity.CategoryEntity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Repository repository;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    private List<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ✅ 保留原有的 EdgeToEdge insets 設定
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ 初始化元件
        spinner = findViewById(R.id.spinner);
        repository = new Repository(getApplication());

        // ✅ 設定 Spinner Adapter（先給空資料，稍後 LiveData 更新）
        adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                categoryNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // ✅ 觀察資料庫變化（LiveData）
        repository.getAllCategories().observe(this, new Observer<List<CategoryEntity>>() {
            @Override
            public void onChanged(List<CategoryEntity> categoryEntities) {
                // 每當資料庫資料改變時，這裡會被自動呼叫
                categoryNames.clear();
                for (CategoryEntity category : categoryEntities) {
                    categoryNames.add(category.getName()); // 假設你的 Entity 有 getName()
                }
                adapter.notifyDataSetChanged();
            }
        });

        // ✅ 若資料庫是空的，插入一些測試資料
        repository.getAllCategories().observe(this, categories -> {
            if (categories == null || categories.isEmpty()) {
                repository.insertCategory(new CategoryEntity("All"));
                repository.insertCategory(new CategoryEntity("Passport"));
                repository.insertCategory(new CategoryEntity("🥤 飲料"));
                repository.insertCategory(new CategoryEntity("🍫 點心"));
            }
        });
    }
}
