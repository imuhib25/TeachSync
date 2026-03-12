package com.intisarmuhib.teachsync;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
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
                        || (subjectModel.getTeacher() != null && subjectModel.getTeacher().toLowerCase().contains(lowerQuery))
                        || (subjectModel.getCode() != null && subjectModel.getCode().toLowerCase().contains(lowerQuery))) {
                    displayList.add(subjectModel);
                }
            }
        }

        notifyDataSetChanged();
    }

    public SubjectModel getItem(int position) {
        if (position >= 0 && position < displayList.size()) {
            return displayList.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < displayList.size()) {
            SubjectModel removed = displayList.remove(position);
            fullList.remove(removed);
            notifyItemRemoved(position);
        }
    }

    public void restoreItem(SubjectModel item, int position) {
        if (position >= 0 && position <= displayList.size()) {
            displayList.add(position, item);
            fullList.add(item);
            notifyItemInserted(position);
        }
    }

    public static void attachSwipeToDelete(RecyclerView recyclerView, OnSwipeListener swipeListener) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                swipeListener.onSwiped(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder,
                                     float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                ColorDrawable bg = new ColorDrawable(Color.parseColor("#C62828"));
                bg.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                bg.draw(c);

                Drawable icon = ContextCompat.getDrawable(rv.getContext(), R.drawable.ic_delete);
                if (icon != null) {
                    icon.setTint(Color.WHITE);
                    int margin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int top = itemView.getTop() + margin;
                    int left = itemView.getRight() - margin - icon.getIntrinsicWidth();
                    icon.setBounds(left, top, left + icon.getIntrinsicWidth(), top + icon.getIntrinsicHeight());
                    icon.draw(c);
                }
                super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recyclerView);
    }

    public interface OnSwipeListener {
        void onSwiped(int position);
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
