package com.example.expirationtracker_java;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirationtracker_java.data.Repository;
import com.example.expirationtracker_java.data.entity.CategoryEntity;
import com.example.expirationtracker_java.data.entity.RecordEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private Button deleteBtn;

    //add button
    FloatingActionButton fabAdd;
    // SearchView
    private SearchView searchView;

    private View rootView;

    enum FilterType {
        NONE,
        EXPIRED,
        SOON,
        SAFE
    }
    private FilterType currectFilter = FilterType.NONE;

    private LinearLayout expiredBtn;
    private LinearLayout soonBtn;
    private LinearLayout safeBtn;


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
        searchView = findViewById(R.id.searchView);
        rootView = findViewById(R.id.main);
        expiredBtn = findViewById(R.id.expried_btn);
        soonBtn = findViewById(R.id.soon_btn);
        safeBtn = findViewById(R.id.safe_btn);

        // ===== 2) init repo =====
        repository = new Repository(getApplication());

        // ===== 3) RecyclerView 基本設定 ===== (下面的item列表)
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter();
        rvRecords.setAdapter(recordAdapter);
        // 在這裡設定動畫
        /*DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(150);
        animator.setRemoveDuration(120);
        animator.setMoveDuration(200);
        animator.setChangeDuration(0);  // 避免 change 閃爍

        // 這行非常重要：避免 DiffUtil 更新內容一直閃爍
        animator.setSupportsChangeAnimations(false);

        rvRecords.setItemAnimator(animator);*/

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
            if (records != null) allRecords.addAll(records);//資料庫的讀進來
            applyFilterAndShow(); // 呼叫function 根據 selectedCid 過濾並顯示
        });

        // ===== 6) Spinner 選取事件：更新 selectedCid，然後重算顯示 =====
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                CategoryEntity selected = categoryList.get(position);
                // 名稱叫 "All" 時顯示全部，否則用該分類的 cid 過濾
                if ("All".equalsIgnoreCase(selected.cname)) {
                    selectedCid = null;
                } else {
                    selectedCid = selected.cid;   // 直接用就好，不用再去 repository 查一次
                }
                applyFilterAndShow();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

        // 設定 searchView 可以點選整行啟動
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
        });

        // 解決 searchView 持續被選取狀態
        rootView.setOnClickListener(v -> {
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                filterByTitle(newText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                filterByTitle(query);
                return true;
            }
        });

        // 設定到期日篩選器
        expiredBtn.setOnClickListener(v -> {
            if (currectFilter == FilterType.EXPIRED) {
                currectFilter = FilterType.NONE;

            } else {
                currectFilter = FilterType.EXPIRED;
            }
            updateFilterUI();
            filterData();
        });

        soonBtn.setOnClickListener(v -> {
            if (currectFilter == FilterType.SOON) {
                currectFilter = FilterType.NONE;
            } else {
                currectFilter = FilterType.SOON;
            }
            updateFilterUI();
            filterData();
        });

        safeBtn.setOnClickListener(v -> {
            if (currectFilter == FilterType.SAFE) {
                currectFilter = FilterType.NONE;
            } else {
                currectFilter = FilterType.SAFE;
            }
            updateFilterUI();
            filterData();
        });

        // ===== (可選) 初始化塞兩筆測試 Record 看畫面 =====DELETE FROM record;
        repository.getAllRecord().observe(this, rs -> {
            if (rs == null || rs.isEmpty()) {
                RecordEntity r1 = new RecordEntity();
                r1.cid = tryFindCidByName("Passport");
                r1.title = "Passport";
                r1.note = "Renew before summer";
                r1.expiredDate = "01/05/2026";
                r1.imagePath = "/storage/emulated/0/Download/passport.jpg";
                repository.insertRecord(r1);

                RecordEntity b = new RecordEntity();
                b.title = "Coupon - Billa";
                b.cid = tryFindCidByName("Coupons");
                b.expiredDate = "03/02/2026";
                repository.insertRecord(b);
            }
        });


        fabAdd.setOnClickListener(v -> {

            // click add button to reach the add item page
            Intent intent = new Intent(MainActivity.this, AddPage.class);
            startActivity(intent);

        });

        // 刪除item
        recordAdapter.setOnDeleteClickListener(record -> {
            repository.deleteRecord(record);
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

    // 篩選功能
    private void filterByTitle(String keyword) {
        shownRecords.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            applyFilterAndShow();
            return;
        }

        String lower = keyword.toLowerCase().trim();

        for (RecordEntity r : allRecords) {
            if (r.title != null && r.title.toLowerCase().contains(lower)) {
                shownRecords.add(r);
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

    private void updateFilterUI() {
        if (currectFilter == FilterType.EXPIRED) {
            expiredBtn.setBackgroundResource(R.drawable.buttonstyle_selected);
        } else {
            expiredBtn.setBackgroundResource(R.drawable.buttonstyle);
        }

        if (currectFilter == FilterType.SOON) {
            soonBtn.setBackgroundResource(R.drawable.buttonstyle_selected);
        } else {
            soonBtn.setBackgroundResource(R.drawable.buttonstyle);
        }

        if (currectFilter == FilterType.SAFE) {
            safeBtn.setBackgroundResource(R.drawable.buttonstyle_selected);
        } else {
            safeBtn.setBackgroundResource(R.drawable.buttonstyle);
        }
    }

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private void filterData() {
        shownRecords.clear();

        LocalDate today = LocalDate.now();
        long soonDays = 14;

        for (RecordEntity r : allRecords) {

            // 1. 分類器過濾
            if (selectedCid != null && r.cid != selectedCid) {
                continue;
            }

            // 2. 日期過濾
            LocalDate expiry = LocalDate.parse(r.expiredDate, formatter);
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);

            boolean pass = false;

            switch (currectFilter) {
                case NONE:
                    pass = true;
                    break;

                case EXPIRED:
                    if (daysDiff < 0) pass = true;
                    break;

                case SOON:
                    if (daysDiff >= 0 && daysDiff <= soonDays) pass = true;
                    break;

                case SAFE:
                    if (daysDiff > soonDays) pass = true;
                    break;
            }

            if (pass) shownRecords.add(r);
        }

        recordAdapter.setRecords(shownRecords);
    }

}
