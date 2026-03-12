package com.intisarmuhib.teachsync;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BatchesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private BatchAdapter adapter;
    private List<BatchModel> batchList;

    private FirebaseFirestore db;
    private String userId;

    private ImageButton backButton;
    private ListenerRegistration batchesListener;

    private int startHour = -1, startMinute = -1;
    private int endHour   = -1, endMinute   = -1;

    private final List<String> subjectList = new ArrayList<>();
    private final String[] commonSubjects = {
            "Mathematics", "Physics", "Chemistry", "Biology", "English",
            "ICT", "Higher Math", "Accounting", "Finance", "Economics"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batches);

        recyclerView = findViewById(R.id.recyclerViewBatch);
        fabAdd       = findViewById(R.id.fabAddBatch);
        backButton   = findViewById(R.id.back_button);

        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();

        batchList = new ArrayList<>();
        adapter   = new BatchAdapter(this, batchList, new BatchAdapter.OnBatchClickListener() {
            @Override
            public void onBatchClick(BatchModel batch) {
                showBatchDialog(batch);
            }

            @Override
            public void onInfoClick(BatchModel batch) {
                showBatchInfoDialog(batch);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> onBackPressed());

        BatchAdapter.attachSwipeToDelete(recyclerView, position -> {
            if (position < 0 || position >= batchList.size()) return;
            BatchModel deleted = batchList.get(position);
            batchList.remove(position);
            adapter.notifyItemRemoved(position);

            deleteBatchAndData(deleted);

            Snackbar.make(recyclerView, "Batch deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        batchList.add(position, deleted);
                        adapter.notifyItemInserted(position);
                        db.collection("users").document(userId)
                                .collection("batches")
                                .document(deleted.getId()).set(deleted);
                    }).show();
        });

        loadSubjects();
        loadBatches();

        fabAdd.setOnClickListener(v -> showBatchDialog(null));
        setupSearch();
    }

    private void showBatchInfoDialog(BatchModel batch) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_batch_info, null);
        dialog.setContentView(view);

        TextView tvName = view.findViewById(R.id.tvInfoName);
        TextView tvSubject = view.findViewById(R.id.tvInfoSubject);
        TextView tvTime = view.findViewById(R.id.tvInfoTime);
        TextView tvDuration = view.findViewById(R.id.tvInfoDuration);
        TextView tvEnrolled = view.findViewById(R.id.tvInfoEnrolled);
        TextView tvPayment = view.findViewById(R.id.tvInfoPayment);
        TextView tvClasses = view.findViewById(R.id.tvInfoClasses);
        TextView tvCycle = view.findViewById(R.id.tvInfoCycle);
        MaterialCalendarView calendarView = view.findViewById(R.id.calendarView);

        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = timeFmt.format(batch.getStartTime().toDate()) + " - " + timeFmt.format(batch.getEndTime().toDate());

        long minutes = batch.getDurationMinutes();
        String durationStr = (minutes / 60) + "h " + (minutes % 60) + "m";

        tvName.setText("Name: " + batch.getName());
        tvSubject.setText("Subject: " + batch.getSubject());
        tvTime.setText("Time: " + timeStr);
        tvDuration.setText("Duration: " + durationStr);
        tvEnrolled.setText("Enrolled Students: " + batch.getEnrolledCount());
        tvPayment.setText("Payment per Student: ৳ " + (int)batch.getPaymentPerStudent());
        tvClasses.setText("Monthly Classes: " + batch.getTotalMonthlyClasses());
        tvCycle.setText("Cycle Count: " + batch.getCycleCount());

        // Fetch classes for this batch
        db.collection("users").document(userId).collection("classes")
                .whereEqualTo("batchId", batch.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ClassModel> history = new ArrayList<>();
                    HashSet<CalendarDay> completedDays = new HashSet<>();
                    HashSet<CalendarDay> postponedDays = new HashSet<>();
                    HashSet<CalendarDay> rescheduledDays = new HashSet<>();
                    HashSet<CalendarDay> extraDays = new HashSet<>();

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ClassModel cm = doc.toObject(ClassModel.class);
                        if (cm != null) {
                            history.add(cm);
                            try {
                                Date date = sdf.parse(cm.getDate());
                                if (date != null) {
                                    CalendarDay day = CalendarDay.from(date);
                                    if (cm.isExtra()) {
                                        extraDays.add(day);
                                    } else if ("completed".equals(cm.getStatus())) {
                                        completedDays.add(day);
                                    } else if ("postponed".equals(cm.getStatus())) {
                                        postponedDays.add(day);
                                    } else if ("rescheduled".equals(cm.getStatus())) {
                                        rescheduledDays.add(day);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("BatchesActivity", "Date parse error", e);
                            }
                        }
                    }

                    calendarView.addDecorators(
                            new EventDecorator(Color.parseColor("#4CAF50"), completedDays),
                            new EventDecorator(Color.parseColor("#F44336"), postponedDays),
                            new EventDecorator(Color.parseColor("#FF9800"), rescheduledDays),
                            new EventDecorator(Color.parseColor("#2196F3"), extraDays)
                    );

                    view.findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportBatchToPdf(batch, history));
                });

        view.findViewById(R.id.btnCloseInfo).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void exportBatchToPdf(BatchModel batch, List<ClassModel> classes) {
        String fileName = "Batch_" + batch.getName().replaceAll("\\s+", "_") + ".pdf";
        File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Batch Report: " + batch.getName()).setFontSize(20).setBold());
            document.add(new Paragraph("Subject: " + batch.getSubject()));
            document.add(new Paragraph("Cycle Count: " + batch.getCycleCount()));
            document.add(new Paragraph("Monthly Classes: " + batch.getTotalMonthlyClasses()));
            document.add(new Paragraph("Enrolled Students: " + batch.getEnrolledCount()));
            document.add(new Paragraph("\nClass History:").setBold());

            Table table = new Table(new float[]{1, 2, 2, 2});
            table.addHeaderCell("Date");
            table.addHeaderCell("Topic");
            table.addHeaderCell("Status");
            table.addHeaderCell("Type");

            for (ClassModel cm : classes) {
                table.addCell(cm.getDate());
                table.addCell(cm.getTopic() != null ? cm.getTopic() : "");
                table.addCell(cm.getStatus() != null ? cm.getStatus().toUpperCase() : "");
                table.addCell(cm.isExtra() ? "EXTRA" : "NORMAL");
            }

            document.add(table);
            document.close();

            Toast.makeText(this, "PDF Exported to Documents", Toast.LENGTH_SHORT).show();
            openPdf(pdfFile);

        } catch (Exception e) {
            Log.e("BatchesActivity", "PDF Export error", e);
            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdf(File file) {
        Uri path = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
    }

    private static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        public EventDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(8, color));
        }
    }

    private void deleteBatchAndData(BatchModel batch) {
        db.collection("users").document(userId)
                .collection("batches").document(batch.getId()).delete();

        // Also delete all invoices associated with this batch
        db.collection("users").document(userId)
                .collection("invoices")
                .whereEqualTo("batchId", batch.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch writeBatch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        writeBatch.delete(doc.getReference());
                    }
                    writeBatch.commit();
                });

        // Also delete all classes associated with this batch
        db.collection("users").document(userId)
                .collection("classes")
                .whereEqualTo("batchId", batch.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch writeBatch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        writeBatch.delete(doc.getReference());
                    }
                    writeBatch.commit();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batchesListener != null) batchesListener.remove();
    }

    private void loadSubjects() {
        db.collection("users").document(userId).collection("subjects")
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> uniqueSubjects = new HashSet<>();
                    for (String s : commonSubjects) {
                        uniqueSubjects.add(s);
                    }
                    for (DocumentSnapshot doc : snap) {
                        String name = doc.getString("name");
                        if (name != null) uniqueSubjects.add(name);
                    }
                    subjectList.clear();
                    subjectList.addAll(uniqueSubjects);
                })
                .addOnFailureListener(e -> Log.e("BatchesActivity", "loadSubjects failed", e));
    }

    private void loadBatches() {
        batchesListener = db.collection("users").document(userId)
                .collection("batches")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Batches", error.getMessage());
                        return;
                    }
                    batchList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            BatchModel batch = doc.toObject(BatchModel.class);
                            if (batch != null) {
                                batch.setId(doc.getId());
                                batchList.add(batch);
                            }
                        }
                    }
                    adapter.setBatches(batchList);
                });
    }

    private void showBatchDialog(BatchModel editBatch) {
        startHour = -1; startMinute = -1;
        endHour   = -1; endMinute   = -1;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_batch, null);
        dialog.setContentView(view);

        TextView tvTitle                    = view.findViewById(R.id.tvInfoTitle);
        EditText etName                     = view.findViewById(R.id.etBatchName);
        AutoCompleteTextView etSubject      = view.findViewById(R.id.etSubject);
        AutoCompleteTextView etMonthly      = view.findViewById(R.id.etMonthlyClasses);
        TextInputEditText etPayment         = view.findViewById(R.id.etPayment);
        TextInputEditText etStart           = view.findViewById(R.id.etStartTime);
        TextInputEditText etEnd             = view.findViewById(R.id.etEndTime);
        TextView tvDuration                 = view.findViewById(R.id.tvDuration);

        // Class Scheduling Views
        RadioGroup radioGroupSchedule = view.findViewById(R.id.radioGroupSchedule);
        RadioButton radioAuto = view.findViewById(R.id.radioAuto);
        RadioButton radioManual = view.findViewById(R.id.radioManual);
        LinearLayout layoutAutoSchedule = view.findViewById(R.id.layoutAutoSchedule);
        LinearLayout layoutManualSchedule = view.findViewById(R.id.layoutManualSchedule);
        MaterialCalendarView calendarManual = view.findViewById(R.id.calendarManual);
        calendarManual.setSelectionMode(MaterialCalendarView.SELECTION_MODE_MULTIPLE);
        ChipGroup chipGroupDays = view.findViewById(R.id.chipGroupDays);

        etSubject.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subjectList));

        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= 31; i++) numbers.add(String.valueOf(i));
        etMonthly.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, numbers));

        radioGroupSchedule.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAuto) {
                layoutAutoSchedule.setVisibility(View.VISIBLE);
                layoutManualSchedule.setVisibility(View.GONE);
            } else {
                layoutAutoSchedule.setVisibility(View.GONE);
                layoutManualSchedule.setVisibility(View.VISIBLE);
            }
        });

        boolean isEdit = editBatch != null;

        if (isEdit) {
            tvTitle.setText("Edit Batch");
            etName.setText(editBatch.getName());
            etSubject.setText(editBatch.getSubject());
            etMonthly.setText(String.valueOf(editBatch.getTotalMonthlyClasses()), false);
            etPayment.setText(String.valueOf(editBatch.getPaymentPerStudent()));

            if (editBatch.getStartTime() != null) {
                Calendar sc = Calendar.getInstance();
                sc.setTime(editBatch.getStartTime().toDate());
                startHour   = sc.get(Calendar.HOUR_OF_DAY);
                startMinute = sc.get(Calendar.MINUTE);
                etStart.setText(formatTime(startHour, startMinute));
            }
            if (editBatch.getEndTime() != null) {
                Calendar ec = Calendar.getInstance();
                ec.setTime(editBatch.getEndTime().toDate());
                endHour   = ec.get(Calendar.HOUR_OF_DAY);
                endMinute = ec.get(Calendar.MINUTE);
                etEnd.setText(formatTime(endHour, endMinute));
            }
            tvDuration.setText("Duration: " + formatDuration(editBatch.getDurationMinutes()));
            
            if (editBatch.isAutoSchedule()) {
                radioAuto.setChecked(true);
                layoutAutoSchedule.setVisibility(View.VISIBLE);
                layoutManualSchedule.setVisibility(View.GONE);
                List<Integer> days = editBatch.getSelectedDays();
                if (days != null) {
                    if (days.contains(Calendar.SATURDAY)) ((Chip)view.findViewById(R.id.chipSat)).setChecked(true);
                    if (days.contains(Calendar.SUNDAY)) ((Chip)view.findViewById(R.id.chipSun)).setChecked(true);
                    if (days.contains(Calendar.MONDAY)) ((Chip)view.findViewById(R.id.chipMon)).setChecked(true);
                    if (days.contains(Calendar.TUESDAY)) ((Chip)view.findViewById(R.id.chipTue)).setChecked(true);
                    if (days.contains(Calendar.WEDNESDAY)) ((Chip)view.findViewById(R.id.chipWed)).setChecked(true);
                    if (days.contains(Calendar.THURSDAY)) ((Chip)view.findViewById(R.id.chipThu)).setChecked(true);
                    if (days.contains(Calendar.FRIDAY)) ((Chip)view.findViewById(R.id.chipFri)).setChecked(true);
                }
            } else {
                radioManual.setChecked(true);
                layoutAutoSchedule.setVisibility(View.GONE);
                layoutManualSchedule.setVisibility(View.VISIBLE);
            }
        }

        etStart.setOnClickListener(v -> showTimePicker(etStart, true, tvDuration));
        etEnd.setOnClickListener(v -> showTimePicker(etEnd, false, tvDuration));

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name       = etName.getText().toString().trim();
            String subject    = etSubject.getText().toString().trim();
            String monthlyStr = etMonthly.getText().toString().trim();
            String paymentStr = etPayment.getText().toString().trim();

            if (name.isEmpty() || subject.isEmpty() || monthlyStr.isEmpty() || paymentStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startHour == -1 || endHour == -1) {
                Toast.makeText(this, "Please select start and end time",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            int startTotal = startHour * 60 + startMinute;
            int endTotal   = endHour   * 60 + endMinute;

            boolean isOvernight = false;
            if (endTotal <= startTotal) {
                endTotal += 24 * 60;
                isOvernight = true;
            }

            long duration = endTotal - startTotal;
            int totalMonthlyClasses = Integer.parseInt(monthlyStr);
            double paymentPerStudent = Double.parseDouble(paymentStr);

            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.HOUR_OF_DAY, startHour);
            startCal.set(Calendar.MINUTE, startMinute);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.set(Calendar.HOUR_OF_DAY, endHour);
            endCal.set(Calendar.MINUTE, endMinute);
            endCal.set(Calendar.SECOND, 0);
            endCal.set(Calendar.MILLISECOND, 0);
            if (isOvernight) {
                endCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            String id = isEdit ? editBatch.getId()
                    : db.collection("users").document(userId)
                            .collection("batches").document().getId();

            BatchModel batch = new BatchModel(
                    id, name, subject,
                    new Timestamp(startCal.getTime()),
                    new Timestamp(endCal.getTime()),
                    duration, totalMonthlyClasses,
                    isEdit ? editBatch.getCurrentMonthCount() : 0,
                    isEdit ? editBatch.getCycleCount() : 1,
                    paymentPerStudent,
                    isEdit ? editBatch.getCreatedAt()
                           : new Timestamp(Calendar.getInstance().getTime())
            );
            
            boolean isAuto = radioAuto.isChecked();
            batch.setAutoSchedule(isAuto);
            if (isAuto) {
                List<Integer> selectedDays = new ArrayList<>();
                if (((Chip)view.findViewById(R.id.chipSun)).isChecked()) selectedDays.add(Calendar.SUNDAY);
                if (((Chip)view.findViewById(R.id.chipMon)).isChecked()) selectedDays.add(Calendar.MONDAY);
                if (((Chip)view.findViewById(R.id.chipTue)).isChecked()) selectedDays.add(Calendar.TUESDAY);
                if (((Chip)view.findViewById(R.id.chipWed)).isChecked()) selectedDays.add(Calendar.WEDNESDAY);
                if (((Chip)view.findViewById(R.id.chipThu)).isChecked()) selectedDays.add(Calendar.THURSDAY);
                if (((Chip)view.findViewById(R.id.chipFri)).isChecked()) selectedDays.add(Calendar.FRIDAY);
                if (((Chip)view.findViewById(R.id.chipSat)).isChecked()) selectedDays.add(Calendar.SATURDAY);
                batch.setSelectedDays(selectedDays);
                batch.setWeeklyCount(selectedDays.size());
            }

            checkAndAddSubject(subject);

            db.collection("users").document(userId)
                    .collection("batches").document(id)
                    .set(batch)
                    .addOnSuccessListener(aVoid -> {
                        if (isAuto) {
                            autoScheduleClasses(batch, chipGroupDays);
                        } else {
                            manualScheduleClasses(batch, calendarManual.getSelectedDates());
                        }
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("BatchesActivity", "Save batch failed", e);
                        Toast.makeText(this, "Failed to save. Try again.",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void manualScheduleClasses(BatchModel batch, List<CalendarDay> selectedDates) {
        if (selectedDates == null || selectedDates.isEmpty()) return;

        WriteBatch writeBatch = db.batch();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeRange = timeFmt.format(batch.getStartTime().toDate()) + " - " + timeFmt.format(batch.getEndTime().toDate());

        int count = 0;
        for (CalendarDay day : selectedDates) {
            count++;
            String classId = db.collection("users").document(userId).collection("classes").document().getId();
            ClassModel classModel = new ClassModel(
                    classId,
                    "Manual Class",
                    batch.getName(),
                    batch.getId(),
                    timeRange,
                    dateFormat.format(day.getDate()),
                    String.valueOf(count),
                    false,
                    batch.getCycleCount(),
                    batch.getTotalMonthlyClasses(),
                    new Timestamp(Calendar.getInstance().getTime())
            );
            writeBatch.set(db.collection("users").document(userId).collection("classes").document(classId), classModel);
        }
        writeBatch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(BatchesActivity.this, selectedDates.size() + " classes scheduled manually", Toast.LENGTH_SHORT).show();
        });
    }

    private void cleanupScheduledClasses(String batchId) {
        db.collection("users").document(userId).collection("classes")
                .whereEqualTo("batchId", batchId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch writeBatch = db.batch();
                    int count = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if (!"completed".equals(status)) {
                            writeBatch.delete(doc.getReference());
                            count++;
                        }
                    }
                    if (count > 0) {
                        final int finalCount = count;
                        writeBatch.commit().addOnSuccessListener(aVoid -> 
                            Toast.makeText(BatchesActivity.this, "Cleaned up " + finalCount + " scheduled classes", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void autoScheduleClasses(BatchModel batch, ChipGroup chipGroup) {
        List<Integer> selectedDays = new ArrayList<>();
        if (((Chip)chipGroup.findViewById(R.id.chipSun)).isChecked()) selectedDays.add(Calendar.SUNDAY);
        if (((Chip)chipGroup.findViewById(R.id.chipMon)).isChecked()) selectedDays.add(Calendar.MONDAY);
        if (((Chip)chipGroup.findViewById(R.id.chipTue)).isChecked()) selectedDays.add(Calendar.TUESDAY);
        if (((Chip)chipGroup.findViewById(R.id.chipWed)).isChecked()) selectedDays.add(Calendar.WEDNESDAY);
        if (((Chip)chipGroup.findViewById(R.id.chipThu)).isChecked()) selectedDays.add(Calendar.THURSDAY);
        if (((Chip)chipGroup.findViewById(R.id.chipFri)).isChecked()) selectedDays.add(Calendar.FRIDAY);
        if (((Chip)chipGroup.findViewById(R.id.chipSat)).isChecked()) selectedDays.add(Calendar.SATURDAY);

        if (selectedDays.isEmpty()) return;

        WriteBatch writeBatch = db.batch();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeRange = timeFmt.format(batch.getStartTime().toDate()) + " - " + timeFmt.format(batch.getEndTime().toDate());

        int classesAddedCount = 0;
        for (int i = 0; i < 60 && classesAddedCount < batch.getTotalMonthlyClasses(); i++) {
            if (selectedDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                classesAddedCount++;
                String classId = db.collection("users").document(userId).collection("classes").document().getId();
                ClassModel classModel = new ClassModel(
                        classId,
                        "Initial Class",
                        batch.getName(),
                        batch.getId(),
                        timeRange,
                        dateFormat.format(cal.getTime()),
                        String.valueOf(classesAddedCount),
                        false,
                        batch.getCycleCount(),
                        batch.getTotalMonthlyClasses(),
                        new Timestamp(Calendar.getInstance().getTime())
                );
                writeBatch.set(db.collection("users").document(userId).collection("classes").document(classId), classModel);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        final int finalClassesAdded = classesAddedCount;
        writeBatch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(BatchesActivity.this, finalClassesAdded + " classes scheduled automatically", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkAndAddSubject(String name) {
        db.collection("users").document(userId).collection("subjects")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        String subId = db.collection("users").document(userId).collection("subjects").document().getId();
                        SubjectModel newSub = new SubjectModel(subId, name, "", "");
                        db.collection("users").document(userId).collection("subjects").document(subId).set(newSub);
                        if (!subjectList.contains(name)) {
                            subjectList.add(name);
                        }
                    }
                });
    }

    private void showTimePicker(EditText editText, boolean isStart, TextView tvDuration) {
        int initH = isStart ? (startHour   == -1 ? 12 : startHour)
                            : (endHour     == -1 ? 12 : endHour);
        int initM = isStart ? (startMinute == -1 ? 0  : startMinute)
                            : (endMinute   == -1 ? 0  : endMinute);

        new android.app.TimePickerDialog(this, (v, h, m) -> {
            if (isStart) { startHour = h; startMinute = m; }
            else         { endHour   = h; endMinute   = m; }
            editText.setText(formatTime(h, m));
            calculateDuration(tvDuration);
        }, initH, initM, false).show();
    }

    private void calculateDuration(TextView tv) {
        if (startHour == -1 || endHour == -1) return;
        int startTotal = startHour * 60 + startMinute;
        int endTotal = endHour * 60 + endMinute;
        int diff = endTotal - startTotal;
        if (diff <= 0) diff += 24 * 60;
        tv.setText("Duration: " + formatDuration(diff));
    }

    private String formatTime(int h, int m) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime());
    }

    private String formatDuration(long minutes) {
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private void setupSearch() {
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchBatch);
        searchView.setOnQueryTextListener(
                new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override public boolean onQueryTextSubmit(String q) {
                        adapter.filter(q); return true;
                    }
                    @Override public boolean onQueryTextChange(String t) {
                        adapter.filter(t); return true;
                    }
                });
    }
}
