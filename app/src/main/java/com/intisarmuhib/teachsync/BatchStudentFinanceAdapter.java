package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BatchStudentFinanceAdapter extends RecyclerView.Adapter<BatchStudentFinanceAdapter.ViewHolder> {

    private final List<InvoiceModel> list;

    public BatchStudentFinanceAdapter(List<InvoiceModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch_student_finance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvoiceModel model = list.get(position);

        holder.tvStudentName.setText(model.getStudentName());
        holder.tvPaid.setText("Paid: " + (int)model.getPaidAmount());
        holder.tvDue.setText("Due: " + (int)model.getDueAmount());
        holder.tvStatus.setText(model.getStatus());

        if ("Paid".equals(model.getStatus())) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else if ("Overdue".equals(model.getStatus())) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvPaid, tvDue, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvPaid = itemView.findViewById(R.id.tvPaidAmount);
            tvDue = itemView.findViewById(R.id.tvDueAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
