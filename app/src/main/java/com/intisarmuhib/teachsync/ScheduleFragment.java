package com.intisarmuhib.teachsync;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    // ── Views ─────────────────────────────────────────────────────────────
    private LinearLayout dateContainer;
    private TextView tvSessionCount;
    private TextView tvCurrentMonth;       // NEW: month name header
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    // ── Firebase ──────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private ListenerRegistration classListener;

    // ── State ─────────────────────────────────────────────────────────────
    private ClassAdapter adapter;
    private String selectedDate;

    // ═════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════

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

        // Red swipe-to-delete with delete icon (using shared helper in ClassAdapter)
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
        if (classListener != null) {
            classListener.remove();
            classListener = null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // DATE CHIPS  — shows month name above strip
    // ═════════════════════════════════════════════════════════════════════

    private void generateDateChips() {
        dateContainer.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFmt  = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat ddFmt   = new SimpleDateFormat("dd", Locale.getDefault());
        // FEATURE: month name header
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
                // Set month header for first chip
                tvCurrentMonth.setText(monthFmt.format(date));
                loadClasses(selectedDate);
            }

            chip.setOnClickListener(v -> {
                clearChipSelection();
                chip.setSelected(true);
                selectedDate = fullStr;
                // Update month header on chip tap
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

    // ═════════════════════════════════════════════════════════════════════
    // LOAD CLASSES  — live Firestore listener
    // ═════════════════════════════════════════════════════════════════════

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
                        if (model != null) {
                            model.setId(doc.getId());
                            list.add(model);
                        }
                    }
                    adapter.setData(list);
                    tvSessionCount.setText(list.size() + " Sessions");
                });
    }

    // ═════════════════════════════════════════════════════════════════════
    // ADD / EDIT BOTTOM SHEET
    // ═════════════════════════════════════════════════════════════════════

    private void showClassBottomSheet(@Nullable ClassModel model) {
        if (!isAdded() || getContext() == null) return;

        boolean isEdit = model != null;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_add_class, null);
        dialog.setContentView(view);

        // ── Views ──────────────────────────────────────────────────────
        TextView tvSheetTitle      = view.findViewById(R.id.tvSheetTitle);
        TextView tvMonthLabel      = view.findViewById(R.id.tvMonthLabel);
        TextInputEditText etTopic  = view.findViewById(R.id.etTopic);
        AutoCompleteTextView dropBatch = view.findViewById(R.id.dropBatch);
        TextInputEditText etDate   = view.findViewById(R.id.etDate);
        CheckBox checkExtra        = view.findViewById(R.id.checkExtra);
        MaterialButton btnSave     = view.findViewById(R.id.btnSave);
        TextInputLayout layoutClassNumber = view.findViewById(R.id.layoutClassNumber);
        TextInputEditText etClassNumber   = view.findViewById(R.id.etClassNumber);

        // ── Sheet title ────────────────────────────────────────────────
        tvSheetTitle.setText(isEdit ? "Edit Class" : "Add New Class");

        // ── Month label from selected date ─────────────────────────────
        // FEATURE: show "March 2025" in the sheet header
        if (selectedDate != null && !selectedDate.isEmpty()) {
            try {
                Date d = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .parse(selectedDate);
                if (d != null) {
                    tvMonthLabel.setText(
                            new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d));
                }
            } catch (ParseException ignored) {}
        }

        // ── Pre-fill for edit ──────────────────────────────────────────
        if (isEdit) {
            etTopic.setText(model.getTopic());
            dropBatch.setText(model.getBatch(), false);
            etDate.setText(model.getDate());
            checkExtra.setChecked(model.isExtra());

            // FEATURE: Manual class number correction — only visible in edit mode
            layoutClassNumber.setVisibility(View.VISIBLE);
            etClassNumber.setText(model.getMonthlyNumber() != null
                    ? model.getMonthlyNumber() : "");
        } else {
            layoutClassNumber.setVisibility(View.GONE);
            etDate.setText(selectedDate);
        }

        // ── Date picker ────────────────────────────────────────────────
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.show(getParentFragmentManager(), "DATE");
            picker.addOnPositiveButtonClickListener(selection -> {
                // Use UTC calendar to avoid timezone date-shift
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(selection);
                String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(cal.getTime());
                etDate.setText(formatted);
                // Update month label as user picks a different date
                tvMonthLabel.setText(
                        new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                .format(cal.getTime()));
            });
        });

        // ── Batch dropdown ─────────────────────────────────────────────
        String userId = DashboardFragment.userId;
        if (userId == null) return;

        Map<String, String> batchMap = new HashMap<>();
        List<String> batchNames = new ArrayList<>();

        db.collection("users").document(userId).collection("batches")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            batchNames.add(name);
                            batchMap.put(name, doc.getId());
                        }
                    }
                    dropBatch.setAdapter(new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, batchNames));
                    dropBatch.setOnClickListener(v -> dropBatch.showDropDown());
                });

        // ── Save ───────────────────────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            String topic     = etTopic.getText() != null ? etTopic.getText().toString().trim() : "";
            String batchName = dropBatch.getText().toString().trim();
            String batchId   = batchMap.get(batchName);
            String date      = etDate.getText() != null ? etDate.getText().toString() : "";

            if (topic.isEmpty() || batchId == null || date.isEmpty()) {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Manual class number override (edit mode only)
            String manualClassNumber = null;
            if (isEdit && layoutClassNumber.getVisibility() == View.VISIBLE) {
                String raw = etClassNumber.getText() != null
                        ? etClassNumber.getText().toString().trim() : "";
                if (!raw.isEmpty()) manualClassNumber = raw;
            }

            saveClass(isEdit, model, topic, batchName, batchId, date,
                    checkExtra.isChecked(), manualClassNumber, dialog);
        });

        dialog.show();
    }

    // ═════════════════════════════════════════════════════════════════════
    // SAVE CLASS  — cycle-based counter, extra class handling
    // ═════════════════════════════════════════════════════════════════════

    private void saveClass(boolean isEdit,
                           @Nullable ClassModel existingModel,
                           String topic,
                           String batchName,
                           String batchId,
                           String date,
                           boolean isExtra,
                           @Nullable String manualClassNumber,
                           BottomSheetDialog dialog) {

        String userId = DashboardFragment.userId;
        if (userId == null) return;

        DocumentReference batchRef = db.collection("users").document(userId)
                .collection("batches").document(batchId);

        batchRef.get().addOnSuccessListener(batchSnap -> {
            if (!isAdded() || !batchSnap.exists()) return;

            Long totalLong = batchSnap.getLong("totalMonthlyClasses");
            int total      = totalLong != null ? totalLong.intValue() : 0;

            Long takenLong = batchSnap.getLong("currentMonthCount");
            int taken      = takenLong != null ? takenLong.intValue() : 0;

            Long cycleLong = batchSnap.getLong("cycleCount");
            int cycleCount = cycleLong != null ? cycleLong.intValue() : 1;

            // ── Block if cycle full (non-extra, new class only) ────────
            // FEATURE: reset after completing total (not monthly reset)
            if (!isEdit && !isExtra && taken >= total) {
                Snackbar.make(recyclerView,
                                "Cycle complete! Reset or start new cycle.",
                                Snackbar.LENGTH_LONG)
                        .setAction("RESET", v -> resetCycle(batchId, batchRef, total))
                        .show();
                return;
            }

            // ── Determine class number ─────────────────────────────────
            // FEATURE: extra class doesn't increment number
            String classNumber;
            if (isExtra) {
                classNumber = "Extra";
            } else if (manualClassNumber != null) {
                // FEATURE: manual correction overrides
                classNumber = manualClassNumber;
            } else if (isEdit && existingModel != null) {
                classNumber = existingModel.getMonthlyNumber();
            } else {
                classNumber = String.valueOf(taken + 1);
            }

            // ── Build time text from batch ─────────────────────────────
            // Assigned once via ternary so it is effectively final for use in lambda below
            Timestamp startTs = batchSnap.getTimestamp("startTime");
            Timestamp endTs   = batchSnap.getTimestamp("endTime");
            final String timeText = (startTs != null && endTs != null)
                    ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(startTs.toDate())
                    + " - "
                    + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(endTs.toDate())
                    : "";

            // ── Build Firestore data map ───────────────────────────────
            Map<String, Object> data = new HashMap<>();
            data.put("topic",         topic);
            data.put("batch",         batchName);
            data.put("batchId",       batchId);
            data.put("date",          date);
            data.put("extra",         isExtra);
            data.put("classTime",     timeText);
            data.put("createdAt",     Timestamp.now());
            data.put("monthlyNumber", classNumber);
            data.put("cycleNumber",   cycleCount);
            data.put("totalInCycle",  total);

            DocumentReference classRef = isEdit && existingModel != null
                    ? db.collection("users").document(userId)
                    .collection("classes").document(existingModel.getId())
                    : db.collection("users").document(userId)
                    .collection("classes").document();

            classRef.set(data).addOnSuccessListener(aVoid -> {
                if (!isAdded()) return;

                // ── Update batch counter (only for non-extra, new class) ─
                if (!isEdit && !isExtra) {
                    int newTaken = taken + 1;
                    Map<String, Object> batchUpdate = new HashMap<>();
                    batchUpdate.put("currentMonthCount", newTaken);

                    // FEATURE: auto-reset if just completed the cycle
                    if (newTaken >= total) {
                        Snackbar.make(recyclerView,
                                        "🎉 Cycle complete! " + total + "/" + total + " classes done.",
                                        Snackbar.LENGTH_LONG)
                                .setAction("RESET NOW", v ->
                                        resetCycle(batchId, batchRef, total))
                                .show();
                    }
                    batchRef.update(batchUpdate);
                }

                dialog.dismiss();
                Snackbar.make(recyclerView, "Class saved", Snackbar.LENGTH_SHORT).show();

                // ── Schedule pre-class push notification ─────────────────
                scheduleNotification(classRef.getId(), topic, batchName, timeText, date);

            }).addOnFailureListener(e -> {
                if (!isAdded()) return;
                Snackbar.make(recyclerView, "Failed to save class", Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════════
    // CYCLE RESET  — resets currentMonthCount, increments cycleCount
    // ═════════════════════════════════════════════════════════════════════

    private void resetCycle(String batchId, DocumentReference batchRef, int total) {
        String userId = DashboardFragment.userId;
        if (userId == null) return;

        batchRef.get().addOnSuccessListener(snap -> {
            Long cycleLong = snap.getLong("cycleCount");
            int newCycle   = (cycleLong != null ? cycleLong.intValue() : 1) + 1;

            Map<String, Object> update = new HashMap<>();
            update.put("currentMonthCount", 0);
            update.put("cycleCount", newCycle);

            batchRef.update(update).addOnSuccessListener(v -> {
                if (!isAdded()) return;
                Snackbar.make(recyclerView,
                        "Cycle reset! Starting cycle " + newCycle,
                        Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════════
    // DELETE CLASS  — removes from Firestore, renumbers, decrements batch
    // ═════════════════════════════════════════════════════════════════════

    private void deleteClass(ClassModel deleted) {
        String userId = DashboardFragment.userId;
        if (userId == null) return;

        // Derive cycleNumber to renumber correctly
        int cycleNumber = deleted.getCycleNumber();

        db.collection("users").document(userId)
                .collection("classes").document(deleted.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;

                    // Renumber remaining classes in same cycle
                    if (!deleted.isExtra()) {
                        renumberCycle(deleted.getBatchId(), cycleNumber);
                        decrementBatchCount(deleted.getBatchId());
                    }

                    // Cancel scheduled notification
                    cancelNotification(deleted.getId());

                    Snackbar.make(recyclerView, "Class deleted", Snackbar.LENGTH_LONG).show();
                });
    }

    private void renumberCycle(String batchId, int cycleNumber) {
        String userId = DashboardFragment.userId;
        if (userId == null) return;

        db.collection("users").document(userId)
                .collection("classes")
                .whereEqualTo("batchId", batchId)
                .whereEqualTo("cycleNumber", cycleNumber)
                .whereEqualTo("extra", false)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 1;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().update("monthlyNumber", String.valueOf(count));
                        count++;
                    }
                });
    }

    private void decrementBatchCount(String batchId) {
        String userId = DashboardFragment.userId;
        if (userId == null) return;

        DocumentReference batchRef = db.collection("users").document(userId)
                .collection("batches").document(batchId);

        batchRef.get().addOnSuccessListener(snap -> {
            Long taken = snap.getLong("currentMonthCount");
            int newVal = taken != null ? Math.max(0, taken.intValue() - 1) : 0;
            batchRef.update("currentMonthCount", newVal);
        });
    }

    // ═════════════════════════════════════════════════════════════════════
    // PUSH NOTIFICATION SCHEDULING
    // FEATURE: schedule alarm X minutes before classTime on classDate
    // ═════════════════════════════════════════════════════════════════════

    private static final int NOTIFY_BEFORE_MINUTES = 10;

    private void scheduleNotification(String classId, String topic,
                                      String batchName, String timeText, String date) {
        if (getContext() == null) return;

        // Parse the class start time and date into an exact millisecond timestamp
        long triggerAtMillis = parseClassStartMillis(date, timeText);
        if (triggerAtMillis <= 0) return;

        // Subtract notify window
        long alarmMillis = triggerAtMillis - (NOTIFY_BEFORE_MINUTES * 60 * 1000L);
        if (alarmMillis <= System.currentTimeMillis()) return; // already past

        Intent intent = new Intent(getContext(), ClassReminderReceiver.class);
        intent.putExtra("title",          batchName + " class starting soon");
        intent.putExtra("message",        topic + " — in " + NOTIFY_BEFORE_MINUTES + " minutes");
        intent.putExtra("notificationId", classId.hashCode());

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), classId.hashCode(), intent, flags);

        AlarmManager alarmManager =
                (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires SCHEDULE_EXACT_ALARM permission
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
                } else {
                    // Fallback to inexact alarm if exact not permitted
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            android.util.Log.w("ScheduleFragment", "Cannot schedule exact alarm: " + e.getMessage());
        }
    }

    private void cancelNotification(String classId) {
        if (getContext() == null) return;

        Intent intent = new Intent(getContext(), ClassReminderReceiver.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), classId.hashCode(), intent, flags);

        AlarmManager alarmManager =
                (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * Parses "dd MMM yyyy" + "hh:mm a - hh:mm a" into epoch millis for class start.
     * Returns -1 if parsing fails.
     */
    private long parseClassStartMillis(String date, String timeText) {
        try {
            if (date == null || timeText == null) return -1;
            String[] parts = timeText.split(" - ");
            if (parts.length < 1) return -1;

            String startTimeStr = parts[0].trim();
            String dateTimeStr  = date + " " + startTimeStr;

            SimpleDateFormat fmt = new SimpleDateFormat(
                    "dd MMM yyyy hh:mm a", Locale.getDefault());
            Date parsed = fmt.parse(dateTimeStr);
            return parsed != null ? parsed.getTime() : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}