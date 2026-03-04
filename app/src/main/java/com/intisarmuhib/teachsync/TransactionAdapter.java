package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<TransactionModel> list;
    private final String currencySymbol;
    private OnTransactionListener listener;

    public interface OnTransactionListener {
        void onDelete(TransactionModel transaction, int position);
        void onEdit(TransactionModel transaction, int position);
        void onGenerateInvoice(TransactionModel transaction);
    }

    public TransactionAdapter(List<TransactionModel> list, String currencySymbol, OnTransactionListener listener) {
        this.list = list;
        this.currencySymbol = currencySymbol;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionModel tx = list.get(position);

        holder.tvStudent.setText(tx.getStudentName());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateStr = tx.getTimestamp() != null ? sdf.format(tx.getTimestamp().toDate()) : "N/A";
        holder.tvDate.setText(dateStr + " • " + tx.getMethod());
        
        holder.tvAmount.setText("+ " + currencySymbol + " " + (int)tx.getAmount());

        // Set icon based on method
        if (tx.getMethod() != null && tx.getMethod().equalsIgnoreCase("Cash")) {
            holder.ivMethod.setImageResource(R.drawable.baseline_payments_24);
        } else {
            holder.ivMethod.setImageResource(R.drawable.outline_notifications_24); // Placeholder
        }

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(tx, position);
        });

        holder.ivInvoice.setOnClickListener(v -> {
            if (listener != null) listener.onGenerateInvoice(tx);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(tx, position);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudent, tvDate, tvAmount;
        ImageView ivMethod, ivDelete, ivInvoice;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudent = itemView.findViewById(R.id.tvTransactionStudent);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            ivMethod = itemView.findViewById(R.id.ivPaymentMethod);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            ivInvoice = itemView.findViewById(R.id.ivInvoice);
        }
    }
}
