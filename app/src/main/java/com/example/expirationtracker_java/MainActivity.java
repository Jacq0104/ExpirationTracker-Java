package com.example.expirationtracker_java;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirationtracker_java.data.Repository;
import com.example.expirationtracker_java.data.entity.CategoryEntity;
import com.example.expirationtracker_java.data.entity.RecordEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Data / repo
    private Repository repository;

    // Spinner
    private Spinner spinner;
    private CategorySpinnerAdapter categorySpinnerAdapter;
    private final List<CategoryEntity> categoryList = new ArrayList<>();
    private Integer selectedCid = null;   // null = 全部

    // RecyclerView
    private RecyclerView rvRecords;
    private RecordAdapter recordAdapter;
    private final List<RecordEntity> allRecords = new ArrayList<>(); // 觀察到的全量
    private final List<RecordEntity> shownRecords = new ArrayList<>(); // 目前顯示的

    //add button
    FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 邊界處理（保留原始設定）
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ===== 1) init views =====
        spinner = findViewById(R.id.spinner);
        rvRecords = findViewById(R.id.rvRecords);
        fabAdd = findViewById(R.id.fabAdd);

        // ===== 2) init repo =====
        repository = new Repository(getApplication());

        // ===== 3) RecyclerView 基本設定 ===== (下面的item列表)
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter();
        rvRecords.setAdapter(recordAdapter);

        // ===== 4) Spinner 設定（用專屬 Adapter）===== (上面的下拉清單)
        categorySpinnerAdapter = new CategorySpinnerAdapter(this, categoryList);
        spinner.setAdapter(categorySpinnerAdapter);

        // 觀察分類變化 → 更新 Spinner
        repository.getAllCategories().observe(this, categories -> {
            categoryList.clear();
            categoryList.addAll(categories);
            categorySpinnerAdapter.notifyDataSetChanged();

            // 若第一次進來想預設「All」（或第一個），可以這樣：
            if (!categoryList.isEmpty() && selectedCid == null) {
                // 嘗試把名稱為 "All" 的設為選取；找不到就選第 0 個
                int index = 0;
                for (int i = 0; i < categoryList.size(); i++) {
                    if ("All".equalsIgnoreCase(categoryList.get(i).cname)) {
                        index = i; break;
                    }
                }
                spinner.setSelection(index);
            }
        });

        // 空表時放一些預設分類（可留可拿掉）
        repository.getAllCategories().observe(this, categories -> {
            if (categories == null || categories.isEmpty()) {
                repository.insertCategory(new CategoryEntity("All"));
                repository.insertCategory(new CategoryEntity("Passport"));
                repository.insertCategory(new CategoryEntity("Warranty cards"));
                repository.insertCategory(new CategoryEntity("Coupons"));
                repository.insertCategory(new CategoryEntity("Others"));
            }
        });

        // ===== 5) 觀察 Record 全量 → 存到 allRecords，然後依目前選取的 cid 過濾顯示 =====
        repository.getAllRecord().observe(this, records -> {
            allRecords.clear();
            if (records != null) allRecords.addAll(records);
            applyFilterAndShow(); // 根據 selectedCid 過濾並顯示
        });

        // ===== 6) Spinner 選取事件：更新 selectedCid，然後重算顯示 =====
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                CategoryEntity selected = categoryList.get(position);
                // 名稱叫 "All" 時顯示全部，否則用該分類的 cid 過濾
                if ("All".equalsIgnoreCase(selected.cname)) {
                    selectedCid = null;
                } else {
                    selectedCid = selected.cid;
                }
                applyFilterAndShow();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

        // ===== (可選) 初始化塞兩筆測試 Record 看畫面 =====
        repository.getAllRecord().observe(this, rs -> {
            if (rs == null || rs.isEmpty()) {
                RecordEntity r1 = new RecordEntity();
                r1.title = "Passport";
                r1.note = "Renew before summer";
                r1.expiredDate = "2026-05-01";
                r1.imagePath = "/storage/emulated/0/Download/passport.jpg";
                repository.insertRecord(r1);

                RecordEntity b = new RecordEntity();
                b.title = "Coupon - Starbucks";
                b.cid = tryFindCidByName("Coupons");
                repository.insertRecord(b);
            }
        });


        fabAdd.setOnClickListener(v -> {

            // click add button to reach the add item page
            Intent intent = new Intent(MainActivity.this, AddPage.class);
            startActivity(intent);

        });
    }

    /** 依 selectedCid 過濾 allRecords，丟給 RecyclerView */
    private void applyFilterAndShow() {
        shownRecords.clear();
        if (selectedCid == null) {
            shownRecords.addAll(allRecords); // 全部
        } else {
            for (RecordEntity r : allRecords) {
                if (r.cid == selectedCid) shownRecords.add(r);
            }
        }
        recordAdapter.setRecords(shownRecords);
    }

    /** 小工具：用分類名稱找 cid（找不到回傳 0） */
    private int tryFindCidByName(String name) {
        for (CategoryEntity c : categoryList) {
            if (c.cname != null && c.cname.equalsIgnoreCase(name)) return c.cid;
        }
        return 0;
    }
}
