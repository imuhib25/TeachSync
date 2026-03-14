package com.intisarmuhib.teachsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingAdapter onboardingAdapter;
    private Button btnNext;
    private TextView tvSkip;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewPager), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);
        tabLayout = findViewById(R.id.tabLayout);

        setupOnboardingItems();

        viewPager.setAdapter(onboardingAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });

        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> items = new ArrayList<>();

        items.add(new OnboardingItem(
                R.drawable.outline_school_24,
                "Manage Your Classes",
                "Effortlessly organize your teaching schedule and student batches in one place."
        ));

        items.add(new OnboardingItem(
                R.drawable.baseline_dashboard_24,
                "Track Progress",
                "Keep a close eye on class attendance and monthly progress of your students."
        ));

        items.add(new OnboardingItem(
                R.drawable.baseline_payments_24,
                "Finance Management",
                "Simple and transparent tracking of student payments and your earnings."
        ));

        onboardingAdapter = new OnboardingAdapter(items);
    }

    private void finishOnboarding() {
        SharedPreferences preferences = getSharedPreferences("onboarding", MODE_PRIVATE);
        preferences.edit().putBoolean("isFirstRun", false).apply();

        startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}