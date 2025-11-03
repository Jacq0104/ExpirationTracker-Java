package com.example.expirationtracker_java;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.expirationtracker_java.data.entity.CategoryEntity;

import java.util.List;

public class CategorySpinnerAdapter extends ArrayAdapter<CategoryEntity> {

    private final Context context;
    private final List<CategoryEntity> categories;

    public CategorySpinnerAdapter(@NonNull Context context, @NonNull List<CategoryEntity> categories) {
        super(context, 0, categories);
        this.context = context;
        this.categories = categories;
    }

    // Spinner 關閉狀態（顯示選中的項目）
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    // Spinner 展開下拉清單時
    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);
        }

        CategoryEntity category = categories.get(position);
        TextView textView = convertView.findViewById(R.id.textView);
        textView.setText(category.cname); // 這裡是你的欄位名稱
        return convertView;
    }
}
