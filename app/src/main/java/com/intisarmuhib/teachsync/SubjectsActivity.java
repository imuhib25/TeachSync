package com.intisarmuhib.teachsync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    FloatingActionButton fab;
    FirebaseFirestore db;

    List<SubjectModel> subjectList;
    SearchView searchView;
    SubjectAdapter adapter;

    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subjects);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        recyclerView = findViewById(R.id.recyclerViewSubs);
        fab = findViewById(R.id.fabAddSub);
        searchView = findViewById(R.id.searchSub);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> onBackPressed());


        db = FirebaseFirestore.getInstance();
        subjectList = new ArrayList<>();

        adapter = new SubjectAdapter(subjectList, this::showSubjectDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadSubjects();
        enableSwipe();
        setupSearch();


        fab.setOnClickListener(v -> showSubjectDialog(null));
    }

    private void loadSubjects() {
        db.collection("users").document(DashboardFragment.userId).collection("subjects")
                .addSnapshotListener((value, error) -> {
                    subjectList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            SubjectModel subject = doc.toObject(SubjectModel.class);
                            subject.setId(doc.getId());
                            subjectList.add(subject);
                        }
                    }
                    adapter.setSubjects(subjectList);
                });
    }

    private void showSubjectDialog(SubjectModel subjectToEdit) {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_subject, null);
        dialog.setContentView(view);

        EditText etName = view.findViewById(R.id.etSubjectName);
        EditText etCode = view.findViewById(R.id.etSubjectCode);
        EditText etTeacher = view.findViewById(R.id.etSubjectTeacher);

        boolean isEdit = subjectToEdit != null;

        if (isEdit) {
            etName.setText(subjectToEdit.getName());
            etCode.setText(subjectToEdit.getCode());
            etTeacher.setText(subjectToEdit.getTeacher());
        }

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {

            String id = isEdit ? subjectToEdit.getId()
                    : db.collection("users").document(DashboardFragment.userId).collection("subjects").document().getId();

            SubjectModel subject = new SubjectModel(
                    id,
                    etName.getText().toString().trim(),
                    etCode.getText().toString().trim(),
                    etTeacher.getText().toString().trim()
            );

            db.collection("users").document(DashboardFragment.userId).collection("subjects").document(id).set(subject);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void enableSwipe() {

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int position = viewHolder.getAdapterPosition();
                SubjectModel deleted = adapter.getItem(position);

                db.collection("users").document(DashboardFragment.userId).collection("subjects").document(deleted.getId()).delete();

                Snackbar.make(recyclerView, "Subject deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v ->
                                db.collection("users").document(DashboardFragment.userId).collection("subjects")
                                        .document(deleted.getId())
                                        .set(deleted))
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }
    private void setupSearch() {
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchSub);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }
}