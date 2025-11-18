package com.example.expirationtracker_java;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.expirationtracker_java.data.Repository;
import com.example.expirationtracker_java.data.entity.CategoryEntity;
import com.example.expirationtracker_java.data.entity.RecordEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import com.google.android.material.appbar.MaterialToolbar;


public class AddPage extends AppCompatActivity {

    // declare the interface elements
    EditText editTitle, editDate, editNote;
    Spinner spinnerCategory;
    FloatingActionButton btnTakePhoto;
    ImageView imagePreview;
    FloatingActionButton fabSave;
    MaterialToolbar toolbar;

    //添加"從資料庫讀"的功能
    private Repository repository;
    private CategorySpinnerAdapter categorySpinnerAdapter;
    private final java.util.List<com.example.expirationtracker_java.data.entity.CategoryEntity> categoryList =
            new java.util.ArrayList<>();

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoUri;

    //添加category新增方式
    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Category name");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 建一個新的 CategoryEntity，標記為使用者自己建立
                    CategoryEntity newCategory = new CategoryEntity(name);
                    newCategory.createdViaUser = true;

                    // 寫進資料庫，Room 會自動幫你 assign 新的 cid
                    repository.insertCategory(newCategory);
                    // insert 後 LiveData 會觸發 observer，上面的 Spinner 就會自動刷新
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_page); // loading xml

        // 綁定 XML 中的元件
        editTitle = findViewById(R.id.editTitle);
        editDate = findViewById(R.id.editDate);
        editNote = findViewById(R.id.editNote);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imagePreview = findViewById(R.id.imagePreview);
        fabSave = findViewById(R.id.fabSave);
        toolbar = findViewById(R.id.toolbar);

        // 建 Repository
        repository = new Repository(getApplication());

        // 建 Spinner 的 adapter（用你們自己寫的 CategorySpinnerAdapter）
        categorySpinnerAdapter = new CategorySpinnerAdapter(this, categoryList);
        spinnerCategory.setAdapter(categorySpinnerAdapter);

        // 從資料庫讀 category，更新 Spinner
        repository.getAllCategories().observe(this, categories -> {
            categoryList.clear();
            if (categories != null) {
                categoryList.addAll(categories);
            }

            // 在最後加一個「Add new category」的假項目
            CategoryEntity addItem = new CategoryEntity("＋ Add new category");
            addItem.cid = -1;              // 用 -1 當作「這是假的，不能拿來存 record」
            categoryList.add(addItem);

            categorySpinnerAdapter.notifyDataSetChanged();
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                // 如果選到的是最後一個項目（＋ Add new category）
                if (position == categoryList.size() - 1) {
                    // 先改回第一個選項（不要停在Add new category）
                    if (categoryList.size() > 1) {
                        spinnerCategory.setSelection(0); // 或者上一個選項 setSelection(categoryList.size() - 2);
                    }

                    showAddCategoryDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });




        // Toolbar 返回箭頭功能
        toolbar.setNavigationOnClickListener(v -> {
            // 回主畫面
            Intent intent = new Intent(AddPage.this, MainActivity.class);
            startActivity(intent);
            finish(); // 關閉目前的 AddPage
        });

        // data selector
        editDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> editDate.setText(String.format("%02d/%02d/%04d", d, (m + 1), y)),
                    year, month, day
            );
            dialog.show();
        });

        // Spinner 寫死category(測試用)
//        String[] categories = {"Document", "Warranty", "Coupon"};
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
//                android.R.layout.simple_spinner_item, categories);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerCategory.setAdapter(adapter);

        // 拍照按鈕
        btnTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // bottom right save fab
        fabSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            String date = editDate.getText().toString();
            String note = editNote.getText().toString();

            //String category = spinnerCategory.getSelectedItem().toString();
            CategoryEntity selected = (CategoryEntity) spinnerCategory.getSelectedItem();
            //怕使用者在添加新的category選add new category就直接按save(這樣cid=-1會存進去database)
            if (selected == null || selected.cid == -1) {
                Toast.makeText(this, "請先選擇一個分類", Toast.LENGTH_SHORT).show();
                return;
            }

            int cid = selected.cid;           // 要存到 Record 的外鍵
            String categoryName = selected.cname; // 要顯示在 Toast 才用到

            // UI: show simple words after clicking the save fab
            Toast.makeText(this,
                    "Saved: " + title + " (" + categoryName + ")",
                    Toast.LENGTH_SHORT).show();

            // 寫進資料庫!!!
            RecordEntity r = new RecordEntity();
            r.title = title;
            r.expiredDate = date;
            r.note = note;
            r.cid = cid;            // ← 重點是這行！
            // 如果有照片：r.imagePath = ...

            repository.insertRecord(r);

            // 回主畫面
            Intent intent = new Intent(AddPage.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // 拍照結果回傳
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                // 預覽拍到的縮圖
                imagePreview.setImageBitmap((android.graphics.Bitmap) extras.get("data"));
            }
        }
    }
}
