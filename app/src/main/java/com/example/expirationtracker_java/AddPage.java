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
            categorySpinnerAdapter.notifyDataSetChanged();
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
