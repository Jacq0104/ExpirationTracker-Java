package com.example.expirationtracker_java;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirationtracker_java.data.entity.RecordEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private List<RecordEntity> records = new ArrayList<>();

    public void setRecords(List<RecordEntity> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        RecordEntity record = records.get(position);

        // 綁定文字
        holder.tvTitle.setText(record.title != null ? record.title : "(無標題)");
        holder.tvNote.setText(record.note != null ? record.note : "");
        holder.tvExpired.setText(record.expiredDate != null ? record.expiredDate : "");

        // 綁定圖片
        if (record.imagePath != null && !record.imagePath.isEmpty()) {
            File imgFile = new File(record.imagePath);
            if (imgFile.exists()) {
                holder.imgRecord.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
            } else {
                holder.imgRecord.setImageResource(R.drawable.ic_launcher_foreground); // 預設圖
            }
        } else {
            holder.imgRecord.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvNote, tvExpired;
        ImageView imgRecord;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvExpired = itemView.findViewById(R.id.tvExpired);
            imgRecord = itemView.findViewById(R.id.imgRecord);
        }
    }
}
