package com.example.meetingassistant.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeetingNotificationDao {
    @Query("SELECT * FROM meeting_notifications WHERE meetingId = :meetingId")
    List<MeetingNotification> getNotificationsForMeeting(long meetingId);

    @Insert
    void insertNotification(MeetingNotification notification);

    @Delete
    void deleteNotification(MeetingNotification notification);

    @Query("DELETE FROM meeting_notifications WHERE meetingId = :meetingId")
    void deleteAllNotificationsForMeeting(long meetingId);
} 