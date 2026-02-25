package com.intisarmuhib.teachsync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.BatchViewHolder> {

    private Context context;
    private List<BatchModel> displayList;
    private List<BatchModel> fullList;

    public interface OnBatchClickListener {
        void onBatchClick(BatchModel batch);
    }

    private OnBatchClickListener listener;

    public BatchAdapter(Context context,
                        List<BatchModel> batchList,
                        OnBatchClickListener listener) {

        this.context = context;
        this.displayList = new ArrayList<>(batchList);
        this.fullList = new ArrayList<>(batchList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.batch_item, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {

        BatchModel batch = displayList.get(position);

        holder.tvName.setText(batch.getName());
        holder.tvSubject.setText("Subject: " + batch.getSubject());

        // Format Start & End Time
        String timeRange = formatTime(batch.getStartTime())
                + " - "
                + formatTime(batch.getEndTime());

        holder.tvTime.setText(timeRange);

        // Format Duration
        holder.tvDuration.setText("Duration: "
                + formatDuration(batch.getDurationMinutes()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBatchClick(batch);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // ------------------ Firestore Timestamp → Readable Time ------------------

    private String formatTime(Timestamp timestamp) {

        if (timestamp == null) return "--";

        Date date = timestamp.toDate();
        SimpleDateFormat sdf =
                new SimpleDateFormat("hh:mm a", Locale.getDefault());

        return sdf.format(date);
    }

    // ------------------ Format Duration ------------------

    private String formatDuration(long minutes) {

        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0) {
            return hours + "h " + mins + "m";
        } else {
            return mins + "m";
        }
    }

    // ------------------ Update Lists ------------------

    public void setBatches(List<BatchModel> batches) {

        fullList.clear();
        fullList.addAll(batches);

        displayList.clear();
        displayList.addAll(batches);

        notifyDataSetChanged();
    }

    // ------------------ Search ------------------

    public void filter(String query) {

        displayList.clear();

        if (query == null || query.trim().isEmpty()) {

            displayList.addAll(fullList);

        } else {

            String lowerQuery = query.toLowerCase().trim();

            for (BatchModel batch : fullList) {

                if (batch.getName().toLowerCase().contains(lowerQuery)
                        || batch.getSubject().toLowerCase().contains(lowerQuery)
                        || formatTime(batch.getStartTime()).toLowerCase().contains(lowerQuery)
                        || formatTime(batch.getEndTime()).toLowerCase().contains(lowerQuery)) {

                    displayList.add(batch);
                }
            }
        }

        notifyDataSetChanged();
    }

    // ------------------ ViewHolder ------------------

    public static class BatchViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvSubject, tvTime, tvDuration;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvBatchName);
            tvSubject = itemView.findViewById(R.id.tvBatchSubject);
            tvTime = itemView.findViewById(R.id.tvBatchTime);
            tvDuration = itemView.findViewById(R.id.tvBatchDuration);
        }
    }
}