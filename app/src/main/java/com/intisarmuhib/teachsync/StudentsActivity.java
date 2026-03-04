package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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

public class StudentsActivity extends AppCompatActivity {

    private static final String TAG = "StudentsActivity";
    RecyclerView recyclerView;
    FloatingActionButton fab;
    StudentAdapter adapter;
    List<StudentModel> studentList;
    private FirebaseAuth mAuth;
    TextInputEditText edtSearch;
    FirebaseFirestore db;
    private ImageButton backButton;
    private LinearLayout layoutEmpty;
    String userID;

    private List<String> batchList = new ArrayList<>();
    private Map<String, String> batchIdMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_students);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerStudents);
        fab = findViewById(R.id.fabAddStudent);
        edtSearch = findViewById(R.id.edtSearch);
        backButton = findViewById(R.id.back_button);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        mAuth = FirebaseAuth.getInstance();
        
        if (mAuth.getCurrentUser() != null) {
            userID = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        studentList = new ArrayList<>();
        adapter = new StudentAdapter(this, studentList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadStudents();
        loadBatches();
        enableSwipeToDelete();
        setupSearch();

        fab.setOnClickListener(v -> showAddStudentBottomSheet());
    }

    private void loadStudents() {
        db.collection("users").document(userID).collection("students")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading students: ", error);
                        return;
                    }
                    if (value == null) return;
                    
                    studentList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        StudentModel student = doc.toObject(StudentModel.class);
                        if (student != null) {
                            student.setId(doc.getId());
                            studentList.add(student);
                        }
                    }
                    adapter.updateList(studentList);
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (studentList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void loadBatches() {
        db.collection("users").document(userID)
                .collection("batches")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    batchList.clear();
                    batchIdMap.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) {
                            batchList.add(name);
                            batchIdMap.put(name, doc.getId());
                        }
                    }
                });
    }

    private void showAddStudentBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_add_student, null);
        dialog.setContentView(view);

        TextInputEditText edtName = view.findViewById(R.id.edtName);
        TextInputEditText edtEmail = view.findViewById(R.id.edtEmail);
        AutoCompleteTextView etBatch = view.findViewById(R.id.etBatchName);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupBatches);
        TextInputEditText edtPhone = view.findViewById(R.id.edtPhone);
        TextInputEditText edtParent = view.findViewById(R.id.edtParent);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveStudent);

        List<String> selectedBatches = new ArrayList<>();
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, batchList);
        etBatch.setAdapter(batchAdapter);

        etBatch.setOnItemClickListener((parent1, view1, position, id) -> {
            String selected = (String) parent1.getItemAtPosition(position);
            if (!selectedBatches.contains(selected)) {
                selectedBatches.add(selected);
                refreshChips(chipGroup, selectedBatches);
            }
            etBatch.setText(""); 
        });

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String parent = edtParent.getText().toString().trim();

            if (name.isEmpty() || selectedBatches.isEmpty()) {
                Toast.makeText(this, "Name and at least one batch required", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false);
            
            WriteBatch batch = db.batch();
            String studentId = db.collection("users").document(userID).collection("students").document().getId();
            
            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("name", name);
            studentMap.put("batches", selectedBatches);
            studentMap.put("email", email);
            studentMap.put("phone", phone);
            studentMap.put("parent", parent);
            studentMap.put("createdAt", FieldValue.serverTimestamp());

            batch.set(db.collection("users").document(userID).collection("students").document(studentId), studentMap);

            for (String bName : selectedBatches) {
                String bId = batchIdMap.get(bName);
                if (bId != null) {
                    batch.update(db.collection("users").document(userID).collection("batches").document(bId), 
                            "enrolledCount", FieldValue.increment(1));
                }
            }

            batch.commit().addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Student Added Successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    private void refreshChips(ChipGroup chipGroup, List<String> selectedBatches) {
        chipGroup.removeAllViews();
        for (String b : selectedBatches) {
            Chip chip = new Chip(this);
            chip.setText(b);
            chip.setTextColor(Color.WHITE);
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

    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= studentList.size()) return;
                
                StudentModel deletedStudent = studentList.get(position);

                WriteBatch batch = db.batch();
                batch.delete(db.collection("users").document(userID).collection("students").document(deletedStudent.getId()));

                for (String bName : deletedStudent.getBatches()) {
                    String bId = batchIdMap.get(bName);
                    if (bId != null) {
                        batch.update(db.collection("users").document(userID).collection("batches").document(bId), 
                                "enrolledCount", FieldValue.increment(-1));
                    }
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    Toast.makeText(StudentsActivity.this, "Student Deleted", Toast.LENGTH_SHORT).show();
                    // updateEmptyState() will be called automatically by the snapshot listener
                });
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = vh.itemView;
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);
                Drawable icon = ContextCompat.getDrawable(StudentsActivity.this, R.drawable.ic_delete);
                if (icon != null) {
                    int margin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    icon.setBounds(itemView.getRight() - margin - icon.getIntrinsicWidth(), itemView.getTop() + margin, itemView.getRight() - margin, itemView.getTop() + margin + icon.getIntrinsicHeight());
                    icon.draw(c);
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                adapter.filter(s.toString()); 
                // We don't call updateEmptyState here because studentList hasn't changed, only the filtered view in adapter.
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}