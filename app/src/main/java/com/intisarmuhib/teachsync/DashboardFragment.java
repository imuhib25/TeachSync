package com.intisarmuhib.teachsync;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class DashboardFragment extends Fragment {

    private TextView welcomeText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment view
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views using fragment's view
        welcomeText = view.findViewById(R.id.welcome_text);

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

        return view;
    }
    }