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

    // Refreshes "On Going" badge every minute
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
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

        if (model.isExtra()) {
            // Extra class: show tag, hide number and remaining chip
            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvMonthlyNumber.setVisibility(View.GONE);
            holder.tvRemaining.setVisibility(View.GONE);
        } else {
            holder.tvExtra.setVisibility(View.GONE);
            holder.tvMonthlyNumber.setVisibility(View.VISIBLE);

            String num   = model.getMonthlyNumber();
            int total    = model.getTotalInCycle();
            holder.tvMonthlyNumber.setText("Class " + num + " of " + total);

            // Remaining classes chip
            try {
                int taken     = Integer.parseInt(num);
                int remaining = total - taken;
                if (remaining > 0) {
                    holder.tvRemaining.setText(remaining + " remaining");
                    holder.tvRemaining.setBackgroundColor(0x993949AB);
                    holder.tvRemaining.setVisibility(View.VISIBLE);
                } else if (remaining == 0) {
                    holder.tvRemaining.setText("Cycle complete!");
                    holder.tvRemaining.setBackgroundColor(0x99388E3C);
                    holder.tvRemaining.setVisibility(View.VISIBLE);
                } else {
                    holder.tvRemaining.setVisibility(View.GONE);
                }
            } catch (NumberFormatException e) {
                holder.tvRemaining.setVisibility(View.GONE);
            }
        }

        // On Going badge
        holder.tvOnGoing.setVisibility(isOnGoing(model) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(model);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    // Returns true if current time is within this class's time window today
    private boolean isOnGoing(ClassModel model) {
        if (model.getClassTime() == null || model.getClassTime().isEmpty()) return false;
        if (model.getDate() == null || model.getDate().isEmpty()) return false;
        try {
            String[] parts = model.getClassTime().split(" - ");
            if (parts.length != 2) return false;

            SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            Date classDate = dateFmt.parse(model.getDate());
            if (classDate == null) return false;

            Calendar today    = Calendar.getInstance();
            Calendar classDay = Calendar.getInstance();
            classDay.setTime(classDate);

            if (today.get(Calendar.YEAR) != classDay.get(Calendar.YEAR) ||
                    today.get(Calendar.DAY_OF_YEAR) != classDay.get(Calendar.DAY_OF_YEAR))
                return false;

            Date startTime = timeFmt.parse(parts[0].trim());
            Date endTime   = timeFmt.parse(parts[1].trim());
            if (startTime == null || endTime == null) return false;

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startTime);
            startCal.set(Calendar.YEAR,         today.get(Calendar.YEAR));
            startCal.set(Calendar.MONTH,        today.get(Calendar.MONTH));
            startCal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endTime);
            endCal.set(Calendar.YEAR,         today.get(Calendar.YEAR));
            endCal.set(Calendar.MONTH,        today.get(Calendar.MONTH));
            endCal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));

            long now = System.currentTimeMillis();
            return now >= startCal.getTimeInMillis() && now <= endCal.getTimeInMillis();
        } catch (Exception e) { return false; }
    }

    // Attaches red background + delete icon swipe to any RecyclerView
    public static void attachSwipeToDelete(RecyclerView rv, OnSwipeListener swipeListener) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView r,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                swipeListener.onSwiped(vh.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView r,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int state, boolean active) {
                View item = vh.itemView;
                ColorDrawable bg = new ColorDrawable(Color.parseColor("#C62828"));
                if (dX < 0)
                    bg.setBounds(item.getRight() + (int)dX, item.getTop(), item.getRight(), item.getBottom());
                else
                    bg.setBounds(item.getLeft(), item.getTop(), item.getLeft() + (int)dX, item.getBottom());
                bg.draw(c);

                Drawable icon = ContextCompat.getDrawable(r.getContext(), R.drawable.ic_delete);
                if (icon != null) {
                    icon.setTint(Color.WHITE);
                    int margin = (item.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int top    = item.getTop() + margin;
                    int bottom = top + icon.getIntrinsicHeight();
                    if (dX < 0) {
                        int right = item.getRight() - margin;
                        icon.setBounds(right - icon.getIntrinsicWidth(), top, right, bottom);
                    } else {
                        int left = item.getLeft() + margin;
                        icon.setBounds(left, top, left + icon.getIntrinsicWidth(), bottom);
                    }
                    icon.draw(c);
                }
                super.onChildDraw(c, r, vh, dX, dY, state, active);
            }
        }).attachToRecyclerView(rv);
    }

    public interface OnSwipeListener {
        void onSwiped(int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvBatch, tvMonthlyNumber, tvExtra,
                tvClassTime, tvOnGoing, tvRemaining;

        ViewHolder(View v) {
            super(v);
            tvTopic         = v.findViewById(R.id.tvTopic);
            tvBatch         = v.findViewById(R.id.tvBatch);
            tvMonthlyNumber = v.findViewById(R.id.tvMonthlyNumber);
            tvExtra         = v.findViewById(R.id.tvExtra);
            tvClassTime     = v.findViewById(R.id.tvClassTime);
            tvOnGoing       = v.findViewById(R.id.tvOnGoing);
            tvRemaining     = v.findViewById(R.id.tvRemaining);
        }
    }
}