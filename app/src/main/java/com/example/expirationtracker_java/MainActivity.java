package com.example.expirationtracker_java;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.expirationtracker_java.data.Repository;
import com.example.expirationtracker_java.data.entity.CategoryEntity;
import com.example.expirationtracker_java.data.entity.RecordEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final List<RecordEntity> allRecords = new ArrayList<>();   // 觀察到的全量
    private final List<RecordEntity> shownRecords = new ArrayList<>(); // 目前顯示的

    // Add button
    private FloatingActionButton fabAdd;

    // SearchView
    private SearchView searchView;
    private View rootView;

    // Filter 狀態
    enum FilterType {
        NONE,
        EXPIRED,
        SOON,
        SAFE
    }

    private FilterType currectFilter = FilterType.NONE;
    private String currentKeyword = "";   // 搜尋關鍵字

    private LinearLayout expiredBtn;
    private LinearLayout soonBtn;
    private LinearLayout safeBtn;

    // 日期格式：記得 expiredDate 需為 "dd/MM/yyyy"
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        askNotificationPermission();
        createNotificationChannel();
        scheduleDailySoonCheck();

        // 邊界處理（保留原始設定）
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ===== 1) 初始化 View =====
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

        // ===== 3) RecyclerView 基本設定 =====
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter();
        rvRecords.setAdapter(recordAdapter);

        // ===== 4) Spinner 設定 =====
        categorySpinnerAdapter = new CategorySpinnerAdapter(this, categoryList);
        spinner.setAdapter(categorySpinnerAdapter);

        // 觀察分類變化 → 更新 Spinner
        repository.getAllCategories().observe(this, categories -> {
            categoryList.clear();
            if (categories != null) categoryList.addAll(categories);
            categorySpinnerAdapter.notifyDataSetChanged();

            // 第一次進來預設選到 "All"（若有）
            if (!categoryList.isEmpty() && selectedCid == null) {
                int index = 0;
                for (int i = 0; i < categoryList.size(); i++) {
                    if ("All".equalsIgnoreCase(categoryList.get(i).cname)) {
                        index = i;
                        break;
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

        // ===== 5) 觀察 Record 全量 → 存到 allRecords，然後依目前所有條件過濾 =====
        repository.getAllRecord().observe(this, records -> {
            allRecords.clear();
            if (records != null) allRecords.addAll(records);
            applyAllFilters();  // ❗統一入口：種類 + 日期 + 搜尋
        });

        // 若一開始沒有資料，塞範例
        repository.getAllRecord().observe(this, rs -> {
            if (rs == null || rs.isEmpty()) {
                RecordEntity r1 = new RecordEntity();
                r1.cid = tryFindCidByName("Passport");
                r1.title = "Passport";
                r1.note = "Renew before summer";
                r1.expiredDate = "01/05/2026";   // dd/MM/yyyy
                r1.imagePath = "/storage/emulated/0/Download/passport.jpg";
                repository.insertRecord(r1);

                RecordEntity b = new RecordEntity();
                b.title = "Coupon - Billa";
                b.cid = tryFindCidByName("Coupons");
                b.expiredDate = "03/02/2026";
                repository.insertRecord(b);
            }
        });

        // ===== 6) Spinner 選取事件：改 selectedCid，然後 applyAllFilters =====
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CategoryEntity selected = categoryList.get(position);
                if ("All".equalsIgnoreCase(selected.cname)) {
                    selectedCid = null;
                } else {
                    selectedCid = selected.cid;
                }
                applyAllFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

        // ===== 7) SearchView 設定：只更新 currentKeyword + applyAllFilters =====
        // 點整個 searchView 區塊也可以展開
        searchView.setOnClickListener(v -> searchView.setIconified(false));

        // 點 rootView 取消 focus
        rootView.setOnClickListener(v -> {
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                currentKeyword = (newText == null) ? "" : newText.trim();
                applyAllFilters();
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                currentKeyword = (query == null) ? "" : query.trim();
                applyAllFilters();
                return true;
            }
        });

        // ===== 8) 日期篩選按鈕：改 currectFilter + 更新UI + applyAllFilters =====
        expiredBtn.setOnClickListener(v -> {
            if (currectFilter == FilterType.EXPIRED) {
                currectFilter = FilterType.NONE;
            } else {
                currectFilter = FilterType.EXPIRED;
            }
            updateFilterUI();
            applyAllFilters();
        });

        soonBtn.setOnClickListener(v -> {
            if (currectFilter == FilterType.SOON) {
                currectFilter = FilterType.NONE;
            } else {
                currectFilter = FilterType.SOON;
            }
            updateFilterUI();
            applyAllFilters();
        });

        safeBtn.setOnClickListener(v -> {
            if (currectFilter == FilterType.SAFE) {
                currectFilter = FilterType.NONE;
            } else {
                currectFilter = FilterType.SAFE;
            }
            updateFilterUI();
            applyAllFilters();
        });

        // ===== 9) Add 按鈕 → 新增/編輯頁 =====
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPage.class);
            startActivity(intent);
        });

        // ===== 10) Swipe to delete / edit =====
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    private final ColorDrawable background = new ColorDrawable();
                    private final int deleteColor =
                            ContextCompat.getColor(MainActivity.this, R.color.swipe_delete_red);
                    private final int editColor =
                            ContextCompat.getColor(MainActivity.this, R.color.swipe_edit_gray);
                    private final Drawable deleteIcon =
                            ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete_white_24dp);
                    private final Drawable editIcon =
                            ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_edit_white_24dp);

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        // false -> no dragging sort
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        RecordEntity record = recordAdapter.getRecordAt(position);
                        if (record == null) return;

                        if (direction == ItemTouchHelper.LEFT) {
                            // swipe from right to left：delete
                            repository.deleteRecord(record);
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            // swipe from left to right：edit
                            openEditPage(record);
                        }
                        // get item back to original position
                        recordAdapter.notifyItemChanged(position);
                    }

                    // when swiping
                    @Override
                    public void onChildDraw(@NonNull Canvas c,
                                            @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY,
                                            int actionState,
                                            boolean isCurrentlyActive) {

                        View itemView = viewHolder.itemView;
                        int itemHeight = itemView.getBottom() - itemView.getTop();

                        if (dX > 0) {
                            // swipe to right：edit（gray background + edit icon）
                            float progress = Math.min(1f, dX / itemView.getWidth());
                            int alpha = (int) (40 + 215 * progress);
                            int colorWithAlpha = ColorUtils.setAlphaComponent(editColor, alpha);

                            background.setColor(colorWithAlpha);
                            background.setBounds(
                                    itemView.getLeft(),
                                    itemView.getTop(),
                                    itemView.getLeft() + (int) dX,
                                    itemView.getBottom()
                            );
                            background.draw(c);

                            if (editIcon != null) {
                                int iconWidth = editIcon.getIntrinsicWidth();
                                int iconHeight = editIcon.getIntrinsicHeight();
                                int margin = dpToPx(24);

                                if (dX > iconWidth + margin * 2) {
                                    int iconLeft = itemView.getLeft() + margin;
                                    int iconRight = iconLeft + iconWidth;
                                    int iconTop = itemView.getTop() + (itemHeight - iconHeight) / 2;
                                    int iconBottom = iconTop + iconHeight;

                                    editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                    editIcon.draw(c);
                                }
                            }

                        } else if (dX < 0) {
                            // swipe to left：delete（red background + delete icon）
                            float progress = Math.min(1f, Math.abs(dX) / itemView.getWidth());
                            int alpha = (int) (120 + 105 * progress);
                            int colorWithAlpha = ColorUtils.setAlphaComponent(deleteColor, alpha);

                            background.setColor(colorWithAlpha);
                            background.setBounds(
                                    itemView.getRight() + (int) dX,
                                    itemView.getTop(),
                                    itemView.getRight(),
                                    itemView.getBottom()
                            );
                            background.draw(c);

                            if (deleteIcon != null) {
                                int iconWidth = deleteIcon.getIntrinsicWidth();
                                int iconHeight = deleteIcon.getIntrinsicHeight();
                                int margin = dpToPx(24);

                                if (Math.abs(dX) > iconWidth + margin * 2) {
                                    int iconRight = itemView.getRight() - margin;
                                    int iconLeft = iconRight - iconWidth;
                                    int iconTop = itemView.getTop() + (itemHeight - iconHeight) / 2;
                                    int iconBottom = iconTop + iconHeight;

                                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                    deleteIcon.draw(c);
                                }
                            }
                        } else {
                            background.setBounds(0, 0, 0, 0);
                        }

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvRecords);
    }

    /** 統一過濾：種類 + 日期 + 搜尋 */
    private void applyAllFilters() {
        shownRecords.clear();

        LocalDate today = LocalDate.now();
        String keywordLower = (currentKeyword == null) ? "" : currentKeyword.toLowerCase();

        for (RecordEntity r : allRecords) {

            // 1. 種類過濾
            if (selectedCid != null && r.cid != selectedCid) {
                continue;
            }

            // 2. 日期過濾
            boolean datePass = false;

            if (currectFilter == FilterType.NONE) {
                datePass = true;
            } else {
                try {
                    LocalDate expiry = LocalDate.parse(r.expiredDate, formatter);
                    long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);
                    long itemSoonDays = r.notifyDaysBefore;

                    switch (currectFilter) {
                        case EXPIRED:
                            if (daysDiff < 0) datePass = true;
                            break;
                        case SOON:
                            if (daysDiff >= 0 && daysDiff <= itemSoonDays) datePass = true;
                            break;
                        case SAFE:
                            if (daysDiff > itemSoonDays) datePass = true;
                            break;
                        default:
                            datePass = true;
                    }
                } catch (Exception e) {
                    // 日期格式錯誤時，這筆資料就略過或視為不通過
                    datePass = false;
                }
            }

            if (!datePass) continue;

            // 3. 搜尋過濾 (title)
            if (!keywordLower.isEmpty()) {
                if (r.title == null || !r.title.toLowerCase().contains(keywordLower)) {
                    continue;
                }
            }

            // 通過所有條件
            shownRecords.add(r);
        }

        recordAdapter.setRecords(shownRecords);
    }

    /** 更新日期篩選按鈕的 UI */
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

    /** 小工具：用分類名稱找 cid（找不到回傳 0） */
    private int tryFindCidByName(String name) {
        for (CategoryEntity c : categoryList) {
            if (c.cname != null && c.cname.equalsIgnoreCase(name)) return c.cid;
        }
        return 0;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "expiry_channel";
            String channelName = "Expiration Reminder";
            String channelDesc = "Notify when items are nearing expiration";

            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDesc);

            android.app.NotificationManager manager =
                    getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // run worker to daily check the soon item, and send notification
    private void scheduleDailySoonCheck() {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        // set the notification time
        nextRun.set(Calendar.HOUR_OF_DAY, 20);
        nextRun.set(Calendar.MINUTE, 20);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);

        // if notification time was past, it would be sent tomorrow
        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        // calculate the working period
        long initialDelay = nextRun.getTimeInMillis() - now.getTimeInMillis();

        // build the worker constraint (let the worker work in low battery mode)
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        // build the period worker (once a day) — 這裡原本是 MINUTES，如果要真的每天記得改成 DAYS
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(Notification.class, 1, TimeUnit.MINUTES) // debug: 1 分鐘
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "soon_daily_check",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }
    //    //test notification ( notify after running the app for 5 sec)
//    private void scheduleTestNotification() {
//        WorkRequest request =
//                new OneTimeWorkRequest.Builder(Notification.class)
//                        .setInitialDelay(5, TimeUnit.SECONDS)
//                        .build();
//
//        WorkManager.getInstance(this).enqueue(request);
//    }
    // need to ask the user "if allow the app to send the notification"
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) above
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }

    private void openEditPage(RecordEntity record) {
        Intent intent = new Intent(MainActivity.this, AddPage.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("record_id", record.rid);
        startActivity(intent);
    }

    // dp convert to px -> let onChildDraw can calculate the icon location
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
