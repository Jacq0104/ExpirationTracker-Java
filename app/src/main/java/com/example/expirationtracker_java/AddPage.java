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
    Spinner spinnerNotifyDays;
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
    static final int REQUEST_IMAGE_PICK = 2;  // 「從相簿選圖片」用的 request code
    Uri photoUri;                             // 選到的圖的path
    String imagePath;
    int selectedNotifyDays = 14;

    // about edit
    private boolean isEditMode = false;
    private int editingRecordId = -1;
    private RecordEntity editingRecord;
    private final Integer[] notifyOptions = {1, 3, 7, 14, 30};


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

        // 綁定 XML中的元件
        editTitle = findViewById(R.id.editTitle);
        editDate = findViewById(R.id.editDate);
        editNote = findViewById(R.id.editNote);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerNotifyDays = findViewById(R.id.spinnerNotifyDays);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imagePreview = findViewById(R.id.imagePreview);
        fabSave = findViewById(R.id.fabSave);
        toolbar = findViewById(R.id.toolbar);

        // 建 Repository
        repository = new Repository(getApplication());

        // whether the edit data brought from MainActivity
        Intent intent = getIntent();
        if (intent != null && "edit".equals(intent.getStringExtra("mode"))) {
            isEditMode = true;
            editingRecordId = intent.getIntExtra("record_id", -1);
        }

        // 建 Spinner 的 adapter（用你們自己寫的 CategorySpinnerAdapter）
        categorySpinnerAdapter = new CategorySpinnerAdapter(this, categoryList);
        spinnerCategory.setAdapter(categorySpinnerAdapter);

        // 從資料庫讀 category，更新 Spinner
        repository.getAllCategories().observe(this, categories -> {
            categoryList.clear();
            for (CategoryEntity c : categories) {
                // 預設不要顯示all
                if (!"All".equalsIgnoreCase(c.cname)) {
                    categoryList.add(c);
                }
            }

            // 在最後加「Add new category」
            CategoryEntity addItem = new CategoryEntity("＋ Add new category");
            addItem.cid = -1;              // 用不合理的cid來避免存到 record」
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
            startActivity(new Intent(AddPage.this, MainActivity.class));
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
//        btnTakePhoto.setOnClickListener(v -> {
//            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//            }
//        });

        // Notify me 的天數選項
        ArrayAdapter<Integer> notifyAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                notifyOptions
        );
        notifyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotifyDays.setAdapter(notifyAdapter);

        // 預設選在 14（index 3）
        spinnerNotifyDays.setSelection(3);

        spinnerNotifyDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedNotifyDays = notifyOptions[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });


        // 從相簿選擇圖片
        btnTakePhoto.setOnClickListener(v -> {
            Intent pickIntent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );
            startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
        });

        // bottom right save fab
        fabSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            String date = editDate.getText().toString();
            String note = editNote.getText().toString();

            // error prevention: check title
            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                Toast.makeText(this, "Please enter Title", Toast.LENGTH_SHORT).show();
                return;
            }

            // check date
            if (date.isEmpty()) {
                editDate.setError("Expiration Date is required");
                editDate.requestFocus();
                Toast.makeText(this, "Please choose Expiration Date", Toast.LENGTH_SHORT).show();
                return;
            }

            //String category = spinnerCategory.getSelectedItem().toString();
            CategoryEntity selected = (CategoryEntity) spinnerCategory.getSelectedItem();
            //check category
            // 怕使用者在添加新的category選add new category就直接按save(這樣cid=-1會存進去database)
            if (selected == null || selected.cid == -1) {
                Toast.makeText(this, "Please choose a category", Toast.LENGTH_SHORT).show();
                return;
            }

            int cid = selected.cid;           // 要存到 Record 的外鍵
            String categoryName = selected.cname; // 要顯示在 Toast 才用到

            if (isEditMode && editingRecord != null) {
                // edit mode：update the previous one
                editingRecord.title = title;
                editingRecord.expiredDate = date;
                editingRecord.note = note;
                editingRecord.cid = cid;
                editingRecord.notifyDaysBefore = selectedNotifyDays;
                if (imagePath != null && !imagePath.isEmpty()) {
                    editingRecord.imagePath = imagePath;
                }

                repository.updateRecord(editingRecord);
                Toast.makeText(this,
                        "Updated: " + title + " (" + categoryName + ")",
                        Toast.LENGTH_SHORT).show();

            } else {
                // add mode：insert new data
                RecordEntity r = new RecordEntity();
                r.title = title;
                r.expiredDate = date;
                r.note = note;
                r.cid = cid;
                r.notifyDaysBefore = selectedNotifyDays;
                if (imagePath != null && !imagePath.isEmpty()) {
                    r.imagePath = imagePath;
                }

                repository.insertRecord(r);
                Toast.makeText(this,
                        "Saved: " + title + " (" + categoryName + ")",
                        Toast.LENGTH_SHORT).show();
            }

            // back to main page
            Intent back = new Intent(AddPage.this, MainActivity.class);
            startActivity(back);
            finish();
        });

        // if it's edit mode and get the id-> show the previous data
        if (isEditMode && editingRecordId != -1) {
            loadRecordForEdit(editingRecordId);
        }
    }

    private void loadRecordForEdit(int rid) {
        repository.getRecordById(rid).observe(this, record -> {
            if (record == null) return;
            editingRecord = record;

            // fill in UI blank
            editTitle.setText(record.title != null ? record.title : "");
            editDate.setText(record.expiredDate != null ? record.expiredDate : "");
            editNote.setText(record.note != null ? record.note : "");

            // picture
            if (record.imagePath != null && !record.imagePath.isEmpty()) {
                imagePath = record.imagePath;
                try {
                    imagePreview.setImageURI(Uri.parse(imagePath));
                } catch (Exception ignored) { }
            }

            // choose the right category by cid
            if (record.cid != 0) {
                for (int i = 0; i < categoryList.size(); i++) {
                    if (categoryList.get(i).cid == record.cid) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }

            // Notify days spinner
            int index = 3; // default= 14
            if (record.notifyDaysBefore > 0) {
                for (int i = 0; i < notifyOptions.length; i++) {
                    if (notifyOptions[i] == record.notifyDaysBefore) {
                        index = i;
                        break;
                    }
                }
            }
            spinnerNotifyDays.setSelection(index);
            selectedNotifyDays = notifyOptions[index];
        });
    }

    // 拍照結果回傳
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            // 使用者從相簿挑選一張圖片
            photoUri = data.getData();          // photo path
            if (photoUri != null) {
                imagePreview.setImageURI(photoUri);   // 圓圈裡顯示圖片
                imagePath = photoUri.toString();      // Uri 字串存進 DB
            }
        }
    }
}
