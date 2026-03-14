package com.intisarmuhib.teachsync;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    FloatingActionButton fab;
    FirebaseFirestore db;
    String userId;

    List<SubjectModel> subjectList;
    SearchView searchView;
    SubjectAdapter adapter;

    private ImageButton backButton;
    private ChipGroup chipGroupCommon;
    private LinearLayout layoutEmpty;
    private AdView mAdView;

    private final String[] commonSubjects = {
            "Mathematics", "Physics", "Chemistry", "Biology", "English",
            "ICT", "Higher Math", "Accounting", "Finance", "Economics"
    };

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

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        // Initialize and load Banner Ad
        mAdView = findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        recyclerView = findViewById(R.id.recyclerViewSubs);
        fab = findViewById(R.id.fabAddSub);
        searchView = findViewById(R.id.searchSub);
        backButton = findViewById(R.id.back_button);
        chipGroupCommon = findViewById(R.id.chipGroupCommon);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        backButton.setOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        subjectList = new ArrayList<>();

        adapter = new SubjectAdapter(subjectList, this::showSubjectDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupCommonSubjects();
        loadSubjects();
        setupSearch();

        SubjectAdapter.attachSwipeToDelete(recyclerView, position -> {
            if (position == RecyclerView.NO_POSITION) return;
            SubjectModel deleted = adapter.getItem(position);
            if (deleted == null) return;

            adapter.removeItem(position);
            subjectList.remove(deleted);
            updateNoDataVisibility();

            db.collection("users").document(userId).collection("subjects").document(deleted.getId()).delete();

            Snackbar.make(recyclerView, "Subject deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        adapter.restoreItem(deleted, position);
                        subjectList.add(deleted);
                        updateNoDataVisibility();
                        db.collection("users").document(userId).collection("subjects")
                                .document(deleted.getId())
                                .set(deleted);
                    }).show();
        });

        fab.setOnClickListener(v -> showSubjectDialog(null));
    }

    private void setupCommonSubjects() {
        for (String subjectName : commonSubjects) {
            Chip chip = new Chip(this);
            chip.setText(subjectName);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> addCommonSubject(subjectName));
            chipGroupCommon.addView(chip);
        }
    }

    private void addCommonSubject(String name) {
        for (SubjectModel s : subjectList) {
            if (s.getName() != null && s.getName().equalsIgnoreCase(name)) {
                Toast.makeText(this, name + " already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String id = db.collection("users").document(userId).collection("subjects").document().getId();
        SubjectModel subject = new SubjectModel(id, name, "", "");
        db.collection("users").document(userId).collection("subjects").document(id).set(subject)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show());
    }

    private void loadSubjects() {
        db.collection("users").document(userId).collection("subjects")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("SubjectsActivity", "Error loading subjects", error);
                        return;
                    }
                    subjectList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            SubjectModel subject = doc.toObject(SubjectModel.class);
                            if (subject != null) {
                                subject.setId(doc.getId());
                                subjectList.add(subject);
                            }
                        }
                    }
                    adapter.setSubjects(subjectList);
                    updateNoDataVisibility();
                });
    }

    private void updateNoDataVisibility() {
        if (subjectList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showSubjectDialog(SubjectModel subjectToEdit) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_subject, null);
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
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Required");
                return;
            }

            String id = isEdit ? subjectToEdit.getId()
                    : db.collection("users").document(userId).collection("subjects").document().getId();

            SubjectModel subject = new SubjectModel(
                    id,
                    name,
                    etCode.getText().toString().trim(),
                    etTeacher.getText().toString().trim()
            );

            db.collection("users").document(userId).collection("subjects").document(id).set(subject);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupSearch() {
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

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }
}
