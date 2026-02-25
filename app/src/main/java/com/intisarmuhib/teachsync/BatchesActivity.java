package com.intisarmuhib.teachsync;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private int startHour = -1, startMinute = -1;
    private int endHour = -1, endMinute = -1;

    private List<String> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batches);

        recyclerView = findViewById(R.id.recyclerViewBatch);
        fabAdd = findViewById(R.id.fabAddBatch);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        batchList = new ArrayList<>();
        adapter = new BatchAdapter(this, batchList, this::showBatchDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        loadSubjects();
        loadBatches();
        enableSwipeToDelete();
        fabAdd.setOnClickListener(v -> showBatchDialog(null)); setupSearch(); }

    // ------------------ Load Subjects ------------------

    private void loadSubjects() {
        db.collection("users").document(userId)
                .collection("subjects")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    subjectList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        subjectList.add(doc.getString("name"));
                    }
                });
    }

    // ------------------ Load Batches ------------------

    private void loadBatches() {
        db.collection("users").document(userId)
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
                            batch.setId(doc.getId());
                            batchList.add(batch);
                        }
                    }

                    adapter.setBatches(batchList);
                });
    }

    // ------------------ BottomSheet ------------------

    private void showBatchDialog(BatchModel editBatch) {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_batch, null);

        dialog.setContentView(view);

        EditText etName = view.findViewById(R.id.etBatchName);
        AutoCompleteTextView etSubject = view.findViewById(R.id.etSubject);
        AutoCompleteTextView etMonthlyClasses = view.findViewById(R.id.etMonthlyClasses);
        TextInputEditText etStart = view.findViewById(R.id.etStartTime);
        TextInputEditText etEnd = view.findViewById(R.id.etEndTime);
        TextView tvDuration = view.findViewById(R.id.tvDuration);

        // ---------- Subject Dropdown ----------
        ArrayAdapter<String> subjectAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        subjectList);

        etSubject.setAdapter(subjectAdapter);

        // ---------- Monthly Classes Dropdown (1–31) ----------
        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            numbers.add(String.valueOf(i));
        }

        ArrayAdapter<String> numberAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        numbers);

        etMonthlyClasses.setAdapter(numberAdapter);

        boolean isEdit = editBatch != null;

        if (isEdit) {
            etName.setText(editBatch.getName());
            etSubject.setText(editBatch.getSubject());
            etMonthlyClasses.setText(
                    String.valueOf(editBatch.getTotalMonthlyClasses()),
                    false
            );

            startHour = editBatch.getStartTime().toDate().getHours();
            startMinute = editBatch.getStartTime().toDate().getMinutes();
            endHour = editBatch.getEndTime().toDate().getHours();
            endMinute = editBatch.getEndTime().toDate().getMinutes();

            etStart.setText(formatTime(startHour, startMinute));
            etEnd.setText(formatTime(endHour, endMinute));

            tvDuration.setText("Duration: " +
                    formatDuration(editBatch.getDurationMinutes()));
        }

        etStart.setOnClickListener(v -> showTimePicker(etStart, true, tvDuration));
        etEnd.setOnClickListener(v -> showTimePicker(etEnd, false, tvDuration));

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String subject = etSubject.getText().toString().trim();
            String monthlyStr = etMonthlyClasses.getText().toString().trim();

            if (name.isEmpty() || subject.isEmpty() || monthlyStr.isEmpty())
                return;

            if (startHour == -1 || endHour == -1) return;

            int totalMonthlyClasses = Integer.parseInt(monthlyStr);

            int startTotal = startHour * 60 + startMinute;
            int endTotal = endHour * 60 + endMinute;

            if (endTotal <= startTotal) {
                Snackbar.make(view,
                        "End time must be after start time",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }

            long duration = endTotal - startTotal;

            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.HOUR_OF_DAY, startHour);
            startCal.set(Calendar.MINUTE, startMinute);

            Calendar endCal = Calendar.getInstance();
            endCal.set(Calendar.HOUR_OF_DAY, endHour);
            endCal.set(Calendar.MINUTE, endMinute);

            String id = isEdit ? editBatch.getId() :
                    db.collection("users")
                            .document(userId)
                            .collection("batches")
                            .document().getId();

            BatchModel batch = new BatchModel(
                    id,
                    name,
                    subject,
                    new Timestamp(startCal.getTime()),
                    new Timestamp(endCal.getTime()),
                    duration,
                    totalMonthlyClasses,
                    new Timestamp(Calendar.getInstance().getTime())
            );

            db.collection("users")
                    .document(userId)
                    .collection("batches")
                    .document(id)
                    .set(batch);

            dialog.dismiss();
        });

        dialog.show();
    }

    // ------------------ TimePicker ------------------

    private void showTimePicker(EditText editText,
                                boolean isStart,
                                TextView tvDuration) {

        android.app.TimePickerDialog dialog =
                new android.app.TimePickerDialog(this,
                        (view, hourOfDay, minute) -> {

                            if (isStart) {
                                startHour = hourOfDay;
                                startMinute = minute;
                            } else {
                                endHour = hourOfDay;
                                endMinute = minute;
                            }

                            editText.setText(formatTime(hourOfDay, minute));
                            calculateDuration(tvDuration);

                        }, 12, 0, false);

        dialog.show();
    }

    private void calculateDuration(TextView tv) {

        if (startHour == -1 || endHour == -1) return;

        int startTotal = startHour * 60 + startMinute;
        int endTotal = endHour * 60 + endMinute;

        if (endTotal > startTotal) {
            long diff = endTotal - startTotal;
            tv.setText("Duration: " + formatDuration(diff));
        }
    }

    private String formatTime(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(cal.getTime());
    }

    private String formatDuration(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "h " + m + "m";
    }
    // ------------------ Swipe Delete ------------------
    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT)
        { @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; } @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { int position = viewHolder.getAdapterPosition(); BatchModel deletedBatch = batchList.get(position); // Remove locally
             batchList.remove(position); adapter.notifyItemRemoved(position); // Delete from Firestore
             db.collection("users").document(userId).collection("batches").document(deletedBatch.getId()).delete(); // Snackbar Undo
             Snackbar.make(recyclerView, "Batch deleted", Snackbar.LENGTH_LONG) .setAction("UNDO", v -> { batchList.add(position, deletedBatch); adapter.notifyItemInserted(position); db.collection("users").document(userId).collection("batches").document(deletedBatch.getId()).set(deletedBatch); }) .show(); } @Override public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) { View itemView = viewHolder.itemView; Paint paint = new Paint(); paint.setColor(Color.RED); // Draw red background
             c.drawRect( itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint ); // Draw delete icon
        Drawable icon = ContextCompat.getDrawable(BatchesActivity.this, R.drawable.ic_delete);
        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2; int iconTop = itemView.getTop() + iconMargin;
        int iconBottom = iconTop + icon.getIntrinsicHeight(); int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth(); int iconRight = itemView.getRight() - iconMargin; icon.setBounds(iconLeft, iconTop, iconRight, iconBottom); icon.draw(c); super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive); } }; new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView); }
    // ------------------ Search setup ------------------
    private void setupSearch() {
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchBatch);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { adapter.filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { adapter.filter(newText); return true; } }); }
    // ------------------ Edit batch helper ------------------
    private void showEditDialog(BatchModel batch) { showBatchDialog(batch); }

}
