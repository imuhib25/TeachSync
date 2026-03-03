package com.intisarmuhib.teachsync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduleFragment extends Fragment {

    private LinearLayout dateContainer;
    private TextView tvSessionCount;
    private TextView tvCurrentMonth;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    private FirebaseFirestore db;
    private ListenerRegistration classListener;
    private ClassAdapter adapter;
    private String selectedDate;

    private static final int NOTIFY_BEFORE_MINUTES = 10;

    // ─────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        dateContainer  = view.findViewById(R.id.dateContainer);
        tvSessionCount = view.findViewById(R.id.tvSessionCount);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        recyclerView   = view.findViewById(R.id.classRecycler);
        fabAdd         = view.findViewById(R.id.fabAdd);

        db = FirebaseFirestore.getInstance();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassAdapter();
        recyclerView.setAdapter(adapter);

        // Red swipe-to-delete with icon
        ClassAdapter.attachSwipeToDelete(recyclerView, position -> {
            ClassModel deleted = adapter.getItem(position);
            if (deleted == null) return;
            adapter.removeItem(position);
            deleteClass(deleted);
        });

        generateDateChips();
        adapter.setListener(this::showClassBottomSheet);
        fabAdd.setOnClickListener(v -> showClassBottomSheet(null));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (classListener != null) { classListener.remove(); classListener = null; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // DATE CHIPS
    // ─────────────────────────────────────────────────────────────────────

    private void generateDateChips() {
        dateContainer.removeAllViews();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFmt   = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat fullFmt  = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat ddFmt    = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            View chip = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_date_chip, dateContainer, false);
            TextView tvDay  = chip.findViewById(R.id.tvDay);
            TextView tvDate = chip.findViewById(R.id.tvDate);
            Date date      = calendar.getTime();
            String fullStr = fullFmt.format(date);

            tvDay.setText(dayFmt.format(date));
            tvDate.setText(ddFmt.format(date));

            if (i == 0) {
                chip.setSelected(true);
                selectedDate = fullStr;
                tvCurrentMonth.setText(monthFmt.format(date));
                loadClasses(selectedDate);
            }

            chip.setOnClickListener(v -> {
                clearChipSelection();
                chip.setSelected(true);
                selectedDate = fullStr;
                tvCurrentMonth.setText(monthFmt.format(date));
                loadClasses(selectedDate);
            });

            dateContainer.addView(chip);
            calendar.add(Calendar.DATE, 1);
        }
    }

    private void clearChipSelection() {
        for (int i = 0; i < dateContainer.getChildCount(); i++)
            dateContainer.getChildAt(i).setSelected(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOAD CLASSES
    // ─────────────────────────────────────────────────────────────────────

    private void loadClasses(String date) {
        if (classListener != null) classListener.remove();
        String userId = DashboardFragment.userId;
        if (userId == null || userId.isEmpty()) return;

        classListener = db.collection("users").document(userId)
                .collection("classes")
                .whereEqualTo("date", date)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (error != null || value == null) return;
                    List<ClassModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ClassModel model = doc.toObject(ClassModel.class);
                        if (model != null) { model.setId(doc.getId()); list.add(model); }
                    }
                    adapter.setData(list);
                    tvSessionCount.setText(list.size() + " Sessions");
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADD / EDIT BOTTOM SHEET
    // ─────────────────────────────────────────────────────────────────────

    private void showClassBottomSheet(@Nullable ClassModel model) {
        if (!isAdded() || getContext() == null) return;
        boolean isEdit = model != null;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_add_class, null);
        dialog.setContentView(view);

        TextView tvSheetTitle          = view.findViewById(R.id.tvSheetTitle);
        TextView tvMonthLabel          = view.findViewById(R.id.tvMonthLabel);
        TextInputEditText etTopic      = view.findViewById(R.id.etTopic);
        AutoCompleteTextView dropBatch = view.findViewById(R.id.dropBatch);
        TextInputEditText etDate       = view.findViewById(R.id.etDate);
        CheckBox checkExtra            = view.findViewById(R.id.checkExtra);
        MaterialButton btnSave         = view.findViewById(R.id.btnSave);
        TextInputLayout layoutClassNum = view.findViewById(R.id.layoutClassNumber);
        TextInputEditText etClassNum   = view.findViewById(R.id.etClassNumber);

        tvSheetTitle.setText(isEdit ? "Edit Class" : "Add New Class");
        setMonthLabel(tvMonthLabel, selectedDate);

        if (isEdit) {
            etTopic.setText(model.getTopic());
            dropBatch.setText(model.getBatch(), false);
            etDate.setText(model.getDate());
            checkExtra.setChecked(model.isExtra());
            layoutClassNum.setVisibility(View.VISIBLE);
            etClassNum.setText(model.getMonthlyNumber());
        } else {
            layoutClassNum.setVisibility(View.GONE);
            etDate.setText(selectedDate);
        }

        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.show(getParentFragmentManager(), "DATE");
            picker.addOnPositiveButtonClickListener(selection -> {
                // UTC calendar avoids timezone date-shift in negative UTC offsets
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(selection);
                String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(cal.getTime());
                etDate.setText(formatted);
                setMonthLabel(tvMonthLabel, formatted);
            });
        });

        String userId = DashboardFragment.userId;
        if (userId == null) return;
        Map<String, String> batchMap = new HashMap<>();
        List<String> batchNames = new ArrayList<>();

        db.collection("users").document(userId).collection("batches").get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        if (name != null) { batchNames.add(name); batchMap.put(name, doc.getId()); }
                    }
                    dropBatch.setAdapter(new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, batchNames));
                    dropBatch.setOnClickListener(v -> dropBatch.showDropDown());
                });

        btnSave.setOnClickListener(v -> {
            String topic     = etTopic.getText() != null ? etTopic.getText().toString().trim() : "";
            String batchName = dropBatch.getText().toString().trim();
            String batchId   = batchMap.get(batchName);
            String date      = etDate.getText() != null ? etDate.getText().toString() : "";

            if (topic.isEmpty() || batchId == null || date.isEmpty()) {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Manual class number correction — edit mode only
            String manualNum = null;
            if (isEdit && layoutClassNum.getVisibility() == View.VISIBLE) {
                String raw = etClassNum.getText() != null
                        ? etClassNum.getText().toString().trim() : "";
                if (!raw.isEmpty()) manualNum = raw;
            }

            saveClass(isEdit, model, topic, batchName, batchId, date,
                    checkExtra.isChecked(), manualNum, dialog);
        });

        dialog.show();
    }

    private void setMonthLabel(TextView tv, String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return;
        try {
            Date d = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(dateStr);
            if (d != null)
                tv.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d));
        } catch (ParseException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // SAVE CLASS
    //
    // KEY FIX for "Class 0" bug:
    // We count actual non-extra documents already saved for this batch+cycle
    // instead of reading currentMonthCount from the batch doc. The batch doc
    // counter may be stale (0) if the batch was created before this field
    // existed, or if classes were added by the old code that never incremented
    // it. Counting real documents is always accurate.
    // currentMonthCount on the batch is then synced to match and kept updated.
    // ─────────────────────────────────────────────────────────────────────

    private void saveClass(boolean isEdit,
                           @Nullable ClassModel existingModel,
                           String topic, String batchName, String batchId,
                           String date, boolean isExtra,
                           @Nullable String manualClassNumber,
                           BottomSheetDialog dialog) {

        String userId = DashboardFragment.userId;
        if (userId == null) return;

        DocumentReference batchRef = db.collection("users").document(userId)
                .collection("batches").document(batchId);

        // Step 1: get batch metadata
        batchRef.get().addOnSuccessListener(batchSnap -> {
            if (!isAdded() || !batchSnap.exists()) return;

            Long totalLong = batchSnap.getLong("totalMonthlyClasses");
            int total      = totalLong != null ? totalLong.intValue() : 0;

            Long cycleLong = batchSnap.getLong("cycleCount");
            int cycleCount = cycleLong != null ? cycleLong.intValue() : 1;

            // Step 2: count actual saved non-extra docs for this batch+cycle
            // This is the source of truth — immune to stale counters
            db.collection("users").document(userId)
                    .collection("classes")
                    .whereEqualTo("batchId",     batchId)
                    .whereEqualTo("cycleNumber", cycleCount)
                    .whereEqualTo("extra",       false)
                    .get()
                    .addOnSuccessListener(existingSnap -> {
                        if (!isAdded()) return;

                        int taken = existingSnap.size();

                        // Heal stale counter on batch doc if needed
                        Long storedTaken = batchSnap.getLong("currentMonthCount");
                        if (storedTaken == null || storedTaken.intValue() != taken) {
                            batchRef.update("currentMonthCount", taken);
                        }

                        // Block full cycle (non-extra new class only)
                        if (!isEdit && !isExtra && total > 0 && taken >= total) {
                            Snackbar.make(recyclerView,
                                            "Cycle complete! Reset to add more classes.",
                                            Snackbar.LENGTH_LONG)
                                    .setAction("RESET", v -> resetCycle(batchRef))
                                    .show();
                            return;
                        }

                        // Determine class number
                        // taken = actual docs already on disk for this cycle
                        // First class: taken=0 → "1". Second: taken=1 → "2". Etc.
                        final String classNumber;
                        if (isExtra) {
                            classNumber = "Extra";
                        } else if (manualClassNumber != null) {
                            classNumber = manualClassNumber;
                        } else if (isEdit && existingModel != null) {
                            classNumber = existingModel.getMonthlyNumber();
                        } else {
                            classNumber = String.valueOf(taken + 1);
                        }

                        // Build time string from batch timestamps
                        Timestamp startTs = batchSnap.getTimestamp("startTime");
                        Timestamp endTs   = batchSnap.getTimestamp("endTime");
                        final String timeText = (startTs != null && endTs != null)
                                ? new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                .format(startTs.toDate())
                                + " - "
                                + new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                .format(endTs.toDate())
                                : "";

                        Map<String, Object> data = new HashMap<>();
                        data.put("topic",         topic);
                        data.put("batch",         batchName);
                        data.put("batchId",       batchId);
                        data.put("date",          date);
                        data.put("extra",         isExtra);
                        data.put("classTime",     timeText);
                        data.put("createdAt",     Timestamp.now());
                        data.put("monthlyNumber", classNumber);  // always String now
                        data.put("cycleNumber",   cycleCount);
                        data.put("totalInCycle",  total);

                        DocumentReference classRef = isEdit && existingModel != null
                                ? db.collection("users").document(userId)
                                .collection("classes").document(existingModel.getId())
                                : db.collection("users").document(userId)
                                .collection("classes").document();

                        classRef.set(data)
                                .addOnSuccessListener(aVoid -> {
                                    if (!isAdded()) return;

                                    // Increment batch counter for new non-extra class
                                    if (!isEdit && !isExtra) {
                                        int newTaken = taken + 1;
                                        batchRef.update("currentMonthCount", newTaken);

                                        if (total > 0 && newTaken >= total) {
                                            Snackbar.make(recyclerView,
                                                            "Cycle complete! "
                                                                    + newTaken + "/" + total + " classes done.",
                                                            Snackbar.LENGTH_LONG)
                                                    .setAction("RESET", v -> resetCycle(batchRef))
                                                    .show();
                                        }
                                    }

                                    dialog.dismiss();
                                    Snackbar.make(recyclerView, "Class saved",
                                            Snackbar.LENGTH_SHORT).show();
                                    scheduleNotification(classRef.getId(), topic,
                                            batchName, timeText, date);
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Snackbar.make(recyclerView, "Failed to save class",
                                            Snackbar.LENGTH_SHORT).show();
                                });
                    });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // CYCLE RESET
    // ─────────────────────────────────────────────────────────────────────

    private void resetCycle(DocumentReference batchRef) {
        batchRef.get().addOnSuccessListener(snap -> {
            Long cycleLong = snap.getLong("cycleCount");
            int newCycle   = (cycleLong != null ? cycleLong.intValue() : 1) + 1;
            Map<String, Object> update = new HashMap<>();
            update.put("currentMonthCount", 0);
            update.put("cycleCount", newCycle);
            batchRef.update(update).addOnSuccessListener(v -> {
                if (!isAdded()) return;
                Snackbar.make(recyclerView, "Cycle reset! Starting cycle " + newCycle,
                        Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELETE CLASS
    // ─────────────────────────────────────────────────────────────────────

    private void deleteClass(ClassModel deleted) {
        String userId = DashboardFragment.userId;
        if (userId == null) return;

        db.collection("users").document(userId)
                .collection("classes").document(deleted.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    if (!deleted.isExtra()) {
                        renumberCycle(userId, deleted.getBatchId(), deleted.getCycleNumber());
                        decrementBatchCount(userId, deleted.getBatchId());
                    }
                    cancelNotification(deleted.getId());
                    Snackbar.make(recyclerView, "Class deleted", Snackbar.LENGTH_LONG).show();
                });
    }

    private void renumberCycle(String userId, String batchId, int cycleNumber) {
        db.collection("users").document(userId).collection("classes")
                .whereEqualTo("batchId",     batchId)
                .whereEqualTo("cycleNumber", cycleNumber)
                .whereEqualTo("extra",       false)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 1;
                    for (DocumentSnapshot doc : snapshot.getDocuments())
                        doc.getReference().update("monthlyNumber", String.valueOf(count++));
                });
    }

    private void decrementBatchCount(String userId, String batchId) {
        DocumentReference batchRef = db.collection("users").document(userId)
                .collection("batches").document(batchId);
        batchRef.get().addOnSuccessListener(snap -> {
            Long taken = snap.getLong("currentMonthCount");
            batchRef.update("currentMonthCount",
                    taken != null ? Math.max(0, taken.intValue() - 1) : 0);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUSH NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────────────

    private void scheduleNotification(String classId, String topic,
                                      String batchName, String timeText, String date) {
        if (getContext() == null) return;
        long triggerAt = parseClassStartMillis(date, timeText);
        if (triggerAt <= 0) return;
        long alarmAt = triggerAt - (NOTIFY_BEFORE_MINUTES * 60 * 1000L);
        if (alarmAt <= System.currentTimeMillis()) return;

        Intent intent = new Intent(getContext(), ClassReminderReceiver.class);
        intent.putExtra("title",          batchName + " class starting soon");
        intent.putExtra("message",        topic + " — in " + NOTIFY_BEFORE_MINUTES + " minutes");
        intent.putExtra("notificationId", classId.hashCode());

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pi = PendingIntent.getBroadcast(
                getContext(), classId.hashCode(), intent, flags);
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms())
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAt, pi);
                else
                    am.set(AlarmManager.RTC_WAKEUP, alarmAt, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, alarmAt, pi);
            }
        } catch (SecurityException e) {
            android.util.Log.w("ScheduleFragment", "Alarm denied: " + e.getMessage());
        }
    }

    private void cancelNotification(String classId) {
        if (getContext() == null) return;
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), classId.hashCode(),
                new Intent(getContext(), ClassReminderReceiver.class), flags);
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }

    private long parseClassStartMillis(String date, String timeText) {
        try {
            if (date == null || timeText == null) return -1;
            String[] parts = timeText.split(" - ");
            if (parts.length < 1) return -1;
            Date parsed = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
                    .parse(date + " " + parts[0].trim());
            return parsed != null ? parsed.getTime() : -1;
        } catch (Exception e) { return -1; }
    }
}