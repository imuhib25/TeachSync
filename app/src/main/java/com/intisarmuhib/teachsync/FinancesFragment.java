package com.intisarmuhib.teachsync;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class FinancesFragment extends Fragment {

    private static final String TAG = "FinancesFragment";
    private TextView tvMonthLabel, tvCollected, tvDue, tvOverdue, tvForecast;
    private ProgressBar progressFinance;
    private RecyclerView rvBatchFinance, rvTransactions;
    private FloatingActionButton fabAddFinance;
    private ImageButton btnPrevMonth, btnNextMonth;
    private LinearLayout layoutEmptyState;
    private AdView mAdView;

    private FirebaseFirestore db;
    private String userId;
    private String currencySymbol = "৳";
    private Calendar selectedCalendar = Calendar.getInstance();

    private final List<TransactionModel> transactionList = new ArrayList<>();
    private final List<BatchFinanceModel> batchFinanceList = new ArrayList<>();
    private final List<InvoiceModel> currentMonthInvoices = new ArrayList<>();

    private TransactionAdapter transactionAdapter;
    private BatchFinanceAdapter batchFinanceAdapter;

    private ListenerRegistration transactionsListener;
    private ListenerRegistration invoicesListener;

    private double totalCollectedThisMonth = 0;
    private double totalExpectedThisMonth = 0;
    
    private Set<String> notifiedBatchCycles = new HashSet<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finances, container, false);

        initViews(view);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        loadCurrency();
        updateLabels();

        // Load Ad
        AdRequest adRequest = new AdRequest.Builder().build();
        if (mAdView != null) {
            mAdView.loadAd(adRequest);
        }

        transactionAdapter = new TransactionAdapter(transactionList, currencySymbol, new TransactionAdapter.OnTransactionListener() {
            @Override
            public void onDelete(TransactionModel transaction, int position) {
                showDeleteConfirmation(transaction);
            }

            @Override
            public void onEdit(TransactionModel transaction, int position) {
                showEditTransactionDialog(transaction);
            }

            @Override
            public void onGenerateInvoice(TransactionModel transaction) {
                generatePdfInvoice(transaction);
            }
        });
        rvTransactions.setAdapter(transactionAdapter);

        batchFinanceAdapter = new BatchFinanceAdapter(batchFinanceList, currencySymbol, this::showBatchStudentsDialog);
        rvBatchFinance.setAdapter(batchFinanceAdapter);

        loadFinanceData();

        fabAddFinance.setOnClickListener(v -> showAddTransactionDialog());

        btnPrevMonth.setOnClickListener(v -> {
            selectedCalendar.add(Calendar.MONTH, -1);
            updateUIForMonthChange();
        });

        btnNextMonth.setOnClickListener(v -> {
            selectedCalendar.add(Calendar.MONTH, 1);
            updateUIForMonthChange();
        });

        return view;
    }

    private void updateUIForMonthChange() {
        updateLabels();
        loadFinanceData();
    }

    private void initViews(View view) {
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel);
        tvCollected = view.findViewById(R.id.tvCollected);
        tvDue = view.findViewById(R.id.tvDue);
        tvOverdue = view.findViewById(R.id.tvOverdue);
        tvForecast = view.findViewById(R.id.tvForecast);
        progressFinance = view.findViewById(R.id.progressFinance);
        rvBatchFinance = view.findViewById(R.id.rvBatchFinance);
        rvTransactions = view.findViewById(R.id.rvTransactions);
        fabAddFinance = view.findViewById(R.id.fabAddFinance);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        mAdView = view.findViewById(R.id.adView);

        rvBatchFinance.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadCurrency() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currencySymbol = prefs.getString("currency_symbol", "৳");
    }

    private void updateLabels() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthLabel.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void loadFinanceData() {
        if (userId == null) return;

        if (transactionsListener != null) transactionsListener.remove();
        if (invoicesListener != null) invoicesListener.remove();

        int currentMonth = selectedCalendar.get(Calendar.MONTH);
        int currentYear = selectedCalendar.get(Calendar.YEAR);

        // TRANSACTIONS
        transactionsListener = db.collection("users")
                .document(userId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null || !isAdded()) return;

                    transactionList.clear();
                    totalCollectedThisMonth = 0;

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        TransactionModel tx = doc.toObject(TransactionModel.class);
                        if (tx == null) continue;

                        tx.setId(doc.getId());

                        if (tx.getTimestamp() != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(tx.getTimestamp().toDate());

                            if (cal.get(Calendar.MONTH) == currentMonth &&
                                    cal.get(Calendar.YEAR) == currentYear) {
                                transactionList.add(tx);
                                totalCollectedThisMonth += tx.getAmount();
                            }
                        }
                    }

                    tvCollected.setText(String.format(Locale.getDefault(),
                            "%s %d", currencySymbol, (int) totalCollectedThisMonth));

                    transactionAdapter.notifyDataSetChanged();
                    
                    if (layoutEmptyState != null) {
                        layoutEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    calculateOverallProgress();
                });

        // INVOICES
        invoicesListener = db.collection("users")
                .document(userId)
                .collection("invoices")
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null || !isAdded()) return;

                    double totalDue = 0;
                    double totalOverdue = 0;
                    totalExpectedThisMonth = 0;
                    currentMonthInvoices.clear();

                    Map<String, BatchFinanceModel> batchMap = new HashMap<>();
                    Map<String, Set<String>> batchCycleStudentsMap = new HashMap<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        InvoiceModel inv = doc.toObject(InvoiceModel.class);
                        if (inv == null) continue;

                        if (inv.getMonth() == null) continue;

                        Calendar invMonth = Calendar.getInstance();
                        invMonth.setTime(inv.getMonth().toDate());

                        boolean isSelectedMonth = invMonth.get(Calendar.MONTH) == currentMonth &&
                                invMonth.get(Calendar.YEAR) == currentYear;
                        
                        boolean isOverdue = "Overdue".equalsIgnoreCase(inv.getStatus());

                        // We show invoices of the selected month OR any overdue invoices regardless of month
                        if (!isSelectedMonth && !isOverdue) {
                            continue;
                        }

                        currentMonthInvoices.add(inv);

                        double amount = inv.getAmount();
                        double paid = inv.getPaidAmount();
                        double due = Math.max(0, amount - paid);

                        if (isSelectedMonth) {
                            totalExpectedThisMonth += amount;
                        }

                        if (due > 0) {
                            totalDue += due;

                            if (isOverdue) {
                                totalOverdue += due;
                            }
                        }

                        String batchId = inv.getBatchId();
                        String batchName = inv.getBatchName();
                        int cycleCount = inv.getCycleCount();

                        if (batchId == null) continue;

                        String compositeKey = batchId + "_c" + cycleCount;
                        Set<String> students = batchCycleStudentsMap.get(compositeKey);

                        if (students == null) {
                            students = new HashSet<>();
                            batchCycleStudentsMap.put(compositeKey, students);
                        }

                        students.add(inv.getStudentId());

                        BatchFinanceModel batch = batchMap.get(compositeKey);

                        if (batch == null) {
                            String displayName = batchName + (cycleCount > 1 ? " (Cycle " + cycleCount + ")" : "");
                            batch = new BatchFinanceModel(
                                    batchId,
                                    displayName,
                                    0, // studentCount updated later
                                    0, // collected
                                    0, // due
                                    cycleCount
                            );
                            batchMap.put(compositeKey, batch);
                        }

                        batch.setCollectedAmount(batch.getCollectedAmount() + paid);
                        batch.setDueAmount(batch.getDueAmount() + due);
                    }

                    tvDue.setText(String.format(Locale.getDefault(), "%s %d", currencySymbol, (int) totalDue));
                    tvOverdue.setText(String.format(Locale.getDefault(), "%s %d", currencySymbol, (int) totalOverdue));
                    tvForecast.setText(String.format(Locale.getDefault(), "%s %d", currencySymbol, (int) totalExpectedThisMonth));

                    batchFinanceList.clear();
                    for (String key : batchMap.keySet()) {
                        BatchFinanceModel bfm = batchMap.get(key);
                        if (bfm != null) {
                            Set<String> students = batchCycleStudentsMap.get(key);
                            bfm.setStudentCount(students != null ? students.size() : 0);
                        }
                    }
                    batchFinanceList.addAll(batchMap.values());

                    // Check for payment completions
                    for (BatchFinanceModel bfm : batchFinanceList) {
                        if (bfm.getDueAmount() <= 0 && bfm.getCollectedAmount() > 0) {
                            checkAndNotifyCompletion(bfm);
                        }
                    }

                    batchFinanceAdapter.notifyDataSetChanged();

                    calculateOverallProgress();
                });
    }

    private void checkAndNotifyCompletion(BatchFinanceModel bfm) {
        String key = bfm.getBatchId() + "_" + bfm.getCycleCount();
        if (notifiedBatchCycles.contains(key)) return;

        notifiedBatchCycles.add(key);
        
        String title = "Payment Complete";
        String message = "All payments for batch " + bfm.getBatchName() + " are collected.";

        // Send System Notification
        sendSystemNotification(title, message);

        // Add to Recent Activity / Dashboard Notifications
        ActivityModel activity = new ActivityModel(title, message, null);
        db.collection("users").document(userId).collection("recent_activities").add(activity);
    }

    private void sendSystemNotification(String title, String message) {
        if (getContext() == null) return;
        
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), TeachSyncApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.outline_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void showBatchStudentsDialog(BatchFinanceModel model) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(R.layout.dialog_batch_students_finance);

        TextView tvName = dialog.findViewById(R.id.tvDialogBatchName);
        RecyclerView rv = dialog.findViewById(R.id.rvBatchStudentsFinance);
        ImageButton btnDownload = dialog.findViewById(R.id.btnDownloadBatchPdf);

        if (tvName != null) tvName.setText(model.getBatchName());

        List<InvoiceModel> batchInvoices = new ArrayList<>();
        for (InvoiceModel inv : currentMonthInvoices) {
            if (model.getBatchId().equals(inv.getBatchId()) && model.getCycleCount() == inv.getCycleCount()) {
                batchInvoices.add(inv);
            }
        }

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(new BatchStudentFinanceAdapter(batchInvoices));
        }

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> generateBatchFinancePdf(model, batchInvoices));
        }

        dialog.show();
    }

    private void generateBatchFinancePdf(BatchFinanceModel model, List<InvoiceModel> invoices) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Header
        paint.setColor(Color.BLACK);
        paint.setTextSize(18f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        int x = 40, y = 50;
        canvas.drawText("TeachSync - Batch Finance Breakdown", x, y, paint);

        paint.setTextSize(12f);
        paint.setTypeface(Typeface.DEFAULT);
        y += 25;
        canvas.drawText("Batch: " + model.getBatchName(), x, y, paint);
        y += 20;
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        canvas.drawText("Month: " + sdf.format(selectedCalendar.getTime()), x, y, paint);

        // Summary
        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Summary:", x, y, paint);
        paint.setTypeface(Typeface.DEFAULT);
        y += 20;
        canvas.drawText("Total Collected: " + currencySymbol + " " + (int)model.getCollectedAmount(), x, y, paint);
        y += 20;
        canvas.drawText("Total Due: " + currencySymbol + " " + (int)model.getDueAmount(), x, y, paint);
        y += 20;
        canvas.drawText("Student Count: " + model.getStudentCount(), x, y, paint);

        // Table Header
        y += 50;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Student Name", x, y, paint);
        canvas.drawText("Amount", 250, y, paint);
        canvas.drawText("Paid", 350, y, paint);
        canvas.drawText("Due", 450, y, paint);
        canvas.drawText("Status", 520, y, paint);
        
        y += 10;
        paint.setStrokeWidth(1f);
        canvas.drawLine(x, y, 555, y, paint);

        // Table Rows
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10f);
        for (InvoiceModel inv : invoices) {
            y += 25;
            if (y > 800) { // Check for page overflow
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
            canvas.drawText(inv.getStudentName(), x, y, paint);
            canvas.drawText(String.valueOf((int)inv.getAmount()), 250, y, paint);
            canvas.drawText(String.valueOf((int)inv.getPaidAmount()), 350, y, paint);
            canvas.drawText(String.valueOf((int)inv.getDueAmount()), 450, y, paint);
            canvas.drawText(inv.getStatus(), 520, y, paint);
        }

        document.finishPage(page);

        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String fileName = "BatchFinance_" + model.getBatchName().replaceAll("\\s+", "_") + "_" + 
                         new SimpleDateFormat("MMM_yyyy", Locale.getDefault()).format(selectedCalendar.getTime()) + ".pdf";
        File pdfFile = new File(dir, fileName);

        try {
            document.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(getContext(), R.string.pdf_generated, Toast.LENGTH_SHORT).show();
            openPdf(pdfFile);
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    private void calculateOverallProgress() {
        if (totalExpectedThisMonth > 0) {
            int progress = (int) ((totalCollectedThisMonth / totalExpectedThisMonth) * 100);
            if (progressFinance != null) {
                progressFinance.setProgress(Math.min(progress, 100));
            }
        } else {
            if (progressFinance != null) progressFinance.setProgress(0);
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroyView() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroyView();
        if (transactionsListener != null) transactionsListener.remove();
        if (invoicesListener != null) invoicesListener.remove();
    }

    private void showDeleteConfirmation(TransactionModel transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_transaction)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTransaction(transaction))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteTransaction(TransactionModel tx) {
        if (tx.getInvoiceId() != null) {
            db.collection("users").document(userId).collection("invoices")
                    .document(tx.getInvoiceId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            InvoiceModel inv = doc.toObject(InvoiceModel.class);
                            if (inv == null) return;
                            
                            double newPaid = Math.max(0, inv.getPaidAmount() - tx.getAmount());
                            String newStatus = newPaid < inv.getAmount() ? "Due" : "Paid";
                            doc.getReference().update("paidAmount", newPaid, "status", newStatus);
                        }
                    });
        }

        db.collection("users").document(userId).collection("transactions")
                .document(tx.getId()).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), R.string.transaction_deleted, Toast.LENGTH_SHORT).show());
    }

    private void showAddTransactionDialog() {
        showTransactionDialog(null);
    }

    private void showEditTransactionDialog(TransactionModel tx) {
        showTransactionDialog(tx);
    }

    private void showTransactionDialog(TransactionModel existingTx) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_transaction);

        AutoCompleteTextView studentSpinner = dialog.findViewById(R.id.spinnerStudent);
        TextInputEditText etAmount = dialog.findViewById(R.id.etAmount);
        TextInputEditText etDate = dialog.findViewById(R.id.etTransactionDate);
        RadioGroup rgMethod = dialog.findViewById(R.id.rgMethod);
        Button btnSave = dialog.findViewById(R.id.btnSaveTransaction);
        TextView tvTitle = dialog.findViewById(R.id.tvTransactionTitle);

        if (studentSpinner == null || etAmount == null || etDate == null || rgMethod == null || btnSave == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        if (existingTx != null) {
            if (tvTitle != null) tvTitle.setText(R.string.edit_transaction);
            studentSpinner.setText(existingTx.getStudentName());
            studentSpinner.setEnabled(false);
            etAmount.setText(String.valueOf((int)existingTx.getAmount()));
            if (existingTx.getTimestamp() != null) {
                etDate.setText(sdf.format(existingTx.getTimestamp().toDate()));
            }
            if ("bKash".equals(existingTx.getMethod())) rgMethod.check(R.id.rb_bkash);
            else if ("Nagad".equals(existingTx.getMethod())) rgMethod.check(R.id.rb_nagad);
            else rgMethod.check(R.id.rb_cash);
            btnSave.setText(R.string.update_transaction);
        } else {
            etDate.setText(sdf.format(Calendar.getInstance().getTime()));
        }

        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.show(getChildFragmentManager(), "TRANSACTION_DATE");
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(selection);
                etDate.setText(sdf.format(cal.getTime()));
            });
        });

        List<String> studentNames = new ArrayList<>();
        Map<String, DocumentSnapshot> studentMap = new HashMap<>();

        db.collection("users").document(userId).collection("students").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) {
                            studentNames.add(name);
                            studentMap.put(name, doc);
                        }
                    }
                    studentSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, studentNames));
                });

        btnSave.setOnClickListener(v -> {
            btnSave.setEnabled(false);
            String selectedName = studentSpinner.getText().toString();
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString() : "";
            String dateStr = etDate.getText().toString();

            if (selectedName.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty()) {
                btnSave.setEnabled(true);
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError(getString(R.string.invalid_amount));
                btnSave.setEnabled(true);
                return;
            }

            Timestamp timestamp;
            try {
                Date parsedDate = sdf.parse(dateStr);
                timestamp = (parsedDate != null) ? new Timestamp(parsedDate) : Timestamp.now();
            } catch (Exception e) {
                timestamp = Timestamp.now();
            }

            String method = "Cash";
            int id = rgMethod.getCheckedRadioButtonId();
            if (id == R.id.rb_bkash) method = "bKash";
            else if (id == R.id.rb_nagad) method = "Nagad";

            if (existingTx != null) {
                updateExistingTransaction(dialog, existingTx, amount, method, timestamp);
            } else {
                saveNewTransaction(dialog, studentMap.get(selectedName), amount, method, selectedName, timestamp);
            }
        });

        dialog.show();
    }

    private void saveNewTransaction(BottomSheetDialog dialog, DocumentSnapshot studentDoc, double amount, String method, String studentName, Timestamp timestamp) {
        if (studentDoc == null) {
            Toast.makeText(getContext(), R.string.student_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        String studentId = studentDoc.getId();

        // Robust Invoice Matching Logic
        db.collection("users").document(userId).collection("invoices")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(invoices -> {
                    DocumentSnapshot bestInvDoc = null;
                    
                    // Month of payment
                    Calendar txCal = Calendar.getInstance();
                    txCal.setTime(timestamp.toDate());
                    int txMonth = txCal.get(Calendar.MONTH);
                    int txYear = txCal.get(Calendar.YEAR);

                    for (DocumentSnapshot doc : invoices.getDocuments()) {
                        InvoiceModel inv = doc.toObject(InvoiceModel.class);
                        if (inv == null || "Paid".equalsIgnoreCase(inv.getStatus())) continue;

                        Calendar invCal = Calendar.getInstance();
                        invCal.setTime(inv.getMonth().toDate());

                        // Match exact month first
                        if (invCal.get(Calendar.MONTH) == txMonth && invCal.get(Calendar.YEAR) == txYear) {
                            bestInvDoc = doc;
                            break;
                        }
                        
                        // Fallback to oldest unpaid invoice
                        if (bestInvDoc == null) {
                            bestInvDoc = doc;
                        } else {
                            Calendar currentBestCal = Calendar.getInstance();
                            currentBestCal.setTime(bestInvDoc.toObject(InvoiceModel.class).getMonth().toDate());
                            if (invCal.before(currentBestCal)) {
                                bestInvDoc = doc;
                            }
                        }
                    }

                    String invoiceId = null;
                    String batchId = null;
                    if (bestInvDoc != null) {
                        invoiceId = bestInvDoc.getId();
                        InvoiceModel matchedInv = bestInvDoc.toObject(InvoiceModel.class);
                        batchId = matchedInv.getBatchId();
                        
                        double newPaid = matchedInv.getPaidAmount() + amount;
                        String newStatus = newPaid >= matchedInv.getAmount() ? "Paid" : "Due";
                        bestInvDoc.getReference().update("paidAmount", newPaid, "status", newStatus);
                    }

                    String txId = db.collection("users").document(userId).collection("transactions").document().getId();
                    TransactionModel tx = new TransactionModel(txId, studentId, studentName, batchId, invoiceId, amount, method, timestamp, "");

                    db.collection("users").document(userId).collection("transactions").document(txId).set(tx)
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                Toast.makeText(getContext(), R.string.payment_recorded, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), R.string.save_failed, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                });
    }

    private void updateExistingTransaction(BottomSheetDialog dialog, TransactionModel tx, double newAmount, String newMethod, Timestamp timestamp) {
        double diff = newAmount - tx.getAmount();

        if (tx.getInvoiceId() != null && diff != 0) {
            db.collection("users").document(userId).collection("invoices").document(tx.getInvoiceId())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            InvoiceModel inv = doc.toObject(InvoiceModel.class);
                            if (inv == null) return;

                            double updatedPaid = inv.getPaidAmount() + diff;
                            String newStatus = updatedPaid >= inv.getAmount() ? "Paid" : "Due";
                            doc.getReference().update("paidAmount", updatedPaid, "status", newStatus);
                        }
                    });
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", newAmount);
        updates.put("method", newMethod);
        updates.put("timestamp", timestamp);

        db.collection("users").document(userId).collection("transactions").document(tx.getId())
                .update(updates).addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), R.string.transaction_updated, Toast.LENGTH_SHORT).show();
                });
    }

    private void generatePdfInvoice(TransactionModel tx) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 450, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Header
        paint.setColor(Color.BLACK);
        paint.setTextSize(14f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        int x = 10, y = 30;
        canvas.drawText("TEACHSYNC - RECEIPT", x, y, paint);

        // Receipt Info
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10f);
        y += 30;
        String shortId = tx.getId() != null && tx.getId().length() >= 8 ? tx.getId().substring(0, 8).toUpperCase() : "N/A";
        canvas.drawText("Receipt ID: " + shortId, x, y, paint);
        y += 20;
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String dateStr = tx.getTimestamp() != null ? sdf.format(tx.getTimestamp().toDate()) : "N/A";
        canvas.drawText("Date: " + dateStr, x, y, paint);

        // Separator
        y += 20;
        paint.setStrokeWidth(1f);
        canvas.drawLine(x, y, 290, y, paint);

        // Bill To
        y += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Bill To:", x, y, paint);
        y += 20;
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(tx.getStudentName() != null ? tx.getStudentName() : "Unknown Student", x, y, paint);

        // Table Header
        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Description", x, y, paint);
        canvas.drawText("Amount", 220, y, paint);
        y += 10;
        canvas.drawLine(x, y, 290, y, paint);

        // Table Row
        y += 30;
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Tuition Fee Payment", x, y, paint);
        canvas.drawText(currencySymbol + " " + (int)tx.getAmount(), 220, y, paint);

        // Footer Separator
        y += 40;
        canvas.drawLine(x, y, 290, y, paint);

        // Total
        y += 25;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, View.GONE == 1 ? Typeface.BOLD : Typeface.BOLD)); // dummy condition to keep formatting
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Total Paid:", x, y, paint);
        canvas.drawText(currencySymbol + " " + (int)tx.getAmount(), 220, y, paint);

        // Payment Method
        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextSize(9f);
        canvas.drawText("Payment Method: " + (tx.getMethod() != null ? tx.getMethod() : "N/A"), x, y, paint);

        // Thank You Note
        y += 40;
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Thank you for choosing TeachSync!", 150, y, paint);

        document.finishPage(page);

        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) {
            Toast.makeText(getContext(), R.string.storage_unavailable, Toast.LENGTH_SHORT).show();
            document.close();
            return;
        }

        String fileName = "Receipt_" + (tx.getId() != null && tx.getId().length() >= 5 ? tx.getId().substring(0, 5) : "DOC") + ".pdf";
        File pdfFile = new File(dir, fileName);

        try {
            document.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(getContext(), R.string.pdf_generated, Toast.LENGTH_SHORT).show();
            openPdf(pdfFile);
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF: " + e.getMessage());
            Toast.makeText(getContext(), getString(R.string.save_pdf_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }

    private void openPdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF: " + e.getMessage());
            Toast.makeText(getContext(), R.string.no_pdf_viewer, Toast.LENGTH_LONG).show();
        }
    }
}
