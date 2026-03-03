package com.intisarmuhib.teachsync;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private List<ClassModel> list = new ArrayList<>();
    private OnItemClickListener listener;

    // Handler for periodic "On Going" refresh every 60 seconds
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView attachedRecyclerView;

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

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attachedRecyclerView = recyclerView;
        // Start periodic refresh so "On Going" badge updates in real time
        handler.postDelayed(refreshRunnable, 60_000);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        handler.removeCallbacks(refreshRunnable);
        attachedRecyclerView = null;
    }

    public void setData(List<ClassModel> newList) {
        this.list = new ArrayList<>(newList != null ? newList : new ArrayList<>());
        notifyDataSetChanged();
    }

    public ClassModel getItem(int position) {
        if (position < 0 || position >= list.size()) return null;
        return list.get(position);
    }

    public void removeItem(int position) {
        if (position < 0 || position >= list.size()) return;
        list.remove(position);
        notifyItemRemoved(position);
    }

    public void addItem(int position, ClassModel model) {
        if (position < 0 || position > list.size()) return;
        list.add(position, model);
        notifyItemInserted(position);
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

        String batch = model.getBatch() != null ? model.getBatch() : "";
        holder.tvBatch.setText("Batch: " + batch);

        String classTime = model.getClassTime() != null ? model.getClassTime() : "";
        holder.tvClassTime.setText(classTime);

        // ── Extra class: show "Extra" label, hide number ──────────────────
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

            // ── Remaining classes chip ──────────────────────────────────────
            try {
                int taken = Integer.parseInt(num);
                int remaining = total - taken;
                if (remaining > 0) {
                    holder.tvRemaining.setText(remaining + " remaining");
                    holder.tvRemaining.setVisibility(View.VISIBLE);
                    holder.tvRemaining.setBackgroundColor(0x993949AB);
                } else if (remaining == 0) {
                    holder.tvRemaining.setText("Cycle complete!");
                    holder.tvRemaining.setVisibility(View.VISIBLE);
                    holder.tvRemaining.setBackgroundColor(0x99388E3C);
                } else {
                    holder.tvRemaining.setVisibility(View.GONE);
                }
            } catch (NumberFormatException e) {
                holder.tvRemaining.setVisibility(View.GONE);
            }
        }

        // ── On Going badge ────────────────────────────────────────────────
        if (isOnGoing(model)) {
            holder.tvOnGoing.setVisibility(View.VISIBLE);
        } else {
            holder.tvOnGoing.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(model);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ── Determine if a class is currently on-going ────────────────────────
    // classTime format: "10:00 AM - 11:30 AM"
    private boolean isOnGoing(ClassModel model) {
        if (model.getClassTime() == null || model.getClassTime().isEmpty()) return false;
        if (model.getDate() == null || model.getDate().isEmpty()) return false;

        try {
            String[] parts = model.getClassTime().split(" - ");
            if (parts.length != 2) return false;

            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            Date classDate = dateFmt.parse(model.getDate());
            if (classDate == null) return false;

            // Check today matches class date
            Calendar today = Calendar.getInstance();
            Calendar classDay = Calendar.getInstance();
            classDay.setTime(classDate);

            if (today.get(Calendar.YEAR) != classDay.get(Calendar.YEAR) ||
                today.get(Calendar.DAY_OF_YEAR) != classDay.get(Calendar.DAY_OF_YEAR)) {
                return false;
            }

            Date startTime = timeFmt.parse(parts[0].trim());
            Date endTime = timeFmt.parse(parts[1].trim());
            if (startTime == null || endTime == null) return false;

            // Apply today's date to parsed times
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startTime);
            startCal.set(Calendar.YEAR, today.get(Calendar.YEAR));
            startCal.set(Calendar.MONTH, today.get(Calendar.MONTH));
            startCal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endTime);
            endCal.set(Calendar.YEAR, today.get(Calendar.YEAR));
            endCal.set(Calendar.MONTH, today.get(Calendar.MONTH));
            endCal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));

            long now = System.currentTimeMillis();
            return now >= startCal.getTimeInMillis() && now <= endCal.getTimeInMillis();

        } catch (Exception e) {
            return false;
        }
    }

    // ── Attach red swipe background + delete icon to a RecyclerView ───────
    public static void attachSwipeToDelete(RecyclerView recyclerView, OnSwipeListener swipeListener) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                   @NonNull RecyclerView.ViewHolder vh,
                                   @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                swipeListener.onSwiped(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                     @NonNull RecyclerView rv,
                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                     float dX, float dY, int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;

                // Red background
                ColorDrawable background = new ColorDrawable(Color.parseColor("#C62828"));
                if (dX < 0) {
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                } else {
                    background.setBounds(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + (int) dX, itemView.getBottom());
                }
                background.draw(c);

                // Delete icon
                Drawable icon = ContextCompat.getDrawable(rv.getContext(), R.drawable.ic_delete);
                if (icon != null) {
                    icon.setTint(Color.WHITE);
                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconTop    = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();

                    if (dX < 0) {
                        int iconLeft  = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    } else {
                        int iconLeft  = itemView.getLeft() + iconMargin;
                        int iconRight = iconLeft + icon.getIntrinsicWidth();
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }
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
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTopic, tvBatch, tvMonthlyNumber, tvExtra, tvClassTime,
                 tvOnGoing, tvRemaining;

        ViewHolder(View itemView) {
            super(itemView);
            tvTopic         = itemView.findViewById(R.id.tvTopic);
            tvBatch         = itemView.findViewById(R.id.tvBatch);
            tvMonthlyNumber = itemView.findViewById(R.id.tvMonthlyNumber);
            tvExtra         = itemView.findViewById(R.id.tvExtra);
            tvClassTime     = itemView.findViewById(R.id.tvClassTime);
            tvOnGoing       = itemView.findViewById(R.id.tvOnGoing);
            tvRemaining     = itemView.findViewById(R.id.tvRemaining);
        }
    }
}
