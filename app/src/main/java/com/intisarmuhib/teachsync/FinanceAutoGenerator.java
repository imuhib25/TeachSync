package com.intisarmuhib.teachsync;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FinanceAutoGenerator {

    private static final String TAG = "FinanceAutoGenerator";

    /**
     * Robust incremental invoice generation. 
     * Ensures every batch enrollment for every student has a corresponding invoice for the current month.
     */
    public static void generateMonthlyInvoices(Context context, String userId) {
        if (userId == null) return;
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp currentMonthTs = new Timestamp(cal.getTime());

        String monthId = new SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(cal.getTime());

        Log.d(TAG, "Starting full invoice sync for: " + monthId);
        
        // Fetch all required data in parallel
        Tasks.whenAllSuccess(
                db.collection("users").document(userId).collection("batches").get(),
                db.collection("users").document(userId).collection("students").get(),
                db.collection("users").document(userId).collection("invoices").get()
        ).addOnSuccessListener(results -> {
            List<DocumentSnapshot> batchDocs = ((QuerySnapshot) results.get(0)).getDocuments();
            List<DocumentSnapshot> studentDocs = ((QuerySnapshot) results.get(1)).getDocuments();
            List<DocumentSnapshot> allInvoiceDocs = ((QuerySnapshot) results.get(2)).getDocuments();

            if (batchDocs.isEmpty() || studentDocs.isEmpty()) {
                Log.d(TAG, "No batches or students found. Sync aborted.");
                return;
            }

            // Map batch names to their document data for quick lookup
            Map<String, List<DocumentSnapshot>> nameToBatches = new HashMap<>();
            for (DocumentSnapshot doc : batchDocs) {
                String name = doc.getString("name");
                if (name != null) {
                    if (!nameToBatches.containsKey(name)) {
                        nameToBatches.put(name, new ArrayList<>());
                    }
                    nameToBatches.get(name).add(doc);
                }
            }

            // Track existing invoices to avoid duplicates and handle overdue status
            Set<String> existingInvoiceKeys = new HashSet<>();
            WriteBatch writeBatch = db.batch();
            boolean hasChanges = false;
            final int[] newInvoiceCount = {0};

            for (DocumentSnapshot doc : allInvoiceDocs) {
                InvoiceModel inv = doc.toObject(InvoiceModel.class);
                if (inv != null) {
                    existingInvoiceKeys.add(doc.getId());
                    
                    // Mark previous months' due invoices as Overdue
                    if (inv.getMonth() != null && "Due".equals(inv.getStatus())) {
                        Calendar invMonth = Calendar.getInstance();
                        invMonth.setTime(inv.getMonth().toDate());
                        if (invMonth.before(cal)) {
                            writeBatch.update(doc.getReference(), "status", "Overdue");
                            hasChanges = true;
                        }
                    }
                }
            }

            for (DocumentSnapshot studentDoc : studentDocs) {
                StudentModel student = studentDoc.toObject(StudentModel.class);
                if (student == null) continue;
                student.setId(studentDoc.getId());

                List<String> enrolledBatchNames = student.getBatches();
                if (enrolledBatchNames == null || enrolledBatchNames.isEmpty()) continue;

                for (String bName : enrolledBatchNames) {
                    List<DocumentSnapshot> matchingBatches = nameToBatches.get(bName);
                    if (matchingBatches == null) continue;

                    for (DocumentSnapshot bDoc : matchingBatches) {
                        String bId = bDoc.getId();
                        Double fee = bDoc.getDouble("paymentPerStudent");
                        if (fee == null) fee = 0.0;
                        
                        int cycleCount = bDoc.getLong("cycleCount") != null ? bDoc.getLong("cycleCount").intValue() : 1;

                        // Composite key ensures uniqueness per student, per batch, per month, per cycle
                        String invoiceId = monthId + "_c" + cycleCount + "_" + bId + "_" + student.getId();

                        if (!existingInvoiceKeys.contains(invoiceId)) {
                            InvoiceModel invoice = new InvoiceModel(
                                    invoiceId, student.getId(), student.getName(), bId, bName,
                                    fee, 0, "Due", currentMonthTs, Timestamp.now(), cycleCount);

                            writeBatch.set(db.collection("users").document(userId)
                                    .collection("invoices").document(invoiceId), invoice);
                            
                            hasChanges = true;
                            newInvoiceCount[0]++;
                            existingInvoiceKeys.add(invoiceId);
                            Log.d(TAG, "Queued invoice: " + invoiceId + " for " + student.getName() + " in " + bName);
                        }
                    }
                }
            }

            if (hasChanges) {
                writeBatch.commit().addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "Sync complete. Generated " + newInvoiceCount[0] + " new invoices."));
            } else {
                Log.d(TAG, "All invoices are already up to date.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Sync failed: ", e));
    }
}
