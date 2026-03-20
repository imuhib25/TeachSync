package com.intisarmuhib.teachsync;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TutorialActivity extends AppCompatActivity {

    private RecyclerView rvTutorials;
    private TutorialAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTutorials = findViewById(R.id.rvTutorials);
        rvTutorials.setLayoutManager(new LinearLayoutManager(this));

        List<TutorialItem> items = new ArrayList<>();
        items.add(new TutorialItem(
                "How to add a batch?",
                "Learn how to create and manage your student batches efficiently.",
                "https://youtu.be/example1",
                R.drawable.outline_school_24
        ));
        items.add(new TutorialItem(
                "Managing Student Attendance",
                "Step-by-step guide to mark and track student attendance.",
                "https://youtu.be/example2",
                R.drawable.baseline_dashboard_24
        ));
        items.add(new TutorialItem(
                "Generating Invoices",
                "How to generate and share professional invoices with parents.",
                "https://youtu.be/example3",
                R.drawable.baseline_payments_24
        ));
        items.add(new TutorialItem(
                "Tracking Payments",
                "Keep track of pending and completed payments from students.",
                "https://youtu.be/example4",
                R.drawable.baseline_payments_24
        ));

        adapter = new TutorialAdapter(items, item -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getVideoUrl()));
            startActivity(intent);
        });

        rvTutorials.setAdapter(adapter);
    }
}