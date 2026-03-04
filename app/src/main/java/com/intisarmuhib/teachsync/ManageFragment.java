package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class ManageFragment extends Fragment {

    MaterialButton btnAddStudents;
    MaterialButton btnAddSubjects;
    MaterialButton btnAddBatches;
    ImageView profilePic;
    FirebaseAuth mAuth;
    FirebaseFirestore firestore;


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

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            DocumentReference documentReference = firestore.collection("users").document(userId);
            documentReference.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) return;
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty() && isAdded()) {
                        Glide.with(this).load(avatarUrl).circleCrop().into(profilePic);
                    }
                }
            });
        }

        btnAddStudents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), StudentsActivity.class));

            }
        });

        btnAddSubjects.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
                                                  startActivity(new Intent(getActivity(), SubjectsActivity.class));

                                              }
                                          });
        btnAddBatches.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), BatchesActivity.class));

            }
        });

                // Inflate the layout for this fragment
        return view;
    }

    private void showDialog(Context context, int layoutResId){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutResId);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

    }
}