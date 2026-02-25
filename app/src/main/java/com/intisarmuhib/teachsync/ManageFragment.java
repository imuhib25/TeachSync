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

import com.google.android.material.button.MaterialButton;


public class ManageFragment extends Fragment {

    MaterialButton btnAddStudents;
    MaterialButton btnAddSubjects;
    MaterialButton btnAddBatches;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manage, container, false);

       btnAddBatches = view.findViewById(R.id.btnAddBatches);
        btnAddStudents = view.findViewById(R.id.btnAddStudents);
        btnAddSubjects = view.findViewById(R.id.btnAddSubjects);

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