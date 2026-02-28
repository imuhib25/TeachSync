package com.intisarmuhib.teachsync;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class ScheduleFragment extends Fragment {

    private LinearLayout dateContainer;
    private TextView tvSessionCount;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    private FirebaseFirestore db;
    private ClassAdapter adapter;

    private String selectedDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        dateContainer = view.findViewById(R.id.dateContainer);
        tvSessionCount = view.findViewById(R.id.tvSessionCount);
        recyclerView = view.findViewById(R.id.classRecycler);
        fabAdd = view.findViewById(R.id.fabAdd);

        db = FirebaseFirestore.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassAdapter();
        recyclerView.setAdapter(adapter);

        generateDateChips();

        fabAdd.setOnClickListener(v -> showAddClassBottomSheet());

        return view;
    }

    private void generateDateChips() {

        dateContainer.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {

            View chip = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_date_chip, dateContainer, false);

            TextView tvDay = chip.findViewById(R.id.tvDay);
            TextView tvDate = chip.findViewById(R.id.tvDate);

            Date date = calendar.getTime();

            tvDay.setText(dayFormat.format(date));
            tvDate.setText(new SimpleDateFormat("dd").format(date));

            String formattedDate = fullFormat.format(date);

            if (i == 0) {
                chip.setSelected(true);
                selectedDate = formattedDate;
                loadClassesForDate(selectedDate);
            }

            chip.setOnClickListener(v -> {
                clearSelections();
                chip.setSelected(true);
                selectedDate = formattedDate;
                loadClassesForDate(selectedDate);
            });

            dateContainer.addView(chip);
            calendar.add(Calendar.DATE, 1);
        }
    }

    private void clearSelections() {
        for (int i = 0; i < dateContainer.getChildCount(); i++) {
            dateContainer.getChildAt(i).setSelected(false);
        }
    }

    private void loadClassesForDate(String date) {

        db.collection("classes")
                .whereEqualTo("date", date)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    List<ClassModel> list = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        list.add(doc.toObject(ClassModel.class));
                    }

                    adapter.setData(list);
                    tvSessionCount.setText(list.size() + " Sessions");
                });
    }

    private void showAddClassBottomSheet() {

        BottomSheetDialog sheetDialog = new BottomSheetDialog(requireContext());

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_add_class, null);

        sheetDialog.setContentView(view);

        TextInputEditText etTopic = view.findViewById(R.id.etTopic);
        AutoCompleteTextView dropBatch = view.findViewById(R.id.dropBatch);
        AutoCompleteTextView dropMonthly =
                view.findViewById(R.id.dropMonthlyClass);
        TextInputEditText etDate = view.findViewById(R.id.etDate);
        CheckBox checkExtra = view.findViewById(R.id.checkExtra);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        // Monthly Number Dropdown (1–20)
        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            numbers.add("Class " + i);
        }

        ArrayAdapter<String> monthlyAdapter =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, numbers);

        dropMonthly.setAdapter(monthlyAdapter);

        etDate.setText(selectedDate);
        final String[] sheetDate = {selectedDate};

        etDate.setOnClickListener(v -> {

            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Select Date")
                            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                            .build();

            picker.show(getParentFragmentManager(), "DATE_PICKER");

            picker.addOnPositiveButtonClickListener(selection -> {

                SimpleDateFormat sdf =
                        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                sheetDate[0] = sdf.format(new Date(selection));
                etDate.setText(sheetDate[0]);
            });
        });

        btnSave.setOnClickListener(v -> {

            String topic = etTopic.getText().toString().trim();
            String batch = dropBatch.getText().toString().trim();
            String monthlyNumber = dropMonthly.getText().toString().trim();
            boolean isExtra = checkExtra.isChecked();

            if (topic.isEmpty()) {
                etTopic.setError("Required");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("topic", topic);
            data.put("batch", batch);
            data.put("date", sheetDate[0]);
            data.put("monthlyNumber", monthlyNumber);
            data.put("extra", isExtra);
            data.put("createdAt", FieldValue.serverTimestamp());

            db.collection("classes").add(data);

            sheetDialog.dismiss();
        });

        sheetDialog.show();
    }
}