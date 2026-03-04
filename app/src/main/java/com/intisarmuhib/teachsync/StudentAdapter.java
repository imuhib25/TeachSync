package com.intisarmuhib.teachsync;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private final Activity activity;
    private final List<StudentModel> studentList;
    private List<StudentModel> fullList;
    private final FirebaseFirestore db;
    private final String currentUserId;

    public StudentAdapter(Activity activity, List<StudentModel> studentList) {
        this.activity = activity;
        this.studentList = studentList;
        this.fullList = new ArrayList<>(studentList);
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentModel student = studentList.get(position);

        holder.name.setText(student.getName());
        holder.batch.setText(activity.getString(R.string.batches_label, student.getBatchesDisplay()));
        holder.phone.setText(student.getPhone());
        holder.parent.setText(activity.getString(R.string.parent_label, student.getParent()));

        holder.itemView.setOnClickListener(v -> showEditDialog(student));
        
        holder.btnCall.setOnClickListener(v -> {
            String phoneNumber = student.getPhone();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, R.string.phone_unavailable, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, batch, phone, parent;
        ImageView btnCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            batch = itemView.findViewById(R.id.txtBatch);
            phone = itemView.findViewById(R.id.txtPhone);
            parent = itemView.findViewById(R.id.txtParent);
            btnCall = itemView.findViewById(R.id.btn_call_student);
        }
    }

    public void filter(String text) {
        studentList.clear();
        if (text == null || text.isEmpty()) {
            studentList.addAll(fullList);
        } else {
            String query = text.toLowerCase().trim();
            for (StudentModel item : fullList) {
                if (item.getName() != null && item.getName().toLowerCase().contains(query)) {
                    studentList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateList(List<StudentModel> newList) {
        this.fullList = new ArrayList<>(newList);
        if (this.studentList != newList) {
            this.studentList.clear();
            this.studentList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    private void showEditDialog(StudentModel student) {
        if (currentUserId == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_add_student, null);
        dialog.setContentView(view);

        TextInputEditText nameInput = view.findViewById(R.id.edtName);
        TextInputEditText emailInput = view.findViewById(R.id.edtEmail);
        TextInputEditText phoneInput = view.findViewById(R.id.edtPhone);
        TextInputEditText parentInput = view.findViewById(R.id.edtParent);
        AutoCompleteTextView etBatch = view.findViewById(R.id.etBatchName);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupBatches);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveStudent);
        
        List<String> selectedBatches = new ArrayList<>(student.getBatches());
        nameInput.setText(student.getName());
        emailInput.setText(student.getEmail());
        phoneInput.setText(student.getPhone());
        parentInput.setText(student.getParent());
        btnSave.setText(R.string.update_student);

        refreshChips(chipGroup, selectedBatches);

        Map<String, String> batchIdMap = new HashMap<>();
        List<String> allBatchNames = new ArrayList<>();
        db.collection("users").document(currentUserId).collection("batches").get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot doc : snapshot) {
                String bName = doc.getString("name");
                if (bName != null) {
                    allBatchNames.add(bName);
                    batchIdMap.put(bName, doc.getId());
                }
            }
            ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, allBatchNames);
            etBatch.setAdapter(batchAdapter);
        });

        etBatch.setOnItemClickListener((parent1, view1, position, id) -> {
            String selected = (String) parent1.getItemAtPosition(position);
            if (!selectedBatches.contains(selected)) {
                selectedBatches.add(selected);
                refreshChips(chipGroup, selectedBatches);
            }
            etBatch.setText(""); 
        });

        btnSave.setOnClickListener(v -> {
            String newName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String newEmail = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String newPhone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
            String newParent = parentInput.getText() != null ? parentInput.getText().toString().trim() : "";

            if (newName.isEmpty() || selectedBatches.isEmpty()) {
                Toast.makeText(activity, R.string.error_name_batch_required, Toast.LENGTH_SHORT).show();
                return;
            }

            WriteBatch writeBatch = db.batch();
            List<String> oldBatches = student.getBatches();
            
            for (String b : oldBatches) {
                if (!selectedBatches.contains(b)) {
                    String bId = batchIdMap.get(b);
                    if (bId != null) writeBatch.update(db.collection("users").document(currentUserId).collection("batches").document(bId), "enrolledCount", FieldValue.increment(-1));
                }
            }
            
            for (String b : selectedBatches) {
                if (!oldBatches.contains(b)) {
                    String bId = batchIdMap.get(b);
                    if (bId != null) writeBatch.update(db.collection("users").document(currentUserId).collection("batches").document(bId), "enrolledCount", FieldValue.increment(1));
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("email", newEmail);
            updates.put("phone", newPhone);
            updates.put("batches", selectedBatches);
            updates.put("parent", newParent);

            writeBatch.update(db.collection("users").document(currentUserId).collection("students").document(student.getId()), updates);

            writeBatch.commit().addOnSuccessListener(unused -> {
                Toast.makeText(activity, R.string.student_updated, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(activity, R.string.update_failed, Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    private void refreshChips(ChipGroup chipGroup, List<String> selectedBatches) {
        chipGroup.removeAllViews();
        for (String b : selectedBatches) {
            Chip chip = new Chip(activity);
            chip.setText(b);
            chip.setTextColor(android.graphics.Color.WHITE);
            chip.setChipBackgroundColorResource(R.color.purple_500);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTintResource(android.R.color.white);
            chip.setOnCloseIconClickListener(v -> {
                selectedBatches.remove(b);
                refreshChips(chipGroup, selectedBatches);
            });
            chipGroup.addView(chip);
        }
    }
}