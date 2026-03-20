package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView welcomeText, tvTotalStudents, tvTotalStudentsMonth, tvMonthlyEarnings, tvMonthlyTarget, tvTodayClasses;
    private ProgressBar progressMonthlyEarnings;
    private ImageView profilePic;
    private ImageView btnNotifications, btnInfo, btnTutorial;
    private TextView btnClearActivity;

    private String userId;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String currencySymbol = "৳";

    private AdView mAdView;

    // Ongoing Class Views
    private View layoutOngoingClass;
    private TextView tvOngoingClassName, tvOngoingClassTime;
    private MaterialButton btnViewOngoing;

    // Upcoming Classes Views
    private View layoutUpcomingClasses;
    private RecyclerView rvUpcomingClasses;
    private DashboardClassAdapter upcomingClassAdapter;
    private List<ClassModel> upcomingClasses = new ArrayList<>();
    private TextView btnViewUpcomingAll;

    // Recent Activity Views
    private RecyclerView rvActivity;
    private ActivityAdapter activityAdapter;
    private List<ActivityModel> recentActivities = new ArrayList<>();

    // Active Batches Views
    private RecyclerView rvActiveBatches;
    private ActiveBatchAdapter activeBatchAdapter;
    private List<BatchModel> activeBatches = new ArrayList<>();
    private View btnViewAllBatches;

    private ListenerRegistration transactionsListener;
    private ListenerRegistration invoicesListener;
    private ListenerRegistration batchesRecentListener;
    private ListenerRegistration studentsRecentListener;
    private ListenerRegistration classesRecentListener;
    private ListenerRegistration transactionsRecentListener;
    private ListenerRegistration totalStudentsListener;
    private ListenerRegistration todayClassesListener;
    private ListenerRegistration userListener;
    private ListenerRegistration activeBatchesListener;
    private ListenerRegistration ongoingClassesListener;
    private ListenerRegistration upcomingClassesListener;
    private ListenerRegistration recentActivitiesListener;

    private double currentMonthCollected = 0;
    private double currentMonthExpected = 0;

    private Handler handler = new Handler();
    private Runnable ongoingClassRefresher;

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
        btnTutorial = view.findViewById(R.id.btn_tutorial);
        btnClearActivity = view.findViewById(R.id.btn_clear_activity);

        // Ongoing Class
        layoutOngoingClass = view.findViewById(R.id.layoutOngoingClass);
        tvOngoingClassName = view.findViewById(R.id.tvOngoingClassName);
        tvOngoingClassTime = view.findViewById(R.id.tvOngoingClassTime);
        btnViewOngoing = view.findViewById(R.id.btnViewOngoing);

        // Upcoming Classes
        layoutUpcomingClasses = view.findViewById(R.id.layoutUpcomingClasses);
        rvUpcomingClasses = view.findViewById(R.id.rvUpcomingClasses);
        btnViewUpcomingAll = view.findViewById(R.id.btnViewUpcomingAll);
        upcomingClassAdapter = new DashboardClassAdapter();
        rvUpcomingClasses.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvUpcomingClasses.setAdapter(upcomingClassAdapter);

        // Active Batches
        rvActiveBatches = view.findViewById(R.id.rvActiveBatches);
        btnViewAllBatches = view.findViewById(R.id.btnViewAllBatches);
        activeBatchAdapter = new ActiveBatchAdapter(activeBatches);
        activeBatchAdapter.setOnBatchActionListener(new ActiveBatchAdapter.OnBatchActionListener() {
            @Override
            public void onStartNewCycle(BatchModel batch) {
                handleStartNewCycle(batch);
            }

            @Override
            public void onCloseBatch(BatchModel batch) {
                handleCloseBatch(batch);
            }
        });
        rvActiveBatches.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvActiveBatches.setAdapter(activeBatchAdapter);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize and load Banner Ad
        mAdView = view.findViewById(R.id.adView);
        AdManager.initAd(getContext(), mAdView);

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
        btnTutorial.setOnClickListener(v -> startActivity(new Intent(getActivity(), TutorialActivity.class)));
        btnClearActivity.setOnClickListener(v -> clearActivity());
        btnViewAllBatches.setOnClickListener(v -> startActivity(new Intent(getActivity(), BatchesActivity.class)));
        btnViewOngoing.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MainActivity.class).putExtra("fragment", "schedule"));
        });
        btnViewUpcomingAll.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MainActivity.class).putExtra("fragment", "schedule"));
        });

        rvActivity = view.findViewById(R.id.recyclerRecentActivity);
        activityAdapter = new ActivityAdapter(recentActivities);
        rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivity.setAdapter(activityAdapter);

        updateRecentActivity();
        updateTotalStudents();
        loadFinanceData();
        updateTodayClasses();
        loadActiveBatches();
        loadUpcomingClasses();
        startOngoingClassTimer();

        checkFirstVisit();
        
        return view;
    }

    private void checkFirstVisit() {
        if (getContext() == null) return;
        SharedPreferences preferences = getContext().getSharedPreferences("onboarding", Context.MODE_PRIVATE);
        boolean isFirstDashboardVisit = preferences.getBoolean("isFirstDashboardVisit", true);
        if (isFirstDashboardVisit) {
            preferences.edit().putBoolean("isFirstDashboardVisit", false).apply();
            Intent intent = new Intent(getActivity(), TutorialActivity.class);
            intent.putExtra("isTutorial", true);
            startActivity(intent);
        }
    }

    private void handleStartNewCycle(BatchModel batch) {
        if (batch.isAutoSchedule()) {
            startAutoCycle(batch);
        } else {
            showManualCycleCalendar(batch);
        }
    }

    private void startAutoCycle(BatchModel batch) {
        WriteBatch writeBatch = firestore.batch();
        int newCycle = batch.getCycleCount() + 1;
        
        DocumentReference batchRef = firestore.collection("users").document(userId).collection("batches").document(batch.getId());
        writeBatch.update(batchRef, "cycleCount", newCycle, "currentMonthCount", 0);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeRange = timeFmt.format(batch.getStartTime().toDate()) + " - " + timeFmt.format(batch.getEndTime().toDate());

        List<Integer> selectedDays = batch.getSelectedDays();
        int classesAddedCount = 0;
        for (int i = 0; i < 60 && classesAddedCount < batch.getTotalMonthlyClasses(); i++) {
            if (selectedDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                classesAddedCount++;
                String classId = firestore.collection("users").document(userId).collection("classes").document().getId();
                ClassModel classModel = new ClassModel(
                        classId,
                        "New Cycle Class",
                        batch.getName(),
                        batch.getId(),
                        timeRange,
                        dateFormat.format(cal.getTime()),
                        String.valueOf(classesAddedCount),
                        false,
                        newCycle,
                        batch.getTotalMonthlyClasses(),
                        new Timestamp(Calendar.getInstance().getTime())
                );
                writeBatch.set(firestore.collection("users").document(userId).collection("classes").document(classId), classModel);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        writeBatch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "New cycle started for " + batch.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showManualCycleCalendar(BatchModel batch) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_manual_cycle_calendar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        MaterialCalendarView calendarView = dialog.findViewById(R.id.calendarManualCycle);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_MULTIPLE);
        TextView tvLimit = dialog.findViewById(R.id.tvCycleLimit);
        tvLimit.setText("Select exactly " + batch.getTotalMonthlyClasses() + " dates");

        dialog.findViewById(R.id.btnConfirmCycle).setOnClickListener(v -> {
            List<CalendarDay> selectedDates = calendarView.getSelectedDates();
            if (selectedDates.size() != batch.getTotalMonthlyClasses()) {
                Toast.makeText(getContext(), "Please select exactly " + batch.getTotalMonthlyClasses() + " dates", Toast.LENGTH_SHORT).show();
                return;
            }

            WriteBatch writeBatch = firestore.batch();
            int newCycle = batch.getCycleCount() + 1;
            
            DocumentReference batchRef = firestore.collection("users").document(userId).collection("batches").document(batch.getId());
            
            List<Timestamp> tsList = new ArrayList<>();
            for (CalendarDay d : selectedDates) tsList.add(new Timestamp(d.getDate()));
            
            writeBatch.update(batchRef, "cycleCount", newCycle, "currentMonthCount", 0, "manualDates", tsList);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String timeRange = timeFmt.format(batch.getStartTime().toDate()) + " - " + timeFmt.format(batch.getEndTime().toDate());

            int count = 0;
            for (CalendarDay day : selectedDates) {
                count++;
                String classId = firestore.collection("users").document(userId).collection("classes").document().getId();
                ClassModel classModel = new ClassModel(
                        classId,
                        "Manual Cycle Class",
                        batch.getName(),
                        batch.getId(),
                        timeRange,
                        dateFormat.format(day.getDate()),
                        String.valueOf(count),
                        false,
                        newCycle,
                        batch.getTotalMonthlyClasses(),
                        new Timestamp(Calendar.getInstance().getTime())
                );
                writeBatch.set(firestore.collection("users").document(userId).collection("classes").document(classId), classModel);
            }

            writeBatch.commit().addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "New cycle started for " + batch.getName(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void handleCloseBatch(BatchModel batch) {
        firestore.collection("users").document(userId).collection("batches").document(batch.getId())
                .update("archived", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), batch.getName() + " archived", Toast.LENGTH_SHORT).show();
                });
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
        
        firestore.collection("users").document(userId).collection("recent_activities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        recentActivities.clear();
                        activityAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Activity cleared", Toast.LENGTH_SHORT).show();
                    });
                });

        getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_cleared_activities", System.currentTimeMillis())
                .apply();
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

        appNotifications.add(new ActivityModel("App Up-to-date", "v1.0 is active"));
        notifAdapter.notifyDataSetChanged();
        
        // Load cycle notifications from Firestore
        if (userId != null) {
            firestore.collection("users").document(userId).collection("recent_activities")
                    .whereEqualTo("title", "Cycle Completed")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ActivityModel am = doc.toObject(ActivityModel.class);
                            if (am != null) appNotifications.add(am);
                        }
                        notifAdapter.notifyDataSetChanged();
                    });
        }
        
        loadUpcomingClassNotifications(appNotifications, notifAdapter);
        dialog.findViewById(R.id.btnNotifClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadUpcomingClassNotifications(List<ActivityModel> list, ActivityAdapter adapter) {
        if (userId == null) return;

        Calendar now = Calendar.getInstance();
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now.getTime());

        firestore.collection("users")
                .document(userId)
                .collection("classes")
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(value -> {
                    if (!isAdded()) return;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ClassModel classModel = doc.toObject(ClassModel.class);

                        if (classModel != null) {
                            String title = "Upcoming Class";
                            String desc = classModel.getTopic() + " at " + classModel.getClassTime();

                            list.add(new ActivityModel(title, desc));
                        }
                    }

                    adapter.notifyDataSetChanged();
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        todayClassesListener = firestore.collection("users").document(userId).collection("classes")
                .whereEqualTo("date", today)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    int count = value.size();
                    tvTodayClasses.setText(String.valueOf(count));
                });
    }

    private void loadActiveBatches() {
        if (userId == null) return;
        activeBatchesListener = firestore.collection("users").document(userId).collection("batches")
                .whereEqualTo("archived", false)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    activeBatches.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        BatchModel batch = doc.toObject(BatchModel.class);
                        if (batch != null) activeBatches.add(batch);
                    }
                    activeBatchAdapter.updateList(activeBatches);
                });
    }

    private void loadUpcomingClasses() {
        if (userId == null) return;
        
        upcomingClassesListener = firestore.collection("users").document(userId).collection("classes")
                .whereEqualTo("status", "scheduled")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    
                    List<ClassModel> allUpcoming = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ClassModel cm = doc.toObject(ClassModel.class);
                        if (cm != null && isUpcoming(cm)) {
                            allUpcoming.add(cm);
                        }
                    }
                    
                    Collections.sort(allUpcoming, (c1, c2) -> {
                        try {
                            SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
                            Date d1 = fullFmt.parse(c1.getDate() + " " + c1.getClassTime().split("-")[0].trim());
                            Date d2 = fullFmt.parse(c2.getDate() + " " + c2.getClassTime().split("-")[0].trim());
                            return d1.compareTo(d2);
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    
                    upcomingClasses.clear();
                    for (int i = 0; i < Math.min(allUpcoming.size(), 3); i++) {
                        upcomingClasses.add(allUpcoming.get(i));
                    }
                    
                    upcomingClassAdapter.setData(upcomingClasses);
                    layoutUpcomingClasses.setVisibility(upcomingClasses.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private boolean isUpcoming(ClassModel model) {
        if (model.getClassTime() == null || model.getDate() == null) return false;
        try {
            String startTimeStr = model.getClassTime().split("-")[0].trim();
            SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            Date startTime = fullFmt.parse(model.getDate() + " " + startTimeStr);
            return startTime != null && startTime.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private void startOngoingClassTimer() {
        ongoingClassRefresher = new Runnable() {
            @Override
            public void run() {
                checkOngoingClass();
                handler.postDelayed(this, 60000); // Check every minute
            }
        };
        handler.post(ongoingClassRefresher);
    }

    private void checkOngoingClass() {
        if (userId == null) return;
        
        SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String today = dateSdf.format(new Date());
        
        ongoingClassesListener = firestore.collection("users").document(userId).collection("classes")
                .whereEqualTo("date", today)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    processOngoingClasses(value.getDocuments());
                });
    }

    private void processOngoingClasses(List<DocumentSnapshot> docs) {
        SimpleDateFormat timeSdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        
        DocumentSnapshot ongoingClassDoc = null;
        int endsInMinutes = 0;

        for (DocumentSnapshot doc : docs) {
            String timeRange = doc.getString("classTime");
            if (timeRange != null && timeRange.contains("-")) {
                String[] parts = timeRange.split("-");
                if (parts.length == 2) {
                    try {
                        Date startTime = timeSdf.parse(parts[0].trim());
                        Date endTime = timeSdf.parse(parts[1].trim());
                        
                        Calendar startCal = Calendar.getInstance();
                        startCal.setTime(startTime);
                        int startMin = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE);
                        
                        Calendar endCal = Calendar.getInstance();
                        endCal.setTime(endTime);
                        int endMin = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE);
                        
                        if (nowMinutes >= startMin && nowMinutes < endMin) {
                            ongoingClassDoc = doc;
                            endsInMinutes = endMin - nowMinutes;
                            break;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (ongoingClassDoc != null) {
            ClassModel cm = ongoingClassDoc.toObject(ClassModel.class);
            layoutOngoingClass.setVisibility(View.VISIBLE);
            tvOngoingClassName.setText(cm.getBatch() + (cm.getTopic() != null ? " - " + cm.getTopic() : ""));
            
            if (endsInMinutes > 60) {
                tvOngoingClassTime.setText("Ends in " + (endsInMinutes/60) + "h " + (endsInMinutes%60) + "m");
            } else {
                tvOngoingClassTime.setText("Ends in " + endsInMinutes + " mins");
            }
        } else {
            layoutOngoingClass.setVisibility(View.GONE);
        }
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
        
        // Listen for cycle completion notifications
        recentActivitiesListener = firestore.collection("users").document(userId).collection("recent_activities")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    long lastCleared = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getLong("last_cleared_activities", 0);
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            ActivityModel am = dc.getDocument().toObject(ActivityModel.class);
                            if (am != null) {
                                if (am.getTimestamp() != null && am.getTimestamp().toDate().getTime() <= lastCleared) {
                                    continue;
                                }
                                recentActivities.add(0, am);
                                activityAdapter.notifyItemInserted(0);
                            }
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
        handler.removeCallbacks(ongoingClassRefresher);
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
        if (activeBatchesListener != null) activeBatchesListener.remove();
        if (ongoingClassesListener != null) ongoingClassesListener.remove();
        if (upcomingClassesListener != null) upcomingClassesListener.remove();
        if (recentActivitiesListener != null) recentActivitiesListener.remove();
    }
}
