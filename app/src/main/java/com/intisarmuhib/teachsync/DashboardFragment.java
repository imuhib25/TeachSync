package com.intisarmuhib.teachsync;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView welcomeText, tvTotalStudents, tvTotalStudentsMonth;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    public static String userId;
    private List<StudentModel> studentList;
    private RecyclerView rvActivity;
    private ActivityAdapter activityAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment view
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views using fragment's view
        welcomeText = view.findViewById(R.id.welcome_text);
        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvTotalStudentsMonth = view.findViewById(R.id.tvTotalStudentsMonth);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Get current user
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();

            DocumentReference documentReference = firestore.collection("users").document(userId);
            documentReference.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) return;
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String fName = documentSnapshot.getString("fName");
                    welcomeText.setText("Welcome back,\n" + fName);
                }
            });
        }
        rvActivity = view.findViewById(R.id.recyclerRecentActivity);
        List<ActivityModel> activityList = new ArrayList<>();
        activityAdapter = new ActivityAdapter(activityList);
        rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivity.setAdapter(activityAdapter);
        studentList = new ArrayList<>();


        updateRecentActivity();
        updateTotalStudents();
        return view;
    }
    private void updateTotalStudents() {
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("students")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    int totalStudents = value.size();
                    int thisMonth = 0;

                    java.util.Calendar now = java.util.Calendar.getInstance();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp("createdAt");
                        if (timestamp != null) {
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(timestamp.toDate());
                            if (cal.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) &&
                                    cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)) {
                                thisMonth++;
                            }
                        }
                    }

                    tvTotalStudents.setText(String.valueOf(totalStudents));
                    tvTotalStudentsMonth.setText("+" + thisMonth + " this month");
                });
    }
    private void updateRecentActivity(){
        // Listen for new batches, students & subjects added
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("batches")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                BatchModel batch = dc.getDocument().toObject(BatchModel.class);
                                ActivityModel activity = new ActivityModel(
                                        "New Batch Added",
                                        batch.getName(),
                                        ""
                                );
                                activityAdapter.addActivity(activity);
                            }
                        }
                    }
                });
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("students")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                StudentModel studentModel = dc.getDocument().toObject(StudentModel.class);
                                ActivityModel activity = new ActivityModel(
                                        "New Student Added",
                                        studentModel.getName(),
                                        studentModel.getBatch()
                                );
                                activityAdapter.addActivity(activity);
                            }
                        }
                    }
                });
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("subjects")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                SubjectModel subjectModel = dc.getDocument().toObject(SubjectModel.class);
                                ActivityModel activity = new ActivityModel(
                                        "New Subject Added",
                                        subjectModel.getName(),
                                        subjectModel.getCode()
                                );
                                activityAdapter.addActivity(activity);
                            }
                        }
                    }
                });
        // Listen for new batches, students & subjects deleted
            //code will be added

        // Listen for new batches, students & subjects updated
            //code will be added
    }
    }