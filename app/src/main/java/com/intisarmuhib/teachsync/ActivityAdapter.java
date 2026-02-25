package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<ActivityModel> activityList;

    public ActivityAdapter(List<ActivityModel> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityModel activity = activityList.get(position);
        holder.tvTitle.setText(activity.getTitle());
        holder.tvDesc.setText(activity.getDescription());
        holder.tvAmount.setText(activity.getAmount());
    }

    @Override
    public int getItemCount() { return activityList.size(); }

    public void addActivity(ActivityModel activity) {
        activityList.add(0, activity); // add at top
        notifyItemInserted(0);
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvAmount;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvDesc = itemView.findViewById(R.id.tvActivityDesc);
            tvAmount = itemView.findViewById(R.id.tvActivityAmount);
        }
    }
}
