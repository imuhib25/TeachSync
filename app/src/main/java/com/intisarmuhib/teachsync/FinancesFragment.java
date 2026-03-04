package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    private FirebaseFirestore db;
    private String userId;
    private String currencySymbol = "৳";

    private final List<TransactionModel> transactionList = new ArrayList<>();
    private final List<BatchFinanceModel> batchFinanceList = new ArrayList<>();
    
    private TransactionAdapter transactionAdapter;
    private BatchFinanceAdapter batchFinanceAdapter;

    private ListenerRegistration transactionsListener;
    private ListenerRegistration invoicesListener;

    private double totalCollectedThisMonth = 0;
    private double totalExpectedThisMonth = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finances, container, false);

        initViews(view);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        loadCurrency();
        updateLabels();
        
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
        
        batchFinanceAdapter = new BatchFinanceAdapter(batchFinanceList, currencySymbol);
        rvBatchFinance.setAdapter(batchFinanceAdapter);

        loadFinanceData();

        fabAddFinance.setOnClickListener(v -> showAddTransactionDialog());

        return view;
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
        tvMonthLabel.setText(sdf.format(Calendar.getInstance().getTime()));
    }

    private void loadFinanceData() {
        if (userId == null) return;

        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        transactionsListener = db.collection("users").document(userId).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    transactionList.clear();
                    totalCollectedThisMonth = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        TransactionModel tx = doc.toObject(TransactionModel.class);
                        if (tx != null) {
                            tx.setId(doc.getId());
                            transactionList.add(tx);
                            
                            if (tx.getTimestamp() != null) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(tx.getTimestamp().toDate());
                                if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                                    totalCollectedThisMonth += tx.getAmount();
                                }
                            }
                        }
                    }
                    tvCollected.setText(String.format(Locale.getDefault(), "%s %d", currencySymbol, (int) totalCollectedThisMonth));
                    transactionAdapter.notifyDataSetChanged();
                    calculateOverallProgress();
                });

        invoicesListener = db.collection("users").document(userId).collection("invoices")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    
                    double totalDue = 0;
                    double totalOverdue = 0;
                    totalExpectedThisMonth = 0;
                    
                    Map<String, BatchFinanceModel> batchMap = new HashMap<>();
                    Map<String, Set<String>> batchStudentsMap = new HashMap<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        InvoiceModel inv = doc.toObject(InvoiceModel.class);
                        if (inv != null) {
                            // Filter invoices by current month to get accurate "Expected Monthly"
                            Calendar invMonth = Calendar.getInstance();
                            if (inv.getMonth() != null) {
                                invMonth.setTime(inv.getMonth().toDate());
                                if (invMonth.get(Calendar.MONTH) == currentMonth && invMonth.get(Calendar.YEAR) == currentYear) {
                                    totalExpectedThisMonth += inv.getAmount();
                                }
                            }

                            if ("Due".equals(inv.getStatus())) totalDue += inv.getDueAmount();
                            else if ("Overdue".equals(inv.getStatus())) totalOverdue += inv.getDueAmount();

                            Set<String> students = batchStudentsMap.get(inv.getBatchId());
                            if (students == null) {
                                students = new HashSet<>();
                                batchStudentsMap.put(inv.getBatchId(), students);
                            }
                            students.add(inv.getStudentId());

                            BatchFinanceModel bfm = batchMap.get(inv.getBatchId());
                            if (bfm == null) {
                                bfm = new BatchFinanceModel(inv.getBatchId(), inv.getBatchName(), 
                                        inv.getPaidAmount(), inv.getDueAmount(), 1);
                            } else {
                                bfm = new BatchFinanceModel(inv.getBatchId(), inv.getBatchName(),
                                        bfm.getCollectedAmount() + inv.getPaidAmount(),
                                        bfm.getDueAmount() + inv.getDueAmount(),
                                        students.size());
                            }
                            batchMap.put(inv.getBatchId(), bfm);
                        }
                    }

                    tvDue.setText(String.format(Locale.getDefault(), "%s %d", currencySymbol, (int) totalDue));
                    tvOverdue.setText(String.format(Locale.getDefault(), "%s %d", currencySymbol, (int) totalOverdue));
                    tvForecast.setText(String.format(Locale.getDefault(), "%s %s %d", getString(R.string.expected), currencySymbol, (int) totalExpectedThisMonth));
                    
                    batchFinanceList.clear();
                    for (BatchFinanceModel model : batchMap.values()) {
                        Set<String> students = batchStudentsMap.get(model.getBatchId());
                        if (students != null) {
                            batchFinanceList.add(new BatchFinanceModel(model.getBatchId(), model.getBatchName(), 
                                model.getCollectedAmount(), model.getDueAmount(), students.size()));
                        }
                    }
                    batchFinanceAdapter.notifyDataSetChanged();
                    calculateOverallProgress();
                });
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
    public void onDestroyView() {
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
                            Double currentPaid = doc.getDouble("paidAmount");
                            Double total = doc.getDouble("amount");
                            if (currentPaid == null) currentPaid = 0.0;
                            if (total == null) total = 0.0;
                            
                            double newPaid = Math.max(0, currentPaid - tx.getAmount());
                            String newStatus = newPaid < total ? "Due" : "Paid";
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

        db.collection("users").document(userId).collection("invoices")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(invoices -> {
                    DocumentSnapshot invDoc = null;
                    for (DocumentSnapshot doc : invoices.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"Paid".equals(status)) {
                            invDoc = doc;
                            break;
                        }
                    }

                    String invoiceId = null;
                    String batchId = null;
                    if (invDoc != null) {
                        invoiceId = invDoc.getId();
                        batchId = invDoc.getString("batchId");
                        Double currentPaid = invDoc.getDouble("paidAmount");
                        Double total = invDoc.getDouble("amount");
                        if (currentPaid == null) currentPaid = 0.0;
                        if (total == null) total = 0.0;
                        
                        double newPaid = currentPaid + amount;
                        invDoc.getReference().update("paidAmount", newPaid, "status", newPaid >= total ? "Paid" : invDoc.getString("status"));
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
                            Double currentPaid = doc.getDouble("paidAmount");
                            Double total = doc.getDouble("amount");
                            if (currentPaid == null) currentPaid = 0.0;
                            if (total == null) total = 0.0;
                            
                            double updatedPaid = currentPaid + diff;
                            doc.getReference().update("paidAmount", updatedPaid, "status", updatedPaid >= total ? "Paid" : "Due");
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
