package com.example.meetingassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationTimeAdapter extends RecyclerView.Adapter<NotificationTimeAdapter.NotificationViewHolder> {
    private List<Integer> notifications;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public NotificationTimeAdapter() {
        this.notifications = new ArrayList<>();
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_time, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        int minutes = notifications.get(position);
        String text;
        if (minutes == 1) {
            text = "1 minuta przed spotkaniem";
        } else if (minutes % 10 >= 2 && minutes % 10 <= 4 && (minutes % 100 < 10 || minutes % 100 >= 20)) {
            text = minutes + " minuty przed spotkaniem";
        } else {
            text = minutes + " minut przed spotkaniem";
        }
        holder.textViewMinutesBefore.setText(text);
        
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<Integer> newNotifications) {
        this.notifications = new ArrayList<>(newNotifications);
        notifyDataSetChanged();
    }

    public List<Integer> getNotificationTimes() {
        return new ArrayList<>(notifications);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMinutesBefore;
        ImageButton buttonDelete;

        NotificationViewHolder(View itemView) {
            super(itemView);
            textViewMinutesBefore = itemView.findViewById(R.id.textViewMinutesBefore);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
} 