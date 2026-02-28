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

    public void setData(List<ClassModel> newList) {
        list = newList;
        notifyDataSetChanged();
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
        holder.tvMonthlyNumber.setText(model.getMonthlyNumber());

        if (model.isExtra()) {
            holder.tvExtra.setVisibility(View.VISIBLE);
        } else {
            holder.tvExtra.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTopic, tvBatch, tvMonthlyNumber, tvExtra;

        ViewHolder(View itemView) {
            super(itemView);

            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvBatch = itemView.findViewById(R.id.tvBatch);
            tvMonthlyNumber = itemView.findViewById(R.id.tvMonthlyNumber);
            tvExtra = itemView.findViewById(R.id.tvExtra);
        }
    }
}