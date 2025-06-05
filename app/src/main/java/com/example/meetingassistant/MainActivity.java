package com.example.meetingassistant;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meetingassistant.calendar.CustomCalendarView;
import com.example.meetingassistant.database.AppDatabase;
import com.example.meetingassistant.database.Meeting;
import com.example.meetingassistant.database.MeetingAdapter;
import com.example.meetingassistant.database.MeetingNotification;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewMeetings;
    private Button buttonAddMeeting;
    private CustomCalendarView calendarView;
    private Set<String> meetingDates;
    private Handler handler;
    private MeetingAdapter adapter;
    private BottomNavigationView bottomNavigationView;
    private final int UPDATE_INTERVAL = 60000; // 1 minuta w milisekundach
    private static final String PREFS_NAME = "MeetingAssistantPrefs";
    private static final String SAMPLE_MEETINGS_ADDED = "sampleMeetingsAdded";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja requestu o uprawnienia
        setupLocationPermissionRequest();

        // Inicjalizacja zmiennych
        meetingDates = new HashSet<>();
        handler = new Handler(Looper.getMainLooper());

        // Inicjalizacja kanału powiadomień
        NotificationHelper.createNotificationChannel(this);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        checkAndAddSampleMeetings();
        loadMeetings();
        startTimeUpdates();
        
        // Sprawdź uprawnienia do lokalizacji
        checkLocationPermissions();
    }

    private void initializeViews() {
        recyclerViewMeetings = findViewById(R.id.recyclerViewMeetings);
        buttonAddMeeting = findViewById(R.id.buttonAddMeeting);
        calendarView = findViewById(R.id.calendarView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_meetings);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_meetings) {
                return true;
            } else if (itemId == R.id.navigation_map) {
                startActivity(new Intent(MainActivity.this, MapActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        recyclerViewMeetings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MeetingAdapter(this, new ArrayList<>());
        recyclerViewMeetings.setAdapter(adapter);

        // Ustaw listener dla usuwania spotkań
        adapter.setOnDeleteClickListener(meeting -> {
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    
                    // Pobierz wszystkie powiadomienia dla tego spotkania
                    List<MeetingNotification> notifications = 
                        db.meetingNotificationDao().getNotificationsForMeeting(meeting.getId());
                    
                    // Anuluj wszystkie zaplanowane powiadomienia
                    for (MeetingNotification notification : notifications) {
                        Intent intent = new Intent(this, NotificationReceiver.class);
                        intent.putExtra("meeting_id", meeting.getId());
                        intent.putExtra("minutes_before", notification.getMinutesBefore());
                        
                        int notificationId = (int) (meeting.getId() * 100 + notification.getMinutesBefore());
                        
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            this,
                            notificationId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        if (alarmManager != null) {
                            alarmManager.cancel(pendingIntent);
                        }
                        pendingIntent.cancel();
                    }
                    
                    // Usuń spotkanie i jego powiadomienia z bazy danych
                    db.meetingNotificationDao().deleteAllNotificationsForMeeting(meeting.getId());
                    db.meetingDao().deleteMeetingById(meeting.getId());
                    
                    runOnUiThread(() -> {
                        // Odśwież listę spotkań i kalendarz
                        loadMeetings();
                        calendarView.removeMeeting(meeting);
                        Toast.makeText(MainActivity.this, 
                            "Spotkanie zostało usunięte", 
                            Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Wystąpił błąd podczas usuwania spotkania", 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    private String getDateString(int year, int month, int day) {
        return String.format("%d-%02d-%02d", year, month + 1, day);
    }

    private void setupListeners() {
        // Obsługa kliknięcia przycisku dodawania spotkania
        buttonAddMeeting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMeetingActivity.class);
            startActivity(intent);
        });

        // Obsługa kalendarza
        if (calendarView != null) {
            calendarView.setOnDateSelectedListener((year, month, day) -> {
                String dateStr = getDateString(year, month, day);
                if (meetingDates.contains(dateStr)) {
                    Toast.makeText(MainActivity.this,
                            "Masz zaplanowane spotkanie w tym dniu!",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startTimeUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadMeetings(); // Odśwież listę spotkań, co zaktualizuje pozostały czas
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeetings();
        scheduleNotifications();
    }

    private void checkAndAddSampleMeetings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean sampleMeetingsAdded = prefs.getBoolean(SAMPLE_MEETINGS_ADDED, false);

        if (!sampleMeetingsAdded) {
            new Thread(() -> {
                try {
                    addSampleMeetings();
                    prefs.edit().putBoolean(SAMPLE_MEETINGS_ADDED, true).apply();
                } catch (Exception e) {
                    Log.e("MainActivity", "Error adding sample meetings: " + e.getMessage());
                }
            }).start();
        }
    }

    private void addSampleMeetings() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Calendar calendar = Calendar.getInstance();
        
        // Resetujemy flagę w SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(SAMPLE_MEETINGS_ADDED, false).apply();
        
        // Spotkanie dzisiaj o 14:30
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 30);
        Meeting meeting1 = new Meeting(
            "Jan",
            "Kowalski",
            "Długa",
            "15",
            "Warszawa",
            calendar.getTimeInMillis(),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        );

        // Spotkanie dzisiaj o 16:00 (inna osoba)
        calendar.set(Calendar.HOUR_OF_DAY, 16);
        calendar.set(Calendar.MINUTE, 0);
        Meeting meeting2 = new Meeting(
            "Anna",
            "Nowak",
            "Krótka",
            "7/12",
            "Kraków",
            calendar.getTimeInMillis(),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        );

        // Spotkanie jutro o 10:00 (Jan Kowalski znowu, ale inny dzień)
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        Meeting meeting3 = new Meeting(
            "Jan",
            "Kowalski",
            "Szeroka",
            "22A",
            "Gdańsk",
            calendar.getTimeInMillis(),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        );

        // Spotkanie za tydzień
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 45);
        Meeting meeting4 = new Meeting(
            "Maria",
            "Zielińska",
            "Wąska",
            "3/4",
            "Poznań",
            calendar.getTimeInMillis(),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        );

        // Spotkanie za miesiąc
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        calendar.set(Calendar.MINUTE, 15);
        Meeting meeting5 = new Meeting(
            "Piotr",
            "Wiśniewski",
            "Polna",
            "8",
            "Wrocław",
            calendar.getTimeInMillis(),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        );

        try {
            db.meetingDao().insertMeeting(meeting1);
            db.meetingDao().insertMeeting(meeting2);
            db.meetingDao().insertMeeting(meeting3);
            db.meetingDao().insertMeeting(meeting4);
            db.meetingDao().insertMeeting(meeting5);
            
            Log.d("MainActivity", "Sample meetings added successfully");
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Dodano przykładowe spotkania", Toast.LENGTH_SHORT).show();
                loadMeetings(); // Odśwież listę spotkań
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error adding sample meetings: " + e.getMessage());
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Błąd podczas dodawania przykładowych spotkań", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadMeetings() {
        Log.d("MainActivity", "Loading meetings...");
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<Meeting> meetings = db.meetingDao().getAllMeetings();
                Log.d("MainActivity", "Meetings loaded from database: " + (meetings != null ? meetings.size() : 0));

                runOnUiThread(() -> {
                    try {
                        meetingDates.clear();

                        if (meetings != null && !meetings.isEmpty()) {
                            Log.d("MainActivity", "Processing " + meetings.size() + " meetings");
                            Set<Meeting> meetingsSet = new HashSet<>(meetings);
                            calendarView.setMeetings(meetingsSet);

                            for (Meeting m : meetings) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(m.getDate());
                                String dateStr = getDateString(
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                );
                                meetingDates.add(dateStr);
                                Log.d("MainActivity", "Meeting: " + m.getName() + " on " + dateStr);
                            }

                            adapter.updateMeetings(meetings);
                            Log.d("MainActivity", "Adapter updated with meetings");
                        } else {
                            Log.d("MainActivity", "No meetings to display");
                            adapter.updateMeetings(new ArrayList<>());
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error in UI update: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error loading meetings: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Aktualizacja kalendarza po usunięciu spotkania
    public void updateCalendarAfterDeletion() {
        loadMeetings();
    }

    private void scheduleNotifications() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<Meeting> meetings = db.meetingDao().getAllMeetings();

                if (meetings != null) {
                    long currentTime = System.currentTimeMillis();

                    for (Meeting meeting : meetings) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(meeting.getDate());
                        cal.set(Calendar.HOUR_OF_DAY, meeting.getHour());
                        cal.set(Calendar.MINUTE, meeting.getMinute());
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        long meetingTime = cal.getTimeInMillis();

                        // Pobierz powiadomienia dla tego spotkania
                        List<MeetingNotification> notifications = 
                            db.meetingNotificationDao().getNotificationsForMeeting(meeting.getId());

                        for (MeetingNotification notification : notifications) {
                            long notificationTime = meetingTime - (notification.getMinutesBefore() * 60 * 1000);
                            
                            // Planuj powiadomienie tylko jeśli jest w przyszłości
                            if (notificationTime > currentTime) {
                                Intent intent = new Intent(this, NotificationReceiver.class);
                                intent.putExtra("meeting_id", meeting.getId());
                                intent.putExtra("minutes_before", notification.getMinutesBefore());
                                
                                // Użyj kombinacji ID spotkania i czasu powiadomienia jako unikalnego ID
                                int notificationId = (int) (meeting.getId() * 100 + notification.getMinutesBefore());
                                
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    this,
                                    notificationId,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                );

                                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                                if (alarmManager != null) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        notificationTime,
                                        pendingIntent
                                    );
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error scheduling notifications: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void setupLocationPermissionRequest() {
        locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(
                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(
                    Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    // Dokładna lokalizacja przyznana
                    Toast.makeText(this, "Uprawnienia do dokładnej lokalizacji przyznane", Toast.LENGTH_SHORT).show();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // Przybliżona lokalizacja przyznana
                    Toast.makeText(this, "Uprawnienia do przybliżonej lokalizacji przyznane", Toast.LENGTH_SHORT).show();
                } else {
                    // Brak uprawnień
                    Toast.makeText(this, 
                        "Aplikacja potrzebuje uprawnień do lokalizacji aby pokazywać czas dojazdu", 
                        Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void checkLocationPermissions() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
        }
    }

    private boolean hasLocationPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestLocationPermissions() {
        if (shouldShowRequestPermissionRationale()) {
            // Pokaż wyjaśnienie, dlaczego potrzebujemy uprawnień
            Toast.makeText(this,
                "Aplikacja potrzebuje uprawnień do lokalizacji, aby obliczyć czas dojazdu na spotkanie",
                Toast.LENGTH_LONG).show();
        }
        
        locationPermissionRequest.launch(REQUIRED_PERMISSIONS);
    }

    private boolean shouldShowRequestPermissionRationale() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }
}
