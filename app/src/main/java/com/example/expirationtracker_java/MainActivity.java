package com.example.expirationtracker_java;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 🔹 保留原本視窗邊距設定
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ------------------------------
        // 👇 從這裡開始是 Spinner 的部分
        // ------------------------------

        // 1️⃣ 找到 Spinner 元件 (要確定 activity_main.xml 裡有 <Spinner android:id="@+id/mySpinner"/> )
        Spinner spinner = findViewById(R.id.spinner);

        // 2️⃣ 建立靜態資料清單
        List<String> itemList = Arrays.asList("🍎 蘋果", "🍌 香蕉", "🍇 葡萄", "🍉 西瓜");

        // 3️⃣ 建立 ArrayAdapter 並指定自訂的 layout (spinner_item.xml)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,  // 自訂每個項目的顯示樣式
                itemList
        );

        // 4️⃣ 設定下拉選單展開時的樣式（可以改成 R.layout.spinner_item 也行）
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 5️⃣ 將 adapter 套用到 spinner
        spinner.setAdapter(adapter);
    }
}
