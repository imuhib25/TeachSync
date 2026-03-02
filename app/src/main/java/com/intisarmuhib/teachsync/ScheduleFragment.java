package com.intisarmuhib.teachsync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduleFragment extends Fragment {

    private LinearLayout dateContainer;
    private TextView tvSessionCount;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    private FirebaseFirestore db;
    private ClassAdapter adapter;
    private ListenerRegistration classListener;

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

        enableSwipeDelete();
        generateDateChips();

        adapter.setListener(this::showClassBottomSheet);
        fabAdd.setOnClickListener(v -> showClassBottomSheet(null));

        return view;
    }

    // ====================================================
    // LOAD CLASSES LIVE
    // ====================================================
    private void loadClasses(String date) {

        if (classListener != null)
            classListener.remove();

        classListener = db.collection("users")
                .document(DashboardFragment.userId)
                .collection("classes")
                .whereEqualTo("date", date)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (value == null || error != null) return;

                    List<ClassModel> list = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ClassModel model = doc.toObject(ClassModel.class);
                        if (model != null) {
                            model.setId(doc.getId());
                            list.add(model);
                        }
                    }

                    adapter.setData(list);
                    tvSessionCount.setText(list.size() + " Sessions");
                });
    }

    // ====================================================
    // ADD / EDIT CLASS
    // ====================================================
    private void showClassBottomSheet(@Nullable ClassModel model) {

        boolean isEdit = model != null;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_add_class, null);
        dialog.setContentView(view);

        TextInputEditText etTopic = view.findViewById(R.id.etTopic);
        AutoCompleteTextView dropBatch = view.findViewById(R.id.dropBatch);
        TextInputEditText etDate = view.findViewById(R.id.etDate);
        CheckBox checkExtra = view.findViewById(R.id.checkExtra);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        if (isEdit) {
            etTopic.setText(model.getTopic());
            dropBatch.setText(model.getBatch(), false);
            etDate.setText(model.getDate());
            checkExtra.setChecked(model.isExtra());
        } else {
            etDate.setText(selectedDate);
        }

        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker().build();

            picker.show(getParentFragmentManager(), "DATE");

            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(selection);
                SimpleDateFormat format =
                        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                etDate.setText(format.format(cal.getTime()));
            });
        });

        Map<String,String> batchMap = new HashMap<>();
        List<String> batchNames = new ArrayList<>();

        db.collection("users")
                .document(DashboardFragment.userId)
                .collection("batches")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            batchNames.add(name);
                            batchMap.put(name, doc.getId());
                        }
                    }

                    ArrayAdapter<String> arrayAdapter =
                            new ArrayAdapter<>(requireContext(),
                                    android.R.layout.simple_dropdown_item_1line,
                                    batchNames);

                    dropBatch.setAdapter(arrayAdapter);
                    dropBatch.setOnClickListener(v -> dropBatch.showDropDown());
                });

        btnSave.setOnClickListener(v -> {

            String topic = etTopic.getText().toString().trim();
            String batchName = dropBatch.getText().toString().trim();
            String batchId = batchMap.get(batchName);
            String date = etDate.getText().toString();

            if (topic.isEmpty() || batchId == null || date.isEmpty()) {
                Toast.makeText(getContext(),
                        "Fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            saveClass(isEdit, model, topic, batchName,
                    batchId, date, checkExtra.isChecked(), dialog);
        });

        dialog.show();
    }

    // ====================================================
    // SAVE CLASS WITH MONTHLY LIMIT PROTECTION
    // ====================================================
    private void saveClass(boolean isEdit,
                           @Nullable ClassModel model,
                           String topic,
                           String batchName,
                           String batchId,
                           String date,
                           boolean isExtra,
                           BottomSheetDialog dialog) {

        DocumentReference batchRef =
                db.collection("users")
                        .document(DashboardFragment.userId)
                        .collection("batches")
                        .document(batchId);

        batchRef.get().addOnSuccessListener(batchSnap -> {

            if (!batchSnap.exists()) return;

            Long totalLong = batchSnap.getLong("totalMonthlyClasses");
            int totalAllowed = totalLong != null ? totalLong.intValue() : 0;

            db.collection("users")
                    .document(DashboardFragment.userId)
                    .collection("classes")
                    .whereEqualTo("batchId", batchId)
                    .get()
                    .addOnSuccessListener(classSnap -> {

                        int currentCount = classSnap.size();

                        // 🔥 RESET AFTER COMPLETION
                        if (currentCount >= totalAllowed) {
                            currentCount = 0;
                        }
                        int calculatedNumber;

                        if (isExtra) {
                            calculatedNumber = currentCount;   // do not increase
                        } else {
                            calculatedNumber = currentCount + 1;
                        }

                        final int nextNumber = calculatedNumber;

                        Timestamp start = batchSnap.getTimestamp("startTime");
                        Timestamp end = batchSnap.getTimestamp("endTime");

                        String timeText = "";
                        long startMillis = 0;
                        long endMillis = 0;

                        if (start != null && end != null) {

                            startMillis = start.toDate().getTime();
                            endMillis = end.toDate().getTime();

                            SimpleDateFormat format =
                                    new SimpleDateFormat("hh:mm a", Locale.getDefault());

                            timeText = format.format(start.toDate())
                                    + " - "
                                    + format.format(end.toDate());
                        }

                        Map<String,Object> data = new HashMap<>();
                        data.put("topic", topic);
                        data.put("batch", batchName);
                        data.put("batchId", batchId);
                        data.put("date", date);
                        data.put("extra", isExtra);
                        data.put("classTime", timeText);
                        data.put("startMillis", startMillis);
                        data.put("endMillis", endMillis);
                        data.put("createdAt", Timestamp.now());
                        data.put("monthlyNumber", nextNumber);

                        DocumentReference classRef;

                        if (isEdit)
                            classRef = db.collection("users")
                                    .document(DashboardFragment.userId)
                                    .collection("classes")
                                    .document(model.getId());
                        else
                            classRef = db.collection("users")
                                    .document(DashboardFragment.userId)
                                    .collection("classes")
                                    .document();

                        classRef.set(data).addOnSuccessListener(aVoid -> {

                            int remaining = totalAllowed - nextNumber;

                            tvSessionCount.setText(
                                    "Taken: " + nextNumber +
                                            " | Remaining: " + remaining
                            );

                            dialog.dismiss();

                            Snackbar.make(recyclerView,
                                    "Class Saved",
                                    Snackbar.LENGTH_SHORT).show();
                        });
                    });
        });
    }

    // ====================================================
    // DATE CHIPS
    // ====================================================
    private void generateDateChips() {

        dateContainer.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat =
                new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat fullFormat =
                new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {

            View chip = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_date_chip, dateContainer, false);

            TextView tvDay = chip.findViewById(R.id.tvDay);
            TextView tvDate = chip.findViewById(R.id.tvDate);

            Date date = calendar.getTime();
            String formatted = fullFormat.format(date);

            tvDay.setText(dayFormat.format(date));
            tvDate.setText(new SimpleDateFormat("dd").format(date));

            if (i == 0) {
                chip.setSelected(true);
                selectedDate = formatted;
                loadClasses(selectedDate);
            }

            chip.setOnClickListener(v -> {
                clearSelection();
                chip.setSelected(true);
                selectedDate = formatted;
                loadClasses(selectedDate);
            });

            dateContainer.addView(chip);
            calendar.add(Calendar.DATE, 1);
        }
    }

    private void enableSwipeDelete() {

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int direction) {

                        int position = viewHolder.getAdapterPosition();
                        ClassModel deleted = adapter.getItem(position);
                        deleteClass(deleted);
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c,
                                            @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY,
                                            int actionState,
                                            boolean isCurrentlyActive) {

                        View itemView = viewHolder.itemView;
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);

                        RectF background = new RectF(
                                itemView.getRight() + dX,
                                itemView.getTop(),
                                itemView.getRight(),
                                itemView.getBottom()
                        );

                        c.drawRect(background, paint);

                        super.onChildDraw(c, recyclerView,
                                viewHolder, dX, dY,
                                actionState, isCurrentlyActive);
                    }
                };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void deleteClass(ClassModel deleted) {

        String monthKey = deleted.getMonthKey();

        db.collection("users")
                .document(DashboardFragment.userId)
                .collection("classes")
                .document(deleted.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {

                    renumberMonthly(deleted.getBatchId(), monthKey);

                    Snackbar.make(recyclerView,
                                    "Class Deleted",
                                    Snackbar.LENGTH_LONG)
                            .show();
                });
    }

    private void renumberMonthly(String batchId, String monthKey) {

        db.collection("users")
                .document(DashboardFragment.userId)
                .collection("classes")
                .whereEqualTo("batchId", batchId)
                .whereEqualTo("monthKey", monthKey)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int count = 1;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().update("monthlyNumber", count);
                        count++;
                    }
                });
    }

    private void clearSelection() {
        for (int i = 0; i < dateContainer.getChildCount(); i++)
            dateContainer.getChildAt(i).setSelected(false);
    }
    private void scheduleReminder(long classStartTimeMillis) {

        long reminderTime = classStartTimeMillis - (10 * 60 * 1000); // 10 min before

        Intent intent = new Intent(requireContext(), ClassReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {

            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
            );
        }
    }
}