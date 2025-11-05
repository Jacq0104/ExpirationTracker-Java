package com.example.expirationtracker_java;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;

public class AddPage extends AppCompatActivity {

    // 宣告介面元件
    EditText editTitle, editDate, editNote;
    Spinner spinnerCategory;
    Button btnTakePhoto;
    ImageView imagePreview;
    FloatingActionButton fabSave;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_page); // 載入你剛剛的畫面

        // 綁定 XML 中的元件
        editTitle = findViewById(R.id.editTitle);
        editDate = findViewById(R.id.editDate);
        editNote = findViewById(R.id.editNote);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imagePreview = findViewById(R.id.imagePreview);
        fabSave = findViewById(R.id.fabSave);

        // 日期選擇器
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

        // Spinner 選項
        String[] categories = {"Document", "Warranty", "Coupon"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // 拍照按鈕
        btnTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // 儲存按鈕（右下角勾勾）
        fabSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            String date = editDate.getText().toString();
            String category = spinnerCategory.getSelectedItem().toString();
            String note = editNote.getText().toString();

            // 簡單顯示資料
            Toast.makeText(this,
                    "Saved: " + title + " (" + category + ")",
                    Toast.LENGTH_SHORT).show();

            finish(); // 返回上一頁
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
