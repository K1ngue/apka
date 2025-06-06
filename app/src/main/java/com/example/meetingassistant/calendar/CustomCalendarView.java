package com.example.meetingassistant.calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.meetingassistant.R;
import com.example.meetingassistant.database.Meeting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomCalendarView extends LinearLayout {
    private GridLayout daysGrid;
    private TextView monthText;
    private TextView yearText;
    private Calendar currentDate;
    private Map<String, List<Meeting>> meetingMap;
    private DayView selectedDayView;
    private OnDateSelectedListener dateSelectedListener;
    private float touchX;
    private static final int MIN_SWIPE_DISTANCE = 100;

    private final String[] MONTH_NAMES = {
        "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
        "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
    };

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int day);
    }

    public CustomCalendarView(Context context) {
        super(context);
        init();
    }

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.black));
        currentDate = Calendar.getInstance();
        meetingMap = new HashMap<>();

        // Dodaj nagłówek z miesiącem i rokiem
        addHeader();

        // Dodaj nagłówek z dniami tygodnia
        addWeekDayHeaders();

        // Siatka dni
        daysGrid = new GridLayout(getContext());
        daysGrid.setColumnCount(7);
        daysGrid.setRowCount(6);
        addView(daysGrid);

        updateCalendar();
    }

    private void addHeader() {
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER);
        headerLayout.setPadding(8, 20, 8, 20);

        // Przycisk poprzedniego miesiąca
        ImageButton prevButton = new ImageButton(getContext());
        prevButton.setImageResource(R.drawable.ic_arrow_left);
        prevButton.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
        prevButton.setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        // Kontener na tekst
        LinearLayout textContainer = new LinearLayout(getContext());
        textContainer.setOrientation(LinearLayout.HORIZONTAL);
        textContainer.setGravity(Gravity.CENTER);
        textContainer.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));

        // Tekst miesiąca
        monthText = new TextView(getContext());
        monthText.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        monthText.setTextSize(20);
        monthText.setGravity(Gravity.END);
        monthText.setPadding(0, 0, 8, 0);

        // Tekst roku
        yearText = new TextView(getContext());
        yearText.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        yearText.setTextSize(20);
        yearText.setGravity(Gravity.START);
        yearText.setPadding(8, 0, 0, 0);
        yearText.setOnClickListener(v -> showYearPicker());

        textContainer.addView(monthText);
        textContainer.addView(yearText);

        // Przycisk następnego miesiąca
        ImageButton nextButton = new ImageButton(getContext());
        nextButton.setImageResource(R.drawable.ic_arrow_right);
        nextButton.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
        nextButton.setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        headerLayout.addView(prevButton);
        headerLayout.addView(textContainer);
        headerLayout.addView(nextButton);

        addView(headerLayout);
    }

    private void showYearPicker() {
        NumberPicker yearPicker = new NumberPicker(getContext());
        yearPicker.setMinValue(currentDate.get(Calendar.YEAR) - 100);
        yearPicker.setMaxValue(currentDate.get(Calendar.YEAR) + 100);
        yearPicker.setValue(currentDate.get(Calendar.YEAR));

        new AlertDialog.Builder(getContext())
            .setTitle("Wybierz rok")
            .setView(yearPicker)
            .setPositiveButton("OK", (dialog, which) -> {
                currentDate.set(Calendar.YEAR, yearPicker.getValue());
                updateCalendar();
            })
            .setNegativeButton("Anuluj", null)
            .show();
    }

    private void addWeekDayHeaders() {
        GridLayout headerGrid = new GridLayout(getContext());
        headerGrid.setColumnCount(7);
        String[] weekDays = {"Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd"};

        for (String day : weekDays) {
            TextView textView = new TextView(getContext());
            textView.setText(day);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(8, 8, 8, 8);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            textView.setLayoutParams(params);

            headerGrid.addView(textView);
        }

        addView(headerGrid);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                float deltaX = ev.getX() - touchX;
                if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE) {
                    if (deltaX > 0) {
                        currentDate.add(Calendar.MONTH, -1);
                    } else {
                        currentDate.add(Calendar.MONTH, 1);
                    }
                    updateCalendar();
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void updateCalendar() {
        monthText.setText(MONTH_NAMES[currentDate.get(Calendar.MONTH)]);
        yearText.setText(String.valueOf(currentDate.get(Calendar.YEAR)));

        daysGrid.removeAllViews();

        Calendar calendar = (Calendar) currentDate.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int monthBeginningCell = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell);

        Calendar today = Calendar.getInstance();

        for (int i = 0; i < 42; i++) {
            DayView dayView = new DayView(getContext());
            dayView.setDate(calendar);
            dayView.setCurrentMonth(calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH));
            
            // Sprawdź czy to dzisiejszy dzień
            boolean isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
            
            if (isToday) {
                dayView.setIsToday(true);
            }

            // Sprawdź czy są spotkania na ten dzień
            String dateKey = String.format("%d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
            
            if (meetingMap.containsKey(dateKey) && !meetingMap.get(dateKey).isEmpty()) {
                dayView.setHasMeeting(true);
                List<Meeting> meetings = meetingMap.get(dateKey);
                if (meetings != null && !meetings.isEmpty()) {
                    // Znajdź najbliższe spotkanie
                    Meeting nextMeeting = meetings.get(0);
                    for (Meeting meeting : meetings) {
                        if (meeting.getDate() < nextMeeting.getDate()) {
                            nextMeeting = meeting;
                        }
                    }
                    dayView.setMeetingTime(nextMeeting.getDate());
                }
            }

            final Calendar finalCalendar = (Calendar) calendar.clone();
            dayView.setOnClickListener(v -> {
                if (selectedDayView != null) {
                    selectedDayView.setSelected(false);
                }
                dayView.setSelected(true);
                selectedDayView = dayView;
                
                if (dateSelectedListener != null) {
                    dateSelectedListener.onDateSelected(
                        finalCalendar.get(Calendar.YEAR),
                        finalCalendar.get(Calendar.MONTH),
                        finalCalendar.get(Calendar.DAY_OF_MONTH)
                    );
                }
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dayView.setLayoutParams(params);

            daysGrid.addView(dayView);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        invalidate();
        requestLayout();
    }

    public void setMeetings(Set<Meeting> meetings) {
        meetingMap.clear();
        for (Meeting meeting : meetings) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(meeting.getDate());
            String dateKey = String.format("%d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
            
            List<Meeting> dayMeetings = meetingMap.computeIfAbsent(dateKey, k -> new ArrayList<>());
            dayMeetings.add(meeting);
        }
        updateCalendar();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.dateSelectedListener = listener;
    }

    public Calendar getSelectedDate() {
        return currentDate;
    }

    public void removeMeeting(Meeting meeting) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(meeting.getDate());
        String key = String.format("%d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        );
        List<Meeting> dayMeetings = meetingMap.get(key);
        if (dayMeetings != null) {
            dayMeetings.remove(meeting);
            if (dayMeetings.isEmpty()) {
                meetingMap.remove(key);
            }
        }
        updateCalendar();
    }
} 