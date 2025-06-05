package com.example.meetingassistant;

import android.os.Bundle;
import android.util.Log;
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

public class EditMeetingActivity extends AppCompatActivity {
    private EditText editTextFirstName, editTextLastName, editTextStreet, editTextHouseNumber, editTextCity;
    private TextView errorFirstName, errorLastName, errorStreet, errorHouseNumber, errorCity;
    private TimePicker timePicker;
    private CustomCalendarView calendarView;
    private Calendar selectedDate;
    private Button buttonSaveMeeting, buttonCancel, buttonAddNotification;
    private EditText editTextMinutesBefore;
    private RecyclerView recyclerViewNotifications;
    private NotificationTimeAdapter notificationAdapter;
    private Meeting currentMeeting;
    private long meetingId;
    private List<MeetingNotification> currentNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_meeting);

        meetingId = getIntent().getLongExtra("meeting_id", -1);
        if (meetingId == -1) {
            Toast.makeText(this, "Błąd: nie znaleziono spotkania", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupNotificationsList();
        loadMeeting();
        loadAllMeetings();
        setupListeners();
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

        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        calendarView = findViewById(R.id.calendarView);
        buttonSaveMeeting = findViewById(R.id.buttonSaveMeeting);
        buttonCancel = findViewById(R.id.buttonCancel);
        buttonAddNotification = findViewById(R.id.buttonAddNotification);
        editTextMinutesBefore = findViewById(R.id.editTextMinutesBefore);
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);

        selectedDate = Calendar.getInstance();
    }

    private void setupNotificationsList() {
        notificationAdapter = new NotificationTimeAdapter();
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);
        currentNotifications = new ArrayList<>();

        notificationAdapter.setOnDeleteListener(position -> {
            if (position >= 0 && position < currentNotifications.size()) {
                MeetingNotification notificationToRemove = currentNotifications.get(position);
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    db.meetingNotificationDao().deleteNotification(notificationToRemove);
                    currentNotifications.remove(position);
                    runOnUiThread(() -> {
                        List<Integer> times = new ArrayList<>();
                        for (MeetingNotification notification : currentNotifications) {
                            times.add(notification.getMinutesBefore());
                        }
                        notificationAdapter.updateNotifications(times);
                    });
                }).start();
            }
        });
    }

    private void loadMeeting() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            currentMeeting = db.meetingDao().getMeetingById(meetingId);
            currentNotifications = db.meetingNotificationDao().getNotificationsForMeeting(meetingId);

            if (currentMeeting != null) {
                runOnUiThread(() -> {
                    editTextFirstName.setText(currentMeeting.getName());
                    editTextLastName.setText(currentMeeting.getSurname());
                    editTextStreet.setText(currentMeeting.getStreet());
                    editTextHouseNumber.setText(currentMeeting.getHouseNumber());
                    editTextCity.setText(currentMeeting.getCity());

                    Calendar meetingDate = Calendar.getInstance();
                    meetingDate.setTimeInMillis(currentMeeting.getDate());
                    selectedDate = meetingDate;

                    timePicker.setHour(currentMeeting.getHour());
                    timePicker.setMinute(currentMeeting.getMinute());

                    // Convert notifications to minutes list
                    List<Integer> times = new ArrayList<>();
                    for (MeetingNotification notification : currentNotifications) {
                        times.add(notification.getMinutesBefore());
                    }
                    notificationAdapter.updateNotifications(times);
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Nie znaleziono spotkania", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void loadAllMeetings() {
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

    private void setupListeners() {
        calendarView.setOnDateSelectedListener((year, month, day) -> {
            selectedDate.set(year, month, day);
        });

        buttonAddNotification.setOnClickListener(v -> addNotification());
        buttonSaveMeeting.setOnClickListener(v -> updateMeeting());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void addNotification() {
        String minutesStr = editTextMinutesBefore.getText().toString().trim();
        if (minutesStr.isEmpty()) {
            Toast.makeText(this, "Wprowadź liczbę minut", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int minutes = Integer.parseInt(minutesStr);
            if (minutes <= 0) {
                Toast.makeText(this, "Liczba minut musi być większa od 0", Toast.LENGTH_SHORT).show();
                return;
            }

            // Sprawdź czy takie powiadomienie już istnieje
            for (MeetingNotification existing : currentNotifications) {
                if (existing.getMinutesBefore() == minutes) {
                    Toast.makeText(this, "Powiadomienie dla " + minutes + " minut już istnieje", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            MeetingNotification notification = new MeetingNotification(meetingId, minutes);
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.meetingNotificationDao().insertNotification(notification);
                currentNotifications.add(notification);
                runOnUiThread(() -> {
                    List<Integer> times = new ArrayList<>();
                    for (MeetingNotification n : currentNotifications) {
                        times.add(n.getMinutesBefore());
                    }
                    notificationAdapter.updateNotifications(times);
                    editTextMinutesBefore.setText("");
                    Toast.makeText(this, "Dodano powiadomienie", Toast.LENGTH_SHORT).show();
                });
            }).start();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Wprowadź poprawną liczbę minut", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput() {
        boolean isValid = true;

        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String street = editTextStreet.getText().toString().trim();
        String houseNumber = editTextHouseNumber.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();

        if (firstName.isEmpty()) {
            errorFirstName.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorFirstName.setVisibility(View.GONE);
        }

        if (lastName.isEmpty()) {
            errorLastName.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorLastName.setVisibility(View.GONE);
        }

        if (street.isEmpty()) {
            errorStreet.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorStreet.setVisibility(View.GONE);
        }

        if (houseNumber.isEmpty()) {
            errorHouseNumber.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorHouseNumber.setVisibility(View.GONE);
        }

        if (city.isEmpty()) {
            errorCity.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            errorCity.setVisibility(View.GONE);
        }

        return isValid;
    }

    private void updateMeeting() {
        if (!validateInput()) {
            return;
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Wybierz datę spotkania!", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String street = editTextStreet.getText().toString().trim();
        String houseNumber = editTextHouseNumber.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        currentMeeting.setName(firstName);
        currentMeeting.setSurname(lastName);
        currentMeeting.setStreet(street);
        currentMeeting.setHouseNumber(houseNumber);
        currentMeeting.setCity(city);
        currentMeeting.setDate(selectedDate.getTimeInMillis());
        currentMeeting.setHour(hour);
        currentMeeting.setMinute(minute);

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.meetingDao().updateMeeting(currentMeeting);

                runOnUiThread(() -> {
                    Toast.makeText(EditMeetingActivity.this, "Spotkanie zostało zaktualizowane", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e("EditMeeting", "Error updating meeting: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                        Toast.makeText(EditMeetingActivity.this,
                                "Masz już zaplanowane spotkanie w tym terminie",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(EditMeetingActivity.this,
                                "Błąd podczas aktualizacji spotkania",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
}
