package com.example.meetingassistant;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meetingassistant.calendar.CustomCalendarView;
import com.example.meetingassistant.database.AppDatabase;
import com.example.meetingassistant.database.Meeting;
import com.example.meetingassistant.database.MeetingNotification;
import com.example.meetingassistant.database.NotificationTimeAdapter;
import com.example.meetingassistant.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

public class AddMeetingActivity extends AppCompatActivity {
    private EditText editTextFirstName, editTextLastName, editTextStreet, editTextHouseNumber, editTextCity;
    private TextView errorFirstName, errorLastName, errorStreet, errorHouseNumber, errorCity;
    private CustomCalendarView calendarView;
    private TimePicker timePicker;
    private Button buttonAddMeeting, buttonCancel;
    private EditText editTextNotificationTime;
    private Button buttonAddNotification;
    private RecyclerView recyclerViewNotifications;
    private NotificationTimeAdapter notificationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meeting);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Dodaj spotkanie");
        }

        initializeViews();
        setupListeners();
        setupNotificationsList();
        loadMeetings();
    }

    private void initializeViews() {
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextStreet = findViewById(R.id.editTextStreet);
        editTextHouseNumber = findViewById(R.id.editTextHouseNumber);
        editTextCity = findViewById(R.id.editTextCity);
        
        errorFirstName = findViewById(R.id.errorFirstName);
        errorLastName = findViewById(R.id.errorLastName);
        errorStreet = findViewById(R.id.errorStreet);
        errorHouseNumber = findViewById(R.id.errorHouseNumber);
        errorCity = findViewById(R.id.errorCity);
        
        calendarView = findViewById(R.id.calendarView);
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        buttonAddMeeting = findViewById(R.id.buttonAddMeeting);
        buttonCancel = findViewById(R.id.buttonCancel);

        editTextNotificationTime = findViewById(R.id.editTextNotificationTime);
        buttonAddNotification = findViewById(R.id.buttonAddNotification);
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
    }

    private void setupNotificationsList() {
        notificationAdapter = new NotificationTimeAdapter();
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);

        notificationAdapter.setOnDeleteListener(position -> {
            List<Integer> currentTimes = new ArrayList<>(notificationAdapter.getNotificationTimes());
            currentTimes.remove(position);
            notificationAdapter.updateNotifications(currentTimes);
        });
    }

    private void setupListeners() {
        buttonAddMeeting.setOnClickListener(v -> saveMeeting());
        buttonCancel.setOnClickListener(v -> finish());
        
        buttonAddNotification.setOnClickListener(v -> {
            String minutesStr = editTextNotificationTime.getText().toString();
            if (!TextUtils.isEmpty(minutesStr)) {
                try {
                    int minutes = Integer.parseInt(minutesStr);
                    if (minutes > 0) {
                        List<Integer> currentTimes = new ArrayList<>(notificationAdapter.getNotificationTimes());
                        currentTimes.add(minutes);
                        notificationAdapter.updateNotifications(currentTimes);
                        editTextNotificationTime.setText("");
                    } else {
                        Toast.makeText(this, "Podaj liczbę większą od 0", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Podaj prawidłową liczbę minut", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveMeeting() {
        if (validateInputs()) {
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    
                    Calendar selectedDate = calendarView.getSelectedDate();
                    if (selectedDate == null) {
                        selectedDate = Calendar.getInstance();
                    }
                    
                    selectedDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                    selectedDate.set(Calendar.MINUTE, timePicker.getMinute());

                    Meeting meeting = new Meeting(
                            editTextFirstName.getText().toString(),
                            editTextLastName.getText().toString(),
                            editTextStreet.getText().toString(),
                            editTextHouseNumber.getText().toString(),
                            editTextCity.getText().toString(),
                            selectedDate.getTimeInMillis(),
                            timePicker.getHour(),
                            timePicker.getMinute()
                    );

                    // Zapisz spotkanie i pobierz jego ID
                    long meetingId = db.meetingDao().insertMeeting(meeting);

                    // Zapisz powiadomienia
                    for (int minutes : notificationAdapter.getNotificationTimes()) {
                        MeetingNotification notification = new MeetingNotification(meetingId, minutes);
                        db.meetingNotificationDao().insertNotification(notification);
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(AddMeetingActivity.this,
                                "Spotkanie zostało dodane",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        if (e instanceof IllegalStateException && "DUPLICATE_MEETING".equals(e.getMessage())) {
                            Toast.makeText(AddMeetingActivity.this,
                                    "Masz już zaplanowane spotkanie z tą osobą w tym terminie",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AddMeetingActivity.this,
                                    "Wystąpił błąd podczas zapisywania",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (TextUtils.isEmpty(editTextFirstName.getText())) {
            errorFirstName.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorFirstName.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(editTextLastName.getText())) {
            errorLastName.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorLastName.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(editTextStreet.getText())) {
            errorStreet.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorStreet.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(editTextHouseNumber.getText())) {
            errorHouseNumber.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorHouseNumber.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(editTextCity.getText())) {
            errorCity.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorCity.setVisibility(View.GONE);
        }

        return isValid;
    }

    private void loadMeetings() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<Meeting> meetings = db.meetingDao().getAllMeetings();

                runOnUiThread(() -> {
                    if (meetings != null && !meetings.isEmpty()) {
                        calendarView.setMeetings(new HashSet<>(meetings));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


