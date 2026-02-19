package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class ProfileFragment extends Fragment {

    private TextView pfullname;
    private TextView pjoindate;
    private ImageView settingsBtn;
    private View profileOption;
    private View profileSecurity;
    private View profileNotification;
    private View profileContact;
    private View profileHelp;
    private ImageView logoutBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String userId;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the fragment view
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views using fragment's view
        pfullname = view.findViewById(R.id.profile_fullname);
        pjoindate = view.findViewById(R.id.profile_joindate);
        logoutBtn = view.findViewById(R.id.profile_logoutBtn);
        profileOption = view.findViewById(R.id.profile_option);
        settingsBtn = view.findViewById(R.id.profile_settingsBtn);
        profileSecurity =view.findViewById(R.id.profile_security);
        profileNotification = view.findViewById(R.id.profile_notification);
        profileContact = view.findViewById(R.id.profile_contact);
        profileHelp = view.findViewById(R.id.profile_help);

        profileHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(getContext(),R.layout.dialog_help);
            }
        });

        profileContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(getContext(),R.layout.dialog_contact);
            }
        });

        profileNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(getContext(),R.layout.dialog_notifications);
            }
        });

        profileSecurity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(getContext(),R.layout.dialog_security);

            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(getContext(),R.layout.dialog_settings);
            }
        });

        profileOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(getContext(),R.layout.dialog_personal_info);
            }
        });


logoutBtn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        showLogoutDialog();
    }
});



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
                    pfullname.setText(fName);
                    String date = documentSnapshot.getString("dateofjoin");
                    pjoindate.setText("Joined "+date);
                }
            });
        }

        return view;
    }
    private void showLogoutDialog(){
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_logout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        Button btnLogout = dialog.findViewById(R.id.ldbtnLogout);
        Button btnCancel = dialog.findViewById(R.id.ldbtnCancel);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout(v);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }
    private void logout(View view) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getContext(), LoginActivity.class));
        getActivity().finish();
    }
    private void showDialog(Context context, int layoutResId){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutResId);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

    }

}