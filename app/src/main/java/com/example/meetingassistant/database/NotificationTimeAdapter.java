package com.example.meetingassistant.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meetingassistant.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationTimeAdapter extends RecyclerView.Adapter<NotificationTimeAdapter.ViewHolder> {
    private List<Integer> notificationTimes;
    private OnDeleteListener onDeleteListener;

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public NotificationTimeAdapter() {
        this.notificationTimes = new ArrayList<>();
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_time, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int minutes = notificationTimes.get(position);
        holder.textViewTime.setText(formatTime(minutes));
        holder.buttonDelete.setOnClickListener(v -> {
            if (onDeleteListener != null) {
                onDeleteListener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationTimes.size();
    }

    public void updateNotifications(List<Integer> times) {
        this.notificationTimes = new ArrayList<>(times);
        notifyDataSetChanged();
    }

    public List<Integer> getNotificationTimes() {
        return new ArrayList<>(notificationTimes);
    }

    private String formatTime(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + (hours == 1 ? " godzina" : hours < 5 ? " godziny" : " godzin");
            } else {
                return hours + (hours == 1 ? " godzina " : hours < 5 ? " godziny " : " godzin ") +
                       remainingMinutes + (remainingMinutes == 1 ? " minuta" : remainingMinutes < 5 ? " minuty" : " minut");
            }
        } else {
            return minutes + (minutes == 1 ? " minuta" : minutes < 5 ? " minuty" : " minut");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTime;
        ImageButton buttonDelete;

        ViewHolder(View itemView) {
            super(itemView);
            textViewTime = itemView.findViewById(R.id.textViewNotificationTime);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteNotification);
        }
    }
} 