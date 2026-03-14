package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class ManageFragment extends Fragment {

    MaterialButton btnAddStudents;
    MaterialButton btnAddSubjects;
    MaterialButton btnAddBatches;
    ImageView profilePic;
    FirebaseAuth mAuth;
    FirebaseFirestore firestore;
    private ListenerRegistration userListener;
    private AdView mAdView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manage, container, false);

        btnAddBatches = view.findViewById(R.id.btnAddBatches);
        btnAddStudents = view.findViewById(R.id.btnAddStudents);
        btnAddSubjects = view.findViewById(R.id.btnAddSubjects);
        profilePic = view.findViewById(R.id.manage_profile_pic);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize and load Banner Ad
        mAdView = view.findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            DocumentReference documentReference = firestore.collection("users").document(userId);
            userListener = documentReference.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) return;
                if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).circleCrop().into(profilePic);
                    }
                }
            });
        }

        btnAddStudents.setOnClickListener(v -> startActivity(new Intent(getActivity(), StudentsActivity.class)));

        btnAddSubjects.setOnClickListener(v -> startActivity(new Intent(getActivity(), SubjectsActivity.class)));
        
        btnAddBatches.setOnClickListener(v -> startActivity(new Intent(getActivity(), BatchesActivity.class)));

        return view;
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroyView() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    private void showDialog(Context context, int layoutResId){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutResId);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}