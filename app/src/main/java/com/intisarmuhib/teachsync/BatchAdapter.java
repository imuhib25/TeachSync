package com.intisarmuhib.teachsync;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.BatchViewHolder> {

    private final Context context;
    private final List<BatchModel> displayList;
    private final List<BatchModel> fullList;
    private final OnBatchClickListener listener;

    public interface OnBatchClickListener {
        void onBatchClick(BatchModel batch);
        void onInfoClick(BatchModel batch);
    }

    public BatchAdapter(Context context,
                        List<BatchModel> batchList,
                        OnBatchClickListener listener) {
        this.context = context;
        this.displayList = new ArrayList<>(batchList);
        this.fullList = new ArrayList<>(batchList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.batch_item, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        BatchModel batch = displayList.get(position);

        holder.tvName.setText(batch.getName() != null ? batch.getName() : "");

        String subject = batch.getSubject() != null ? batch.getSubject() : "";
        holder.tvSubject.setText("Subject: " + subject);

        holder.tvEnrolledCount.setText(batch.getEnrolledCount() + " Students");

        holder.tvTime.setText(formatTime(batch.getStartTime())
                + " – " + formatTime(batch.getEndTime()));

        holder.tvDuration.setText("Duration: " + formatDuration(batch.getDurationMinutes()));
        
        holder.tvPayment.setText("৳ " + (int)batch.getPaymentPerStudent());

        // ── Taken / Remaining / Total ─────────────────────────────────────
        int total   = batch.getTotalMonthlyClasses();
        int taken   = batch.getCurrentMonthCount();
        int remaining = Math.max(0, total - taken);

        holder.tvTaken.setText(String.valueOf(taken));
        holder.tvRemaining.setText(String.valueOf(remaining));
        holder.tvTotal.setText(String.valueOf(total));

        // ── Progress bar ──────────────────────────────────────────────────
        int progress = (total > 0) ? (int) ((taken / (float) total) * 100) : 0;
        holder.progressCycle.setProgress(progress);

        // Color remaining green if some left, orange if 1 left, red if 0
        if (remaining == 0) {
            holder.tvRemaining.setTextColor(Color.parseColor("#FF5252")); // red = complete
        } else if (remaining == 1) {
            holder.tvRemaining.setTextColor(Color.parseColor("#FFB300")); // amber = last one
        } else {
            holder.tvRemaining.setTextColor(Color.parseColor("#4CAF50")); // green = ok
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBatchClick(batch);
        });

        holder.tvBtnDetails.setOnClickListener(v -> {
            if (listener != null) listener.onInfoClick(batch);
        });
    }

    @Override
    public int getItemCount() { return displayList.size(); }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "--";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }

    private String formatDuration(long minutes) {
        if (minutes <= 0) return "0m";
        long h = minutes / 60, m = minutes % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    public void setBatches(List<BatchModel> batches) {
        fullList.clear();
        fullList.addAll(batches);
        displayList.clear();
        displayList.addAll(batches);
        notifyDataSetChanged();
    }

    public BatchModel getItem(int position) {
        if (position >= 0 && position < displayList.size()) {
            return displayList.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < displayList.size()) {
            BatchModel removed = displayList.remove(position);
            fullList.remove(removed);
            notifyItemRemoved(position);
        }
    }

    public void restoreItem(BatchModel item, int position) {
        if (position >= 0 && position <= displayList.size()) {
            displayList.add(position, item);
            fullList.add(item);
            notifyItemInserted(position);
        }
    }

    public void filter(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String q = query.toLowerCase(Locale.getDefault()).trim();
            for (BatchModel b : fullList) {
                boolean nameMatch    = b.getName() != null && b.getName().toLowerCase(Locale.getDefault()).contains(q);
                boolean subjectMatch = b.getSubject() != null && b.getSubject().toLowerCase(Locale.getDefault()).contains(q);
                boolean timeMatch    = formatTime(b.getStartTime()).toLowerCase(Locale.getDefault()).contains(q)
                                    || formatTime(b.getEndTime()).toLowerCase(Locale.getDefault()).contains(q);
                if (nameMatch || subjectMatch || timeMatch) displayList.add(b);
            }
        }
        notifyDataSetChanged();
    }

    // ── Attach red swipe-to-delete to a RecyclerView ──────────────────────
    public static void attachSwipeToDelete(RecyclerView recyclerView, OnSwipeListener swipeListener) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                   @NonNull RecyclerView.ViewHolder vh,
                                   @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                swipeListener.onSwiped(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                     float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                ColorDrawable bg = new ColorDrawable(Color.parseColor("#C62828"));
                bg.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());
                bg.draw(c);

                Drawable icon = ContextCompat.getDrawable(rv.getContext(), R.drawable.ic_delete);
                if (icon != null) {
                    icon.setTint(Color.WHITE);
                    int margin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int top    = itemView.getTop() + margin;
                    int left   = itemView.getRight() - margin - icon.getIntrinsicWidth();
                    icon.setBounds(left, top, left + icon.getIntrinsicWidth(), top + icon.getIntrinsicHeight());
                    icon.draw(c);
                }
                super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recyclerView);
    }

    public interface OnSwipeListener {
        void onSwiped(int position);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────
    public static class BatchViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvSubject, tvTime, tvDuration, tvPayment, tvEnrolledCount, tvBtnDetails;
        TextView tvTaken, tvRemaining, tvTotal;
        ProgressBar progressCycle;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tvBatchName);
            tvSubject     = itemView.findViewById(R.id.tvBatchSubject);
            tvEnrolledCount = itemView.findViewById(R.id.tvEnrolledCount);
            tvTime        = itemView.findViewById(R.id.tvBatchTime);
            tvDuration    = itemView.findViewById(R.id.tvBatchDuration);
            tvPayment     = itemView.findViewById(R.id.tvPayment);
            tvTaken       = itemView.findViewById(R.id.tvClassesTaken);
            tvRemaining   = itemView.findViewById(R.id.tvClassesRemaining);
            tvTotal       = itemView.findViewById(R.id.tvClassesTotal);
            tvBtnDetails  = itemView.findViewById(R.id.tvBtnDetails);
            progressCycle = itemView.findViewById(R.id.progressCycle);
        }
    }
}
