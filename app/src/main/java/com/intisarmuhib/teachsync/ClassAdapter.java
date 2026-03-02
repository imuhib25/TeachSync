package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private List<ClassModel> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEdit(ClassModel model);
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<ClassModel> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    public ClassModel getItem(int position) {
        return list.get(position);
    }

    public void removeItem(int position) {
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

        holder.tvTopic.setText(model.getTopic());
        holder.tvBatch.setText("Batch: " + model.getBatch());

        if (model.isExtra()) {
            holder.tvMonthlyNumber.setText("Extra Class");
            holder.tvExtra.setVisibility(View.VISIBLE);
        } else {
            holder.tvMonthlyNumber.setText("Class: " + model.getMonthlyNumber());
            holder.tvExtra.setVisibility(View.GONE);
        }

        holder.tvClassTime.setText("Time: " + model.getClassTime());

        // 🔥 ON GOING LOGIC
        long now = System.currentTimeMillis();

        if (model.getStartMillis() > 0 &&
                now >= model.getStartMillis() &&
                now <= model.getEndMillis()) {

            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvExtra.setText("ON GOING");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(model);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTopic, tvBatch, tvMonthlyNumber, tvExtra, tvClassTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvBatch = itemView.findViewById(R.id.tvBatch);
            tvMonthlyNumber = itemView.findViewById(R.id.tvMonthlyNumber);
            tvExtra = itemView.findViewById(R.id.tvExtra);
            tvClassTime = itemView.findViewById(R.id.tvClassTime);
        }
    }
}