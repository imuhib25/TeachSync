package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    private List<SubjectModel> displayList;
    private List<SubjectModel> fullList;
    private OnSubjectClickListener listener;

    public interface OnSubjectClickListener {
        void onSubjectClick(SubjectModel subject);
    }

    public SubjectAdapter(List<SubjectModel> list, OnSubjectClickListener listener) {
        this.displayList = new ArrayList<>(list);
        this.fullList = new ArrayList<>(list);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        SubjectModel subject = displayList.get(position);

        holder.tvName.setText(subject.getName());
        holder.tvDetails.setText("Code: " + subject.getCode());
              //  " | Teacher: " + subject.getTeacher());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSubjectClick(subject);
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public void setSubjects(List<SubjectModel> list) {
        fullList.clear();
        fullList.addAll(list);

        displayList.clear();
        displayList.addAll(list);

        notifyDataSetChanged();
    }
    // ------------------ Search / Filter ------------------
    public void filter(String query) {
        displayList.clear();

        if (query == null || query.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (SubjectModel subjectModel : fullList) {
                if (subjectModel.getName().toLowerCase().contains(lowerQuery)
                        || subjectModel.getTeacher().toLowerCase().contains(lowerQuery)
                        || subjectModel.getCode().toLowerCase().contains(lowerQuery)) {
                    displayList.add(subjectModel);
                }
            }
        }

        notifyDataSetChanged();
    }

    public SubjectModel getItem(int position) {
        return displayList.get(position);
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvDetails = itemView.findViewById(R.id.tvSubjectDetails);
        }
    }
}