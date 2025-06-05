package com.example.meetingassistant.database;

import com.example.meetingassistant.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meetingassistant.EditMeetingActivity;
import com.example.meetingassistant.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {
    private List<Meeting> meetings;
    private SimpleDateFormat dateFormat;
    private Context context;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Meeting meeting);
    }

    public MeetingAdapter(Context context, List<Meeting> meetings) {
        this.context = context;
        this.meetings = new ArrayList<>(new HashSet<>(meetings));
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meeting, parent, false);
        return new MeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);

        // Format date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(meeting.getDate());
        String date = dateFormat.format(calendar.getTime());

        // Format time
        String time = String.format(Locale.getDefault(), "%02d:%02d", meeting.getHour(), meeting.getMinute());

        // Set text
        holder.textViewName.setText(meeting.getName() + " " + meeting.getSurname());
        holder.textViewDate.setText(date + " " + time);

        // Calculate and display remaining time with color
        String remainingTime = calculateRemainingTime(meeting, holder.textViewRemainingTime);
        holder.textViewRemainingTime.setText(remainingTime);

        // Obsługa przycisku więcej informacji
        holder.iconMore.setOnClickListener(v -> showMeetingDetails(v, meeting));

        // Obsługa przycisku edycji
        holder.iconEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditMeetingActivity.class);
            intent.putExtra("meeting_id", meeting.getId());
            context.startActivity(intent);
        });

        // Obsługa przycisku usuwania
        holder.iconDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(meeting);
            }
        });
    }

    private void showMeetingDetails(View view, Meeting meeting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_meeting_details, null);

        TextView textDetailFirstName = dialogView.findViewById(R.id.textDetailFirstName);
        TextView textDetailLastName = dialogView.findViewById(R.id.textDetailLastName);
        TextView textDetailStreet = dialogView.findViewById(R.id.textDetailStreet);
        TextView textDetailHouseNumber = dialogView.findViewById(R.id.textDetailHouseNumber);
        TextView textDetailCity = dialogView.findViewById(R.id.textDetailCity);
        TextView textDetailDate = dialogView.findViewById(R.id.textDetailDate);
        TextView textDetailTime = dialogView.findViewById(R.id.textDetailTime);
        TextView textDetailRemainingTime = dialogView.findViewById(R.id.textDetailRemainingTime);
        TextView textDetailNotifications = dialogView.findViewById(R.id.textDetailNotifications);
        Button buttonClose = dialogView.findViewById(R.id.buttonCloseDetails);

        textDetailFirstName.setText(meeting.getName());
        textDetailLastName.setText(meeting.getSurname());
        textDetailStreet.setText(meeting.getStreet());
        textDetailHouseNumber.setText(meeting.getHouseNumber());
        textDetailCity.setText(meeting.getCity());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(meeting.getDate());
        textDetailDate.setText(dateFormat.format(calendar.getTime()));
        textDetailTime.setText(String.format(Locale.getDefault(), "%02d:%02d", meeting.getHour(), meeting.getMinute()));
        
        // Zastosuj kolory do pozostałego czasu
        String remainingTime = calculateRemainingTime(meeting, textDetailRemainingTime);
        textDetailRemainingTime.setText(remainingTime);

        // Pobierz i wyświetl powiadomienia
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<MeetingNotification> notifications = db.meetingNotificationDao().getNotificationsForMeeting(meeting.getId());
                
                if (notifications != null && !notifications.isEmpty()) {
                    // Usuń duplikaty używając Set
                    Set<Integer> uniqueMinutes = new HashSet<>();
                    List<MeetingNotification> uniqueNotifications = new ArrayList<>();
                    
                    for (MeetingNotification notification : notifications) {
                        if (uniqueMinutes.add(notification.getMinutesBefore())) {
                            uniqueNotifications.add(notification);
                        }
                    }
                    
                    // Sortuj powiadomienia malejąco (od najwcześniejszego)
                    uniqueNotifications.sort((a, b) -> b.getMinutesBefore() - a.getMinutesBefore());
                    
                    StringBuilder notificationsText = new StringBuilder();
                    for (MeetingNotification notification : uniqueNotifications) {
                        if (notificationsText.length() > 0) {
                            notificationsText.append("\n");
                        }
                        int minutes = notification.getMinutesBefore();
                        if (minutes >= 60) {
                            int hours = minutes / 60;
                            int remainingMinutes = minutes % 60;
                            if (remainingMinutes == 0) {
                                notificationsText.append("• ").append(hours).append(hours == 1 ? " godzinę" : 
                                    hours < 5 ? " godziny" : " godzin").append(" przed spotkaniem");
                            } else {
                                notificationsText.append("• ").append(hours).append(hours == 1 ? " godzinę" : 
                                    hours < 5 ? " godziny" : " godzin").append(" i ").append(remainingMinutes)
                                    .append(" minut przed spotkaniem");
                            }
                        } else {
                            notificationsText.append("• ").append(minutes).append(" minut przed spotkaniem");
                        }
                    }
                    
                    ((MainActivity) context).runOnUiThread(() -> 
                        textDetailNotifications.setText(notificationsText.toString()));
                } else {
                    ((MainActivity) context).runOnUiThread(() -> 
                        textDetailNotifications.setText("Brak ustawionych powiadomień"));
                }
            } catch (Exception e) {
                Log.e("MeetingAdapter", "Error loading notifications: " + e.getMessage());
                ((MainActivity) context).runOnUiThread(() -> 
                    textDetailNotifications.setText("Błąd podczas ładowania powiadomień"));
            }
        }).start();

        AlertDialog dialog = builder.setView(dialogView).create();
        buttonClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String calculateRemainingTime(Meeting meeting, TextView textView) {
        Calendar meetingTime = Calendar.getInstance();
        meetingTime.setTimeInMillis(meeting.getDate());
        meetingTime.set(Calendar.HOUR_OF_DAY, meeting.getHour());
        meetingTime.set(Calendar.MINUTE, meeting.getMinute());
        meetingTime.set(Calendar.SECOND, 0);

        Calendar now = Calendar.getInstance();
        long diffInMillis = meetingTime.getTimeInMillis() - now.getTimeInMillis();

        int colorResId;
        if (diffInMillis < 0) {
            colorResId = R.color.meeting_past;
        } else if (diffInMillis < TimeUnit.HOURS.toMillis(24)) {
            colorResId = R.color.meeting_soon;
        } else {
            colorResId = R.color.meeting_future;
        }

        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(context, colorResId));
        }

        if (diffInMillis < 0) {
            return "Spotkanie już się odbyło";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        diffInMillis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        diffInMillis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

        StringBuilder timeRemaining = new StringBuilder("Pozostało: ");
        if (days > 0) {
            timeRemaining.append(days).append(" dni ");
        }
        if (hours > 0 || days > 0) {
            timeRemaining.append(hours).append(" godz ");
        }
        timeRemaining.append(minutes).append(" min");

        return timeRemaining.toString();
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public void updateMeetings(List<Meeting> newMeetings) {
        this.meetings = new ArrayList<>(newMeetings);
        notifyDataSetChanged();
    }

    static class MeetingViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDate;
        TextView textViewRemainingTime;
        ImageView iconMore;
        ImageView iconEdit;
        ImageView iconDelete;

        MeetingViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewRemainingTime = itemView.findViewById(R.id.textViewRemainingTime);
            iconMore = itemView.findViewById(R.id.iconMore);
            iconEdit = itemView.findViewById(R.id.iconEdit);
            iconDelete = itemView.findViewById(R.id.iconDelete);
        }
    }
}
