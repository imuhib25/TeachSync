package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private TextView welcomeText, tvTotalStudents, tvTotalStudentsMonth, tvMonthlyEarnings, tvMonthlyTarget, tvTodayClasses;
    private ProgressBar progressMonthlyEarnings;
    private ImageView profilePic, btnNotifications, btnInfo;
    private TextView btnClearActivity;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    public static String userId;
    private List<ActivityModel> recentActivities = new ArrayList<>();
    private RecyclerView rvActivity;
    private ActivityAdapter activityAdapter;
    private String currencySymbol = "৳";
    private AdView mAdView;

    private ListenerRegistration transactionsListener;
    private ListenerRegistration invoicesListener;
    private ListenerRegistration batchesRecentListener;
    private ListenerRegistration studentsRecentListener;
    private ListenerRegistration classesRecentListener;
    private ListenerRegistration transactionsRecentListener;
    private ListenerRegistration totalStudentsListener;
    private ListenerRegistration todayClassesListener;
    private ListenerRegistration userListener;

    private double currentMonthCollected = 0;
    private double currentMonthExpected = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        welcomeText = view.findViewById(R.id.welcome_text);
        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvTotalStudentsMonth = view.findViewById(R.id.tvTotalStudentsMonth);
        tvMonthlyEarnings = view.findViewById(R.id.tvMonthlyEarnings);
        tvMonthlyTarget = view.findViewById(R.id.tvMonthlyTarget);
        tvTodayClasses = view.findViewById(R.id.tvTodayClasses);
        progressMonthlyEarnings = view.findViewById(R.id.progressMonthlyEarnings);
        profilePic = view.findViewById(R.id.profile_pic);
        btnNotifications = view.findViewById(R.id.btn_notifications);
        btnInfo = view.findViewById(R.id.btn_info);
        btnClearActivity = view.findViewById(R.id.btn_clear_activity);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize and load Banner Ad
        mAdView = view.findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        loadCurrency();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();

            DocumentReference documentReference = firestore.collection("users").document(userId);
            userListener = documentReference.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null || !isAdded()) return;
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String fName = documentSnapshot.getString("fName");
                    welcomeText.setText(fName != null ? fName : "User");
                    
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).circleCrop().into(profilePic);
                    }
                }
            });
        }

        btnInfo.setOnClickListener(v -> showCreditsDialog());
        btnNotifications.setOnClickListener(v -> showNotificationsDialog());
        btnClearActivity.setOnClickListener(v -> clearActivity());

        rvActivity = view.findViewById(R.id.recyclerRecentActivity);
        activityAdapter = new ActivityAdapter(recentActivities);
        rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivity.setAdapter(activityAdapter);

        updateRecentActivity();
        updateTotalStudents();
        loadFinanceData();
        updateTodayClasses();
        
        return view;
    }

    private void loadCurrency() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currencySymbol = prefs.getString("currency_symbol", "৳");
    }

    private void loadFinanceData() {
        if (userId == null) return;
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        transactionsListener = firestore.collection("users").document(userId).collection("transactions")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    currentMonthCollected = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        TransactionModel tx = doc.toObject(TransactionModel.class);
                        if (tx != null && tx.getTimestamp() != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(tx.getTimestamp().toDate());
                            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                                currentMonthCollected += tx.getAmount();
                            }
                        }
                    }
                    if (tvMonthlyEarnings != null) {
                        tvMonthlyEarnings.setText(currencySymbol + String.format(Locale.getDefault(), "%.0f", currentMonthCollected));
                    }
                    updateProgressBar();
                });

        invoicesListener = firestore.collection("users").document(userId).collection("invoices")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    currentMonthExpected = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        InvoiceModel inv = doc.toObject(InvoiceModel.class);
                        if (inv != null && inv.getMonth() != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(inv.getMonth().toDate());
                            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                                currentMonthExpected += inv.getAmount();
                            }
                        }
                    }
                    if (tvMonthlyTarget != null) {
                        tvMonthlyTarget.setText("Target: " + currencySymbol + String.format(Locale.getDefault(), "%.0f", currentMonthExpected));
                    }
                    updateProgressBar();
                });
    }

    private void updateProgressBar() {
        if (currentMonthExpected > 0) {
            int progress = (int) ((currentMonthCollected / currentMonthExpected) * 100);
            if (progressMonthlyEarnings != null) {
                progressMonthlyEarnings.setProgress(Math.min(progress, 100));
            }
        } else {
            if (progressMonthlyEarnings != null) progressMonthlyEarnings.setProgress(0);
        }
    }

    private void clearActivity() {
        if (getContext() == null) return;
        
        recentActivities.clear();
        activityAdapter.notifyDataSetChanged();
        
        getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_cleared_activities", System.currentTimeMillis())
                .apply();
                
        Toast.makeText(getContext(), "Activity cleared", Toast.LENGTH_SHORT).show();
    }

    private void showCreditsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_credits);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.findViewById(R.id.btnCreditsClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showNotificationsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_notifications_list);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        RecyclerView rvNotif = dialog.findViewById(R.id.rvNotifications);
        rvNotif.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<ActivityModel> appNotifications = new ArrayList<>();
        ActivityAdapter notifAdapter = new ActivityAdapter(appNotifications);
        rvNotif.setAdapter(notifAdapter);

        appNotifications.add(new ActivityModel("App Update: v1.0.1 is now available", ""));
        fetchUpcomingClassNotifications(notifAdapter);

        dialog.findViewById(R.id.btnNotifClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchUpcomingClassNotifications(ActivityAdapter adapter) {
        if (userId == null) return;
        Calendar now = Calendar.getInstance();
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now.getTime());

        firestore.collection("users").document(userId).collection("classes")
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(value -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ClassModel classModel = doc.toObject(ClassModel.class);
                        if (classModel != null) {
                            adapter.addActivity(new ActivityModel("Upcoming Class: " + classModel.getTopic() + " at " + classModel.getClassTime(), ""));
                        }
                    }
                });
    }

    private void updateTotalStudents() {
        if (userId == null) return;
        totalStudentsListener = firestore.collection("users").document(userId).collection("students")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;

                    int totalStudents = value.size();
                    int thisMonth = 0;
                    Calendar now = Calendar.getInstance();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp("createdAt");
                        if (timestamp != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(timestamp.toDate());
                            if (cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                                thisMonth++;
                            }
                        }
                    }

                    if (tvTotalStudents != null) tvTotalStudents.setText(String.valueOf(totalStudents));
                    if (tvTotalStudentsMonth != null) tvTotalStudentsMonth.setText("+" + thisMonth + " this month");
                });
    }

    private void updateTodayClasses() {
        if (userId == null) return;
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        todayClassesListener = firestore.collection("users").document(userId).collection("classes")
                .whereEqualTo("date", todayDate)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    if (tvTodayClasses != null) tvTodayClasses.setText(String.valueOf(value.size()));
                });
    }

    private void updateRecentActivity(){
        if (userId == null) return;
        
        batchesRecentListener = firestore.collection("users").document(userId).collection("batches")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(3)
                .addSnapshotListener((value, error) -> processChanges(value, "New Batch: ", "name", "createdAt"));

        studentsRecentListener = firestore.collection("users").document(userId).collection("students")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(3)
                .addSnapshotListener((value, error) -> processChanges(value, "New Student: ", "name", "createdAt"));

        classesRecentListener = firestore.collection("users").document(userId).collection("classes")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(3)
                .addSnapshotListener((value, error) -> processChanges(value, "Class Added: ", "topic", "createdAt"));

        transactionsRecentListener = firestore.collection("users").document(userId).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    long lastCleared = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getLong("last_cleared_activities", 0);
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            TransactionModel transaction = dc.getDocument().toObject(TransactionModel.class);
                            if (transaction.getTimestamp() != null && transaction.getTimestamp().toDate().getTime() <= lastCleared) {
                                continue;
                            }
                            recentActivities.add(0, new ActivityModel("Received from " + transaction.getStudentName(), "+" + currencySymbol + (int)transaction.getAmount()));
                            activityAdapter.notifyItemInserted(0);
                        }
                    }
                });
    }

    private void processChanges(com.google.firebase.firestore.QuerySnapshot value, String prefix, String field, String timeField) {
        if (value != null && isAdded() && getContext() != null) {
            long lastCleared = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getLong("last_cleared_activities", 0);
            for (DocumentChange dc : value.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    Timestamp ts = dc.getDocument().getTimestamp(timeField);
                    if (ts != null && ts.toDate().getTime() <= lastCleared) {
                        continue;
                    }
                    String content = prefix + dc.getDocument().getString(field);
                    recentActivities.add(0, new ActivityModel(content, ""));
                    activityAdapter.notifyItemInserted(0);
                }
            }
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
        if (batchesRecentListener != null) batchesRecentListener.remove();
        if (studentsRecentListener != null) studentsRecentListener.remove();
        if (classesRecentListener != null) classesRecentListener.remove();
        if (transactionsRecentListener != null) transactionsRecentListener.remove();
        if (totalStudentsListener != null) totalStudentsListener.remove();
        if (todayClassesListener != null) todayClassesListener.remove();
        if (userListener != null) userListener.remove();
    }
}
