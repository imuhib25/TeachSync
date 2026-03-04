package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {

    private List<ActivityModel> activityList;

    public ActivityAdapter(List<ActivityModel> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityModel activity = activityList.get(position);
        holder.tvContent.setText(activity.getContent());
        
        String amount = activity.getAmount();
        if (amount == null || amount.isEmpty()) {
            holder.tvAmount.setVisibility(View.GONE);
        } else {
            holder.tvAmount.setVisibility(View.VISIBLE);
            holder.tvAmount.setText(amount);
            
            if (amount.startsWith("+")) {
                holder.tvAmount.setTextColor(0xFF00E676);
            } else if (amount.startsWith("-")) {
                holder.tvAmount.setTextColor(0xFFFF5252);
            } else {
                holder.tvAmount.setTextColor(0xFFFFFFFF);
            }
        }
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public void addActivity(ActivityModel activity) {
        activityList.add(0, activity);
        notifyItemInserted(0);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvActivityContent);
            tvAmount = itemView.findViewById(R.id.tvActivityAmount);
        }
    }
}
