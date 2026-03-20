package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BatchFinanceAdapter extends RecyclerView.Adapter<BatchFinanceAdapter.ViewHolder> {

    private final List<BatchFinanceModel> list;
    private final String currencySymbol;
    private final OnBatchClickListener listener;

    public interface OnBatchClickListener {
        void onBatchClick(BatchFinanceModel model);
    }

    public BatchFinanceAdapter(List<BatchFinanceModel> list, String currencySymbol, OnBatchClickListener listener) {
        this.list = list;
        this.currencySymbol = currencySymbol;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch_finance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BatchFinanceModel model = list.get(position);

        holder.tvBatchName.setText(model.getBatchName());
        holder.tvCollected.setText(currencySymbol + " " + (int)model.getCollectedAmount());
        holder.tvDue.setText("Due: " + currencySymbol + (int)model.getDueAmount());
        holder.tvStudentCount.setText(model.getStudentCount() + " Students");
        holder.tvCycleCount.setText("Cycle-" + model.getCycleCount());
        holder.progressBar.setProgress(model.getProgress());

        if (model.getDueAmount() <= 0 && model.getCollectedAmount() > 0) {
            holder.layoutCompletedOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.layoutCompletedOverlay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBatchClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatchName, tvCollected, tvDue, tvStudentCount, tvCycleCount;
        ProgressBar progressBar;
        View layoutCompletedOverlay;

        ViewHolder(View itemView) {
            super(itemView);
            tvBatchName = itemView.findViewById(R.id.tvBatchName);
            tvCollected = itemView.findViewById(R.id.tvCollectedAmount);
            tvDue = itemView.findViewById(R.id.tvDueAmount);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            tvCycleCount = itemView.findViewById(R.id.tvCycleCount);
            progressBar = itemView.findViewById(R.id.progressBatchCollection);
            layoutCompletedOverlay = itemView.findViewById(R.id.layoutCompletedOverlay);
        }
    }
}
