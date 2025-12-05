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

import androidx.recyclerview.widget.DiffUtil;
import com.example.expirationtracker_java.data.entity.RecordEntity;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private List<RecordEntity> records = new ArrayList<>();
    private java.util.Set<Integer> expandedPositions = new java.util.HashSet<>();

    /*public interface onDeleteClickListener {
        void onDeleteClick(RecordEntity record);
    }

    private onDeleteClickListener deleteListener;

    public void setOnDeleteClickListener(onDeleteClickListener listener) {
        this.deleteListener = listener;
    }*/
    public void setRecords(List<RecordEntity> newRecords) {
        this.records = newRecords;
        expandedPositions.clear();
        notifyDataSetChanged();
    }

    //get the item id from record (if id exist, then edit; if not, then use the blank add page)
    //Let MainActivity can get the record by position
    public RecordEntity getRecordAt(int position) {
        if (records == null || position < 0 || position >= records.size()) return null;
        return records.get(position);
    }

    /*public void setRecords(List<RecordEntity> newRecords) {//animation
        // 1. 先複製舊資料
        List<RecordEntity> oldList = new ArrayList<>(this.records);

        // 2. 計算差異
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new RecordDiffCallback(oldList, newRecords));

        // 3. 更新資料內容
        this.records.clear();
        this.records.addAll(newRecords);
        expandedPositions.clear(); // 你如果想保留展開狀態，可以自己決定要不要清

        // 4. 把變化套用到 RecyclerView → 自動有動畫 ✨
        diffResult.dispatchUpdatesTo(this);
    }*/


    private final java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

        // === 這裡開始：根據到期日設定顏色 ===
        if (record.expiredDate != null && !record.expiredDate.isEmpty()) {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate expiry =
                        java.time.LocalDate.parse(record.expiredDate, formatter);

                long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);
                // 如果這筆沒有設定（預設 0），就當作 14
                long soonDays = (record.notifyDaysBefore > 0) ? record.notifyDaysBefore : 14;

                int color;
//                long soonDays = 14;
                if (daysDiff < 0) {
                    color = android.graphics.Color.parseColor("#D32F2F"); // 深紅
                } else if (daysDiff >= 0 && daysDiff <= soonDays) {
                    color = android.graphics.Color.parseColor("#FF9800"); // 橘色
                } else {
                    color = android.graphics.Color.parseColor("#4CAF50"); // 綠色
                }
                holder.colorBar.setBackgroundColor(color);

            } catch (Exception e) {
                // 日期格式錯誤就給一個預設顏色，避免閃退
                holder.colorBar.setBackgroundColor(
                        android.graphics.Color.parseColor("#BDBDBD")  // 灰色
                );
            }
        } else {
            // 沒有到期日 → 統一給灰色或你喜歡的顏色
            holder.colorBar.setBackgroundColor(
                    android.graphics.Color.parseColor("#BDBDBD")
            );
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

        /*holder.deleteBtn.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(record);
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvNote, tvExpired;
        ImageView imgRecord;
//        ImageButton deleteBtn;
        View colorBar;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvExpired = itemView.findViewById(R.id.tvExpired);
            imgRecord = itemView.findViewById(R.id.imgRecord);
//            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            colorBar = itemView.findViewById(R.id.colorBar);
        }
    }

    public class RecordDiffCallback extends DiffUtil.Callback {//animation

        private final List<RecordEntity> oldList;
        private final List<RecordEntity> newList;

        public RecordDiffCallback(List<RecordEntity> oldList, List<RecordEntity> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        // 判斷是不是「同一筆」資料（通常用 id）
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            RecordEntity oldItem = oldList.get(oldItemPosition);
            RecordEntity newItem = newList.get(newItemPosition);
            return oldItem.rid == newItem.rid;   // 主鍵欄位
        }

        // 判斷內容有沒有改變（title/note/date 等）
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            RecordEntity oldItem = oldList.get(oldItemPosition);
            RecordEntity newItem = newList.get(newItemPosition);

            return
                    equals(oldItem.title, newItem.title)
                            && equals(oldItem.note, newItem.note)
                            && equals(oldItem.expiredDate, newItem.expiredDate)
                            && equals(oldItem.imagePath, newItem.imagePath)
                            && oldItem.cid == newItem.cid;
        }

        private boolean equals(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    }
}