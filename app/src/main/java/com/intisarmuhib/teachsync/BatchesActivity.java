package com.intisarmuhib.teachsync;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

    // ═════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════

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
        adapter   = new BatchAdapter(this, batchList, this::showBatchDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> onBackPressed());

        // ── FEATURE: red swipe-to-delete (shared helper in BatchAdapter) ─
        BatchAdapter.attachSwipeToDelete(recyclerView, position -> {
            if (position < 0 || position >= batchList.size()) return;
            BatchModel deleted = batchList.get(position);
            batchList.remove(position);
            adapter.notifyItemRemoved(position);

            db.collection("users").document(userId)
                    .collection("batches").document(deleted.getId()).delete();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batchesListener != null) batchesListener.remove();
    }

    // ═════════════════════════════════════════════════════════════════════
    // LOAD DATA
    // ═════════════════════════════════════════════════════════════════════

    private void loadSubjects() {
        db.collection("users").document(userId).collection("subjects")
                .get()
                .addOnSuccessListener(snap -> {
                    subjectList.clear();
                    for (DocumentSnapshot doc : snap) {
                        String name = doc.getString("name");
                        if (name != null) subjectList.add(name);
                    }
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

    // ═════════════════════════════════════════════════════════════════════
    // ADD / EDIT BATCH DIALOG
    // ═════════════════════════════════════════════════════════════════════

    private void showBatchDialog(BatchModel editBatch) {
        // Reset time fields so previous dialog values don't bleed in
        startHour = -1; startMinute = -1;
        endHour   = -1; endMinute   = -1;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_batch, null);
        dialog.setContentView(view);

        EditText etName                     = view.findViewById(R.id.etBatchName);
        AutoCompleteTextView etSubject      = view.findViewById(R.id.etSubject);
        AutoCompleteTextView etMonthly      = view.findViewById(R.id.etMonthlyClasses);
        TextInputEditText etStart           = view.findViewById(R.id.etStartTime);
        TextInputEditText etEnd             = view.findViewById(R.id.etEndTime);
        TextView tvDuration                 = view.findViewById(R.id.tvDuration);

        etSubject.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subjectList));

        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= 31; i++) numbers.add(String.valueOf(i));
        etMonthly.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, numbers));

        boolean isEdit = editBatch != null;

        if (isEdit) {
            etName.setText(editBatch.getName());
            etSubject.setText(editBatch.getSubject());
            etMonthly.setText(String.valueOf(editBatch.getTotalMonthlyClasses()), false);

            // Use Calendar instead of deprecated Date.getHours()
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
        }

        etStart.setOnClickListener(v -> showTimePicker(etStart, true, tvDuration));
        etEnd.setOnClickListener(v -> showTimePicker(etEnd, false, tvDuration));

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name       = etName.getText().toString().trim();
            String subject    = etSubject.getText().toString().trim();
            String monthlyStr = etMonthly.getText().toString().trim();

            if (name.isEmpty() || subject.isEmpty() || monthlyStr.isEmpty()) {
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

            if (endTotal <= startTotal) {
                Snackbar.make(view, "End time must be after start time",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }

            long duration = endTotal - startTotal;
            int totalMonthlyClasses = Integer.parseInt(monthlyStr);

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
                    isEdit ? editBatch.getCreatedAt()
                           : new Timestamp(Calendar.getInstance().getTime())
            );

            db.collection("users").document(userId)
                    .collection("batches").document(id)
                    .set(batch)
                    .addOnSuccessListener(aVoid -> dialog.dismiss())
                    .addOnFailureListener(e -> {
                        Log.e("BatchesActivity", "Save batch failed", e);
                        Toast.makeText(this, "Failed to save. Try again.",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    // ═════════════════════════════════════════════════════════════════════
    // TIME PICKER
    // ═════════════════════════════════════════════════════════════════════

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
        int diff = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
        if (diff > 0) tv.setText("Duration: " + formatDuration(diff));
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

    // ═════════════════════════════════════════════════════════════════════
    // SEARCH
    // ═════════════════════════════════════════════════════════════════════

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
