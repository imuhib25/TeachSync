package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardClassAdapter extends RecyclerView.Adapter<DashboardClassAdapter.ViewHolder> {

    private List<ClassModel> list = new ArrayList<>();

    public void setData(List<ClassModel> newList) {
        this.list = new ArrayList<>(newList != null ? newList : new ArrayList<>());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassModel model = list.get(position);
        holder.tvTopic.setText(model.getTopic());
        holder.tvBatch.setText(model.getBatch());
        holder.tvClassTime.setText(model.getClassTime());

        SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String today = dateSdf.format(new Date());
        
        if (today.equals(model.getDate())) {
            holder.tvDate.setText("Today");
        } else {
            holder.tvDate.setText(model.getDate());
        }

        holder.tvCountdown.setText(getRemainingTime(model));
    }

    private String getRemainingTime(ClassModel model) {
        try {
            String startTimeStr = model.getClassTime().split("-")[0].trim();
            SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            Date startTime = fullFmt.parse(model.getDate() + " " + startTimeStr);
            
            long diffMillis = startTime.getTime() - System.currentTimeMillis();
            if (diffMillis <= 0) return "Starting now";

            long diffMinutes = diffMillis / (60 * 1000);
            long hours = diffMinutes / 60;
            long minutes = diffMinutes % 60;

            if (hours > 24) {
                long days = hours / 24;
                return "In " + days + "d " + (hours % 24) + "h";
            } else if (hours > 0) {
                return "In " + hours + "h " + minutes + "m";
            } else {
                return "In " + minutes + " mins";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvBatch, tvClassTime, tvDate, tvCountdown;

        ViewHolder(View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvBatch = itemView.findViewById(R.id.tvBatch);
            tvClassTime = itemView.findViewById(R.id.tvClassTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCountdown = itemView.findViewById(R.id.tvCountdown);
        }
    }
}
