package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ActiveBatchAdapter extends RecyclerView.Adapter<ActiveBatchAdapter.ViewHolder> {

    private List<BatchModel> batchList;
    private OnBatchActionListener actionListener;

    public interface OnBatchActionListener {
        void onStartNewCycle(BatchModel batch);
        void onCloseBatch(BatchModel batch);
    }

    public ActiveBatchAdapter(List<BatchModel> batchList) {
        this.batchList = batchList;
    }

    public void setOnBatchActionListener(OnBatchActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BatchModel batch = batchList.get(position);
        holder.tvName.setText(batch.getName());
        holder.tvSubject.setText(batch.getSubject());
        holder.tvStudents.setText(batch.getEnrolledCount() + " Students");
        holder.tvProgress.setText("Taken: " + batch.getCurrentMonthCount() + "/" + batch.getTotalMonthlyClasses());
        holder.tvCycleCount.setText("C: " + batch.getCycleCount());

        if (batch.getStartTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(batch.getStartTime().toDate()));
        }

        // Overlay logic
        if (batch.getTotalMonthlyClasses() > 0 && batch.getCurrentMonthCount() >= batch.getTotalMonthlyClasses() && !batch.isArchived()) {
            holder.layoutOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.layoutOverlay.setVisibility(View.GONE);
        }

        holder.btnStartNewCycle.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onStartNewCycle(batch);
        });

        holder.btnCloseBatch.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onCloseBatch(batch);
        });
    }

    @Override
    public int getItemCount() {
        return batchList.size();
    }

    public void updateList(List<BatchModel> newList) {
        this.batchList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSubject, tvStudents, tvTime, tvProgress, tvCycleCount;
        LinearLayout layoutOverlay;
        MaterialButton btnStartNewCycle, btnCloseBatch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvBatchName);
            tvSubject = itemView.findViewById(R.id.tvBatchSubject);
            tvStudents = itemView.findViewById(R.id.tvStudentCount);
            tvTime = itemView.findViewById(R.id.tvBatchTime);
            tvProgress = itemView.findViewById(R.id.tvClassProgress);
            tvCycleCount = itemView.findViewById(R.id.tvCycleCount);
            layoutOverlay = itemView.findViewById(R.id.layoutBatchOverlay);
            btnStartNewCycle = itemView.findViewById(R.id.btnStartNewCycle);
            btnCloseBatch = itemView.findViewById(R.id.btnCloseBatch);
        }
    }
}
