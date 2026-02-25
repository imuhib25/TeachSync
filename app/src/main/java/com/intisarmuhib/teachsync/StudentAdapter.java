package com.intisarmuhib.teachsync;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private Activity activity;
    private List<StudentModel> studentList;
    private List<StudentModel> fullList;
    private FirebaseFirestore db;
    String userID;
    public StudentAdapter(Activity activity, List<StudentModel> studentList) {
        this.activity = activity;
        this.studentList = studentList;
        this.fullList = new ArrayList<>(studentList);
        this.db = FirebaseFirestore.getInstance();
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
        holder.email.setText(student.getEmail());
        userID = DashboardFragment.userId;

        // Edit on click
        holder.itemView.setOnClickListener(v -> showEditDialog(student, userID));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView name, email;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            email = itemView.findViewById(R.id.txtEmail);
        }
    }

    // 🔍 Search Filter
    public void filter(String text) {
        studentList.clear();

        if (text.isEmpty()) {
            studentList.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for (StudentModel item : fullList) {
                if (item.getName().toLowerCase().contains(text)) {
                    studentList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateFullList(List<StudentModel> newList) {
        fullList = new ArrayList<>(newList);
    }

    // ✏ Edit Student
    private void showEditDialog(StudentModel student, String userID) {

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.bottom_add_student, null);

        dialog.setContentView(view);

        TextInputEditText name = view.findViewById(R.id.edtName);
        TextInputEditText email = view.findViewById(R.id.edtEmail);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveStudent);

        name.setText(student.getName());
        email.setText(student.getEmail());
        btnSave.setText("Update Student");

        btnSave.setOnClickListener(v -> {

            String newName = name.getText().toString().trim();
            String newEmail = email.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(activity, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(userID).collection("students")
                    .document(student.getId())
                    .update("name", newName,
                            "email", newEmail)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(activity, "Student Updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(activity, "Update Failed", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    // 🗑 Delete Student
    public void deleteStudent(int position) {

        StudentModel student = studentList.get(position);
        db.collection("users").document(userID).collection("students")
                .document(student.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(activity, "Student Deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(activity, "Delete Failed", Toast.LENGTH_SHORT).show());
    }
}