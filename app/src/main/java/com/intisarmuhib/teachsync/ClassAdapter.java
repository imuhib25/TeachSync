package com.intisarmuhib.teachsync;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private List<ClassModel> list = new ArrayList<>();
    private OnStatusUpdateListener statusListener;
    private OnItemClickListener editListener;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
            handler.postDelayed(this, 60_000);
        }
    };

    public interface OnItemClickListener {
        void onEdit(ClassModel model);
    }

    public interface OnStatusUpdateListener {
        void onStatusUpdate(ClassModel model, String oldStatus, String newStatus);
    }

    public void setListener(OnItemClickListener listener) {
        this.editListener = listener;
    }

    public void setStatusUpdateListener(OnStatusUpdateListener listener) {
        this.statusListener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        handler.postDelayed(refreshRunnable, 60_000);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        handler.removeCallbacks(refreshRunnable);
    }

    public void setData(List<ClassModel> newList) {
        this.list = new ArrayList<>(newList != null ? newList : new ArrayList<>());
        notifyDataSetChanged();
    }

    public ClassModel getItem(int position) {
        if (position < 0 || position >= list.size()) return null;
        return list.get(position);
    }

    public void addItem(int position, ClassModel model) {
        if (position < 0 || position > list.size()) return;
        list.add(position, model);
        notifyItemInserted(position);
    }

    public void removeItem(int position) {
        if (position < 0 || position >= list.size()) return;
        list.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassModel model = list.get(position);

        holder.tvTopic.setText(model.getTopic() != null ? model.getTopic() : "");
        holder.tvBatch.setText("Batch: " + (model.getBatch() != null ? model.getBatch() : ""));
        holder.tvClassTime.setText(model.getClassTime() != null ? model.getClassTime() : "");

        // ── Monthly Number & Progress ───────────────────────────────────
        if (model.isExtra()) {
            holder.tvMonthlyNumber.setVisibility(View.GONE);
            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvRemaining.setVisibility(View.GONE);
        } else {
            holder.tvExtra.setVisibility(View.GONE);
            holder.tvMonthlyNumber.setVisibility(View.VISIBLE);
            String num = model.getMonthlyNumber() != null ? model.getMonthlyNumber() : "";
            int total = model.getTotalInCycle();
            holder.tvMonthlyNumber.setText("Class " + num + " of " + total);

            try {
                int taken = Integer.parseInt(num);
                int remaining = total - taken;
                if (remaining > 0) {
                    holder.tvRemaining.setText(remaining + " remaining");
                    holder.tvRemaining.setVisibility(View.VISIBLE);
                } else if (remaining == 0) {
                    holder.tvRemaining.setText("Cycle complete!");
                    holder.tvRemaining.setVisibility(View.VISIBLE);
                } else {
                    holder.tvRemaining.setVisibility(View.GONE);
                }
            } catch (NumberFormatException e) {
                holder.tvRemaining.setVisibility(View.GONE);
            }
        }

        // ── Status & Overlays ───────────────────────────────────────────
        String status = model.getStatus() != null ? model.getStatus() : "scheduled";
        boolean ongoing = isOnGoing(model);
        boolean timeOver = isTimeOver(model);

        // Reset visibilities
        holder.tvOnGoing.setVisibility(View.GONE);
        holder.tvStatusBadge.setVisibility(View.GONE);
        holder.layoutConfirmationOverlay.setVisibility(View.GONE);
        holder.layoutCompletedOverlay.setVisibility(View.GONE);

        if (status.equals("scheduled")) {
            if (ongoing) {
                holder.tvOnGoing.setVisibility(View.VISIBLE);
            } else if (timeOver) {
                holder.layoutConfirmationOverlay.setVisibility(View.VISIBLE);
            }
        } else {
            // "completed", "postponed", "rescheduled"
            holder.layoutCompletedOverlay.setVisibility(View.VISIBLE);
            holder.tvOverlayStatus.setText(status.toUpperCase());
            
            int color = Color.GRAY;
            if (status.equals("completed")) color = Color.parseColor("#4CAF50");
            else if (status.equals("postponed")) color = Color.parseColor("#F44336");
            else if (status.equals("rescheduled")) color = Color.parseColor("#FF9800");
            
            holder.tvOverlayStatus.setTextColor(color);
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(status);
            holder.tvStatusBadge.setBackgroundColor(color);
        }

        // ── Button Listeners ────────────────────────────────────────────
        holder.btnMarkCompleted.setOnClickListener(v -> updateStatus(model, "completed"));
        holder.btnMarkPostponed.setOnClickListener(v -> updateStatus(model, "postponed"));
        holder.btnMarkRescheduled.setOnClickListener(v -> updateStatus(model, "rescheduled"));

        holder.itemView.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(model);
        });
    }

    private void updateStatus(ClassModel model, String newStatus) {
        String oldStatus = model.getStatus();
        if (newStatus.equals(oldStatus)) return;

        model.setStatus(newStatus);
        notifyDataSetChanged();
        
        // Update in Firestore
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .collection("classes").document(model.getId())
                    .update("status", newStatus);
        }
        
        if (statusListener != null) {
            statusListener.onStatusUpdate(model, oldStatus, newStatus);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private boolean isOnGoing(ClassModel model) {
        return checkTime(model, true);
    }

    private boolean isTimeOver(ClassModel model) {
        return checkTime(model, false);
    }

    private boolean checkTime(ClassModel model, boolean checkOngoing) {
        if (model.getClassTime() == null || model.getDate() == null) return false;
        try {
            String[] parts = model.getClassTime().split(" - ");
            if (parts.length != 2) return false;

            SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            Date startTime = fullFmt.parse(model.getDate() + " " + parts[0].trim());
            Date endTime = fullFmt.parse(model.getDate() + " " + parts[1].trim());
            
            long now = System.currentTimeMillis();
            if (checkOngoing) {
                return now >= startTime.getTime() && now <= endTime.getTime();
            } else {
                return now > endTime.getTime();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static void attachSwipeToDelete(RecyclerView recyclerView, OnSwipeListener swipeListener) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                swipeListener.onSwiped(vh.getAdapterPosition());
            }
        }).attachToRecyclerView(recyclerView);
    }

    public interface OnSwipeListener {
        void onSwiped(int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvBatch, tvMonthlyNumber, tvExtra, tvClassTime, tvOnGoing, tvRemaining, tvStatusBadge, tvOverlayStatus;
        LinearLayout layoutCompletedOverlay, layoutConfirmationOverlay;
        Button btnMarkCompleted, btnMarkPostponed, btnMarkRescheduled;

        ViewHolder(View itemView) {
            super(itemView);
            tvTopic         = itemView.findViewById(R.id.tvTopic);
            tvBatch         = itemView.findViewById(R.id.tvBatch);
            tvMonthlyNumber = itemView.findViewById(R.id.tvMonthlyNumber);
            tvExtra         = itemView.findViewById(R.id.tvExtra);
            tvClassTime     = itemView.findViewById(R.id.tvClassTime);
            tvOnGoing       = itemView.findViewById(R.id.tvOnGoing);
            tvStatusBadge   = itemView.findViewById(R.id.tvStatusBadge);
            tvRemaining     = itemView.findViewById(R.id.tvRemaining);
            tvOverlayStatus = itemView.findViewById(R.id.tvOverlayStatus);
            layoutCompletedOverlay = itemView.findViewById(R.id.layoutCompletedOverlay);
            layoutConfirmationOverlay = itemView.findViewById(R.id.layoutConfirmationOverlay);
            btnMarkCompleted = itemView.findViewById(R.id.btnMarkCompleted);
            btnMarkPostponed = itemView.findViewById(R.id.btnMarkPostponed);
            btnMarkRescheduled = itemView.findViewById(R.id.btnMarkRescheduled);
        }
    }
}
