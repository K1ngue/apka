package com.example.meetingassistant;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.meetingassistant.database.AppDatabase;
import com.example.meetingassistant.database.Meeting;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private RecyclerView meetingsRecyclerView;
    private MeetingMapAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Inicjalizacja widoków
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        meetingsRecyclerView = findViewById(R.id.meetingsRecyclerView);

        // Konfiguracja RecyclerView
        meetingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MeetingMapAdapter(this, new ArrayList<>());
        meetingsRecyclerView.setAdapter(adapter);

        // Konfiguracja nawigacji
        bottomNavigationView.setSelectedItemId(R.id.navigation_map);
        setupBottomNavigation();

        // Załaduj spotkania
        loadMeetings();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_meetings) {
                startActivity(new Intent(MapActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_map) {
                return true;
            }
            return false;
        });
    }

    private void loadMeetings() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<Meeting> meetings = db.meetingDao().getAllMeetings();
            
            runOnUiThread(() -> adapter.updateMeetings(meetings));
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeetings();
    }
} 