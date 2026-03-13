package com.intisarmuhib.teachsync;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduleFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────
    private LinearLayout dateContainer;
    private TextView tvSessionCount;
    private TextView tvCurrentMonth;
    private TextView tvClassHeader;
    private RecyclerView recyclerView;
    private HorizontalScrollView dateScrollView;
    private TextView btnClearData;

    // ── Firebase ──────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private ListenerRegistration classListener;

    // ── State ─────────────────────────────────────────────────────────────
    private ClassAdapter adapter;
    private String selectedDate;
    private Calendar currentCalendar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        dateContainer  = view.findViewById(R.id.dateContainer);
        tvSessionCount = view.findViewById(R.id.tvSessionCount);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        tvClassHeader  = view.findViewById(R.id.tvClassHeader);
        recyclerView   = view.findViewById(R.id.classRecycler);
        dateScrollView = view.findViewById(R.id.dateScrollView);
        btnClearData   = view.findViewById(R.id.btnClearData);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        ImageButton btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = view.findViewById(R.id.btnNextMonth);

        db = FirebaseFirestore.getInstance();
        currentCalendar = Calendar.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassAdapter();
        recyclerView.setAdapter(adapter);

        ClassAdapter.attachSwipeToDelete(recyclerView, position -> {
            ClassModel deleted = adapter.getItem(position);
            if (deleted == null) return;
            adapter.removeItem(position);
            deleteClassWithUndo(deleted, position);
        });

        generateDateChips();
        adapter.setListener(this::showClassBottomSheet);
        fabAdd.setOnClickListener(v -> showClassBottomSheet(null));

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            generateDateChips();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            generateDateChips();
        });

        btnClearData.setOnClickListener(v -> showClearDataConfirmation());

        return view;
    }

    private void showClearDataConfirmation() {
        if (selectedDate == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Clear Data")
                .setMessage("Are you sure you want to delete all classes for " + selectedDate + "?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllClassesForSelectedDate())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllClassesForSelectedDate() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null || selectedDate == null) return;

        db.collection("users").document(userId)
                .collection("classes")
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ClassModel model = doc.toObject(ClassModel.class);
                        if (model != null) {
                            if (!model.isExtra()) {
                                decrementBatchCount(model.getBatchId());
                            }
                            cancelNotifications(doc.getId());
                        }
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Cleared all classes for " + selectedDate, Toast.LENGTH_SHORT).show();
                    });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (classListener != null) {
            classListener.remove();
            classListener = null;
        }
    }

    private void generateDateChips() {
        if (!isAdded() || getContext() == null) return;
        dateContainer.removeAllViews();
        
        Calendar tempCal = (Calendar) currentCalendar.clone();
        Calendar today = Calendar.getInstance();
        
        SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String todayStr = fullFmt.format(today.getTime());

        // Move to the first day of the selected month
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat dayFmt  = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat ddFmt   = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        tvCurrentMonth.setText(monthFmt.format(tempCal.getTime()));

        View selectedChip = null;
        boolean isCurrentMonth = tempCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) && 
                                tempCal.get(Calendar.YEAR) == today.get(Calendar.YEAR);

        for (int i = 0; i < daysInMonth; i++) {
            View chip = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_date_chip, dateContainer, false);

            TextView tvDay  = chip.findViewById(R.id.tvDay);
            TextView tvDate = chip.findViewById(R.id.tvDate);

            Date date      = tempCal.getTime();
            String fullStr = fullFmt.format(date);

            tvDay.setText(dayFmt.format(date));
            tvDate.setText(ddFmt.format(date));
            chip.setTag(fullStr);

            // Logic to determine which date to select initially
            boolean shouldSelect = false;
            if (isCurrentMonth) {
                if (fullStr.equals(todayStr)) {
                    shouldSelect = true;
                }
            } else if (i == 0) {
                // Select 1st day of the month if it's not the current month
                shouldSelect = true;
            }

            if (shouldSelect) {
                chip.setSelected(true);
                selectedDate = fullStr;
                updateClassHeader(date);
                loadClasses(selectedDate);
                selectedChip = chip;
            }

            chip.setOnClickListener(v -> {
                clearChipSelection();
                chip.setSelected(true);
                selectedDate = fullStr;
                updateClassHeader(date);
                loadClasses(selectedDate);
            });

            dateContainer.addView(chip);
            tempCal.add(Calendar.DATE, 1);
        }

        // Auto-scroll to selected chip
        if (selectedChip != null) {
            final View finalSelectedChip = selectedChip;
            dateContainer.post(() -> {
                if (dateScrollView != null) {
                    int scrollX = finalSelectedChip.getLeft() - (dateScrollView.getWidth() / 2) + (finalSelectedChip.getWidth() / 2);
                    dateScrollView.smoothScrollTo(scrollX, 0);
                }
            });
        }
    }

    private void updateClassHeader(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            tvClassHeader.setText("Today's Classes");
        } else {
            String datePart = new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(date);
            tvClassHeader.setText(datePart + "'s Classes");
        }
    }

    private void clearChipSelection() {
        for (int i = 0; i < dateContainer.getChildCount(); i++)
            dateContainer.getChildAt(i).setSelected(false);
    }

    private void loadClasses(String date) {
        if (classListener != null) classListener.remove();
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        classListener = db.collection("users").document(userId)
                .collection("classes")
                .whereEqualTo("date", date)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        Log.e("ScheduleFragment", "Firestore error: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    List<ClassModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ClassModel model = doc.toObject(ClassModel.class);
                        if (model != null) {
                            model.setId(doc.getId());
                            list.add(model);
                        }
                    }

                    Collections.sort(list, (o1, o2) -> {
                        if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                        return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                    });

                    adapter.setData(list);
                    tvSessionCount.setText(list.size() + " Sessions");
                });
    }

    private void showClassBottomSheet(@Nullable ClassModel model) {
        if (!isAdded() || getContext() == null) return;
        boolean isEdit = model != null;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_add_class, null);
        dialog.setContentView(view);

        TextView tvSheetTitle      = view.findViewById(R.id.tvSheetTitle);
        TextView tvMonthLabel      = view.findViewById(R.id.tvMonthLabel);
        TextInputEditText etTopic  = view.findViewById(R.id.etTopic);
        AutoCompleteTextView dropBatch = view.findViewById(R.id.dropBatch);
        TextInputEditText etDate   = view.findViewById(R.id.etDate);
        CheckBox checkExtra        = view.findViewById(R.id.checkExtra);
        MaterialButton btnSave     = view.findViewById(R.id.btnSave);
        TextInputLayout layoutClassNumber = view.findViewById(R.id.layoutClassNumber);
        TextInputEditText etClassNumber   = view.findViewById(R.id.etClassNumber);

        tvSheetTitle.setText(isEdit ? "Edit Class" : "Add New Class");

        if (selectedDate != null) {
            try {
                Date d = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(selectedDate);
                if (d != null) tvMonthLabel.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d));
            } catch (Exception ignored) {}
        }

        if (isEdit) {
            etTopic.setText(model.getTopic());
            dropBatch.setText(model.getBatch(), false);
            etDate.setText(model.getDate());
            checkExtra.setChecked(model.isExtra());
            layoutClassNumber.setVisibility(View.VISIBLE);
            etClassNumber.setText(model.getMonthlyNumber());
        } else {
            layoutClassNumber.setVisibility(View.GONE);
            etDate.setText(selectedDate);
        }

        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.show(getParentFragmentManager(), "DATE");
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(selection);
                String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
                etDate.setText(formatted);
                tvMonthLabel.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime()));
            });
        });

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        Map<String, String> batchMap = new HashMap<>();
        List<String> batchNames = new ArrayList<>();
        db.collection("users").document(userId).collection("batches").get().addOnSuccessListener(snapshot -> {
            if (!isAdded()) return;
            for (DocumentSnapshot doc : snapshot) {
                String name = doc.getString("name");
                if (name != null) {
                    batchNames.add(name);
                    batchMap.put(name, doc.getId());
                }
            }
            dropBatch.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, batchNames));
            dropBatch.setOnClickListener(v -> dropBatch.showDropDown());
        });

        btnSave.setOnClickListener(v -> {
            String topic = etTopic.getText() != null ? etTopic.getText().toString().trim() : "";
            String batchName = dropBatch.getText().toString().trim();
            String batchId = batchMap.get(batchName);
            String date = etDate.getText() != null ? etDate.getText().toString() : "";

            if (topic.isEmpty() || batchId == null || date.isEmpty()) {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String manualClassNumber = isEdit ? (etClassNumber.getText() != null ? etClassNumber.getText().toString().trim() : null) : null;
            saveClass(isEdit, model, topic, batchName, batchId, date, checkExtra.isChecked(), manualClassNumber, dialog);
        });

        dialog.show();
    }

    private void saveClass(boolean isEdit, @Nullable ClassModel existingModel, String topic, String batchName, String batchId, String date, boolean isExtra, @Nullable String manualClassNumber, BottomSheetDialog dialog) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        DocumentReference batchRef = db.collection("users").document(userId).collection("batches").document(batchId);
        batchRef.get().addOnSuccessListener(batchSnap -> {
            if (!isAdded() || !batchSnap.exists()) return;

            int total = batchSnap.getLong("totalMonthlyClasses") != null ? batchSnap.getLong("totalMonthlyClasses").intValue() : 0;
            int taken = batchSnap.getLong("currentMonthCount") != null ? batchSnap.getLong("currentMonthCount").intValue() : 0;
            int cycleCount = batchSnap.getLong("cycleCount") != null ? batchSnap.getLong("cycleCount").intValue() : 1;

            if (!isEdit && !isExtra && taken >= total) {
                Snackbar.make(recyclerView, "Cycle complete!", Snackbar.LENGTH_LONG).setAction("RESET", v -> resetCycle(batchId, batchRef)).show();
                return;
            }

            String classNumber = isExtra ? "Extra" : (manualClassNumber != null ? manualClassNumber : (isEdit ? existingModel.getMonthlyNumber() : String.valueOf(taken + 1)));

            Timestamp startTs = batchSnap.getTimestamp("startTime");
            Timestamp endTs = batchSnap.getTimestamp("endTime");
            
            final String finalTimeText;
            if (startTs != null && endTs != null) {
                SimpleDateFormat fmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                finalTimeText = fmt.format(startTs.toDate()) + " - " + fmt.format(endTs.toDate());
            } else {
                finalTimeText = "";
            }

            if (finalTimeText.isEmpty()) {
                executeSave(isEdit, existingModel, topic, batchName, batchId, date, isExtra, classNumber, finalTimeText, cycleCount, total, taken, batchRef, dialog);
                return;
            }

            // Conflict check
            db.collection("users").document(userId).collection("classes")
                    .whereEqualTo("date", date)
                    .whereEqualTo("classTime", finalTimeText)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!isAdded()) return;
                        for (DocumentSnapshot doc : querySnapshot) {
                            if (isEdit && existingModel != null && doc.getId().equals(existingModel.getId())) continue;
                            
                            String conflictBatch = doc.getString("batch");
                            Toast.makeText(getContext(), "Already " + conflictBatch + " is added on " + finalTimeText, Toast.LENGTH_LONG).show();
                            return;
                        }

                        executeSave(isEdit, existingModel, topic, batchName, batchId, date, isExtra, classNumber, finalTimeText, cycleCount, total, taken, batchRef, dialog);
                    });
        });
    }

    private void executeSave(boolean isEdit, @Nullable ClassModel existingModel, String topic, String batchName, String batchId, String date, boolean isExtra, String classNumber, String finalTimeText, int cycleCount, int total, int currentTaken, DocumentReference batchRef, BottomSheetDialog dialog) {
        String userId = FirebaseAuth.getInstance().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("topic", topic);
        data.put("batch", batchName);
        data.put("batchId", batchId);
        data.put("date", date);
        data.put("extra", isExtra);
        data.put("classTime", finalTimeText);
        data.put("createdAt", isEdit && existingModel != null ? existingModel.getCreatedAt() : Timestamp.now());
        data.put("monthlyNumber", classNumber);
        data.put("cycleNumber",   cycleCount);
        data.put("totalInCycle",  total);

        DocumentReference classRef = (isEdit && existingModel != null) ? db.collection("users").document(userId).collection("classes").document(existingModel.getId()) : db.collection("users").document(userId).collection("classes").document();

        classRef.set(data).addOnSuccessListener(aVoid -> {
            if (!isAdded()) return;
            if (!isEdit && !isExtra) {
                batchRef.update("currentMonthCount", currentTaken + 1);
            }
            dialog.dismiss();
            Snackbar.make(recyclerView, "Class saved", Snackbar.LENGTH_SHORT).show();

            if (!date.equals(selectedDate)) {
                selectedDate = date;
                // Try to find the chip for the new date and select it
                for (int i = 0; i < dateContainer.getChildCount(); i++) {
                    View chip = dateContainer.getChildAt(i);
                    if (date.equals(chip.getTag())) {
                        clearChipSelection();
                        chip.setSelected(true);
                        try {
                            Date d = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(date);
                            if (d != null) updateClassHeader(d);
                        } catch (Exception ignored) {}
                        break;
                    }
                }
                loadClasses(selectedDate);
            }
            scheduleClassNotifications(classRef.getId(), topic, batchName, finalTimeText, date);
        });
    }

    private void resetCycle(String batchId, DocumentReference batchRef) {
        batchRef.get().addOnSuccessListener(snap -> {
            BatchModel batch = snap.toObject(BatchModel.class);
            if (batch == null) return;
            
            int newCycle = batch.getCycleCount() + 1;
            Map<String, Object> update = new HashMap<>();
            update.put("currentMonthCount", 0);
            update.put("cycleCount", newCycle);
            batchRef.update(update).addOnSuccessListener(v -> {
                Snackbar.make(recyclerView, "Cycle reset!", Snackbar.LENGTH_SHORT).show();
                if (batch.isAutoSchedule()) {
                    autoScheduleNextCycle(batch, newCycle);
                }
            });
        });
    }

    private void autoScheduleNextCycle(BatchModel batch, int newCycle) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        WriteBatch writeBatch = db.batch();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeRange = timeFmt.format(batch.getStartTime().toDate()) + " - " + timeFmt.format(batch.getEndTime().toDate());

        List<Integer> selectedDays = batch.getSelectedDays();
        if (selectedDays == null || selectedDays.isEmpty()) return;

        int classesAdded = 0;
        for (int i = 0; i < 60 && classesAdded < batch.getTotalMonthlyClasses(); i++) {
            if (selectedDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                classesAdded++;
                String classId = db.collection("users").document(userId).collection("classes").document().getId();
                ClassModel classModel = new ClassModel(
                        classId,
                        "Initial Class",
                        batch.getName(),
                        batch.getId(),
                        timeRange,
                        dateFormat.format(cal.getTime()),
                        String.valueOf(classesAdded),
                        false,
                        newCycle,
                        batch.getTotalMonthlyClasses(),
                        new Timestamp(Calendar.getInstance().getTime())
                );
                writeBatch.set(db.collection("users").document(userId).collection("classes").document(classId), classModel);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        final int finalClassesAdded = classesAdded;
        writeBatch.commit().addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                Toast.makeText(getContext(), finalClassesAdded + " classes scheduled for new cycle", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteClassWithUndo(ClassModel deleted, int position) {
        Snackbar.make(recyclerView, "Class deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    adapter.addItem(position, deleted);
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            performFinalDelete(deleted);
                        }
                    }
                })
                .show();
    }

    private void performFinalDelete(ClassModel deleted) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;
        db.collection("users").document(userId).collection("classes").document(deleted.getId()).delete().addOnSuccessListener(aVoid -> {
            if (!isAdded()) return;
            if (!deleted.isExtra()) {
                renumberCycle(deleted.getBatchId(), deleted.getCycleNumber());
                decrementBatchCount(deleted.getBatchId());
            }
            cancelNotifications(deleted.getId());
        });
    }

    private void renumberCycle(String batchId, int cycleNumber) {
        String userId = FirebaseAuth.getInstance().getUid();
        db.collection("users").document(userId).collection("classes")
                .whereEqualTo("batchId", batchId)
                .whereEqualTo("cycleNumber", cycleNumber)
                .whereEqualTo("extra", false)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 1;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().update("monthlyNumber", String.valueOf(count++));
                    }
                });
    }

    private void decrementBatchCount(String batchId) {
        String userId = FirebaseAuth.getInstance().getUid();
        DocumentReference batchRef = db.collection("users").document(userId).collection("batches").document(batchId);
        batchRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                Long currentCount = snap.getLong("currentMonthCount");
                int newVal = Math.max(0, (currentCount != null ? currentCount.intValue() : 0) - 1);
                batchRef.update("currentMonthCount", newVal);
            }
        });
    }

    // ── NOTIFICATION & ALARM ──────────────────────────────────────────
    private static final int NOTIFY_BEFORE_MINUTES = 30;

    private void scheduleClassNotifications(String classId, String topic, String batchName, String timeText, String date) {
        if (getContext() == null) return;

        long triggerAtMillis = parseClassStartMillis(date, timeText);
        if (triggerAtMillis <= 0) return;

        long now = System.currentTimeMillis();

        // 1. Reminder Alarm (30 minutes before as requested)
        long reminderMillis = triggerAtMillis - (NOTIFY_BEFORE_MINUTES * 60 * 1000L);
        if (reminderMillis > now) {
            scheduleAlarm(classId.hashCode(), reminderMillis, 
                    batchName + " class starting soon", 
                    topic + " — in " + NOTIFY_BEFORE_MINUTES + " minutes");
        }

        // 2. Start Alarm (at class start time)
        if (triggerAtMillis > now) {
            scheduleAlarm(classId.hashCode() + 1, triggerAtMillis, 
                    batchName + " class starting now", 
                    topic + " — is starting now!");
        }
    }

    private void scheduleAlarm(int requestCode, long triggerTime, String title, String message) {
        if (getContext() == null) return;

        Intent intent = new Intent(getContext(), ClassReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("notificationId", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), requestCode, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
        
        Log.d("ScheduleFragment", "Alarm scheduled for: " + new Date(triggerTime).toString() + " (" + title + ")");
    }

    private void cancelNotifications(String classId) {
        if (getContext() == null) return;
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(getContext(), ClassReminderReceiver.class);
        
        // Cancel reminder
        PendingIntent pi1 = PendingIntent.getBroadcast(getContext(), classId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pi1);

        // Cancel start alarm
        PendingIntent pi2 = PendingIntent.getBroadcast(getContext(), classId.hashCode() + 1, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pi2);
    }

    private long parseClassStartMillis(String date, String timeText) {
        try {
            if (date == null || timeText == null || !timeText.contains(" - ")) return -1;
            String startTimeStr = timeText.split(" - ")[0].trim();
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            Date parsed = fmt.parse(date + " " + startTimeStr);
            return parsed != null ? parsed.getTime() : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
