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

    public BatchFinanceAdapter(List<BatchFinanceModel> list, String currencySymbol) {
        this.list = list;
        this.currencySymbol = currencySymbol;
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
        holder.progressBar.setProgress(model.getProgress());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatchName, tvCollected, tvDue, tvStudentCount;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvBatchName = itemView.findViewById(R.id.tvBatchName);
            tvCollected = itemView.findViewById(R.id.tvCollectedAmount);
            tvDue = itemView.findViewById(R.id.tvDueAmount);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            progressBar = itemView.findViewById(R.id.progressBatchCollection);
        }
    }
}
