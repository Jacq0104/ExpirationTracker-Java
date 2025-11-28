package com.example.expirationtracker_java;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private java.util.Set<Integer> expandedPositions = new java.util.HashSet<>();

    public interface onDeleteClickListener {
        void onDeleteClick(RecordEntity record);
    }

    private onDeleteClickListener deleteListener;

    public void setOnDeleteClickListener(onDeleteClickListener listener) {
        this.deleteListener = listener;
    }
    public void setRecords(List<RecordEntity> newRecords) {
        this.records = newRecords;
        expandedPositions.clear();   // 重刷資料時先清空展開狀態
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

        // 是否展開note
        boolean isExpanded = expandedPositions.contains(position);
        if (isExpanded) {
            holder.tvNote.setMaxLines(Integer.MAX_VALUE);
            holder.tvNote.setEllipsize(null);
        } else {
            holder.tvNote.setMaxLines(2);
            holder.tvNote.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }

        // 點一下note: 展開or收起
        holder.itemView.setOnClickListener(v -> {
            if (expandedPositions.contains(holder.getAdapterPosition())) {
                expandedPositions.remove(holder.getAdapterPosition());
            } else {
                expandedPositions.add(holder.getAdapterPosition());
            }
            notifyItemChanged(holder.getAdapterPosition());
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvNote, tvExpired;
        ImageView imgRecord;
        ImageButton deleteBtn;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvExpired = itemView.findViewById(R.id.tvExpired);
            imgRecord = itemView.findViewById(R.id.imgRecord);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}