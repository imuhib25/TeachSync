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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton fab;
    StudentAdapter adapter;
    List<StudentModel> studentList;
    private FirebaseAuth mAuth;
    TextInputEditText edtSearch;
    FirebaseFirestore db;
    private ImageButton backButton;
    String userID;

    private List<String> batchList = new ArrayList<>();
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
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        int position = viewHolder.getAdapterPosition();
                        adapter.deleteStudent(position);
                    }
                };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);

        recyclerView = findViewById(R.id.recyclerStudents);
        fab = findViewById(R.id.fabAddStudent);
        edtSearch = findViewById(R.id.edtSearch);
        backButton = findViewById(R.id.back_button);
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        backButton.setOnClickListener(v -> onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        studentList = new ArrayList<>();
        adapter = new StudentAdapter(this, studentList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadStudents();
        loadBacthes();
        enableSwipeToDelete();
        setupSearch();

        fab.setOnClickListener(v -> showAddStudentBottomSheet());
    }

    // 🔹 Load Students from Firestore
    private void loadStudents() {

        db.collection("users").document(userID).collection("students")
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    studentList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        StudentModel student = doc.toObject(StudentModel.class);
                        student.setId(doc.getId());
                        studentList.add(student);
                    }

                    adapter.updateFullList(studentList);
                    adapter.notifyDataSetChanged();
                });
    }

    // Load Batches from Firestore
    private void loadBacthes() {
        db.collection("users").document(userID)
                .collection("batches")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    batchList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batchList.add(doc.getString("name"));
                    }
                });
    }

    // 🔹 Add Student Bottom Sheet
    private void showAddStudentBottomSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_add_student, null);

        dialog.setContentView(view);

        TextInputEditText edtName = view.findViewById(R.id.edtName);
        TextInputEditText edtEmail = view.findViewById(R.id.edtEmail);
        AutoCompleteTextView etBatch = view.findViewById(R.id.etBatchName);
        TextInputEditText edtPhone = view.findViewById(R.id.edtPhone);
        TextInputEditText edtParent = view.findViewById(R.id.edtParent);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveStudent);

        ArrayAdapter<String> bacthAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        batchList);

        etBatch.setAdapter(bacthAdapter);


        btnSave.setOnClickListener(v -> {

            String name = edtName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String batch = etBatch.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String parent = edtParent.getText().toString().trim();


            if (name.isEmpty() || email.isEmpty() || batch.isEmpty() || phone.isEmpty() || parent.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false);

            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("name", name);
            studentMap.put("batch", batch);
            studentMap.put("email", email);
            studentMap.put("phone", phone);
            studentMap.put("parent", parent);
            studentMap.put("createdAt", FieldValue.serverTimestamp());
            db.collection("users").document(userID)
                    .collection("students")
                    .add(studentMap)
                    .addOnSuccessListener(documentReference -> {

                        Toast.makeText(this, "Student Added Successfully", Toast.LENGTH_SHORT).show();

                        edtName.setText("");
                        edtEmail.setText("");
                        etBatch.setText("");
                        edtPhone.setText("");
                        edtParent.setText("");


                        btnSave.setEnabled(true);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {

                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Failed to Add Student", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    // 🔹 Swipe to Delete
    private void enableSwipeToDelete() {

        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        int position = viewHolder.getAdapterPosition();
                        StudentModel deletedStudent = studentList.get(position);

                        // Remove from list temporarily
                        studentList.remove(position);
                        adapter.notifyItemRemoved(position);

                        // Delete from Firestore
                        db.collection("users").document(userID).collection("students")
                                .document(deletedStudent.getId())
                                .delete();

                        Snackbar.make(recyclerView, "Student deleted", Snackbar.LENGTH_LONG)
                                .setAction("UNDO", v -> {

                                    // Restore locally
                                    studentList.add(position, deletedStudent);
                                    adapter.notifyItemInserted(position);

                                    // Restore in Firestore
                                    db.collection("users").document(userID).collection("students")
                                            .document(deletedStudent.getId())
                                            .set(deletedStudent);
                                })
                                .show();
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c,
                                            @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX,
                                            float dY,
                                            int actionState,
                                            boolean isCurrentlyActive) {

                        View itemView = viewHolder.itemView;

                        Paint paint = new Paint();
                        paint.setColor(Color.RED);

                        // Draw red background
                        c.drawRect(
                                itemView.getRight() + dX,
                                itemView.getTop(),
                                itemView.getRight(),
                                itemView.getBottom(),
                                paint
                        );

                        // Draw delete icon
                        Drawable icon = ContextCompat.getDrawable(
                                StudentsActivity.this,
                                R.drawable.ic_delete
                        );

                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();

                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    // 🔹 Search Setup
    private void setupSearch() {

        edtSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}