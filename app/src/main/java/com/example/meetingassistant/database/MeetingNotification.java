package com.example.meetingassistant.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "meeting_notifications",
        foreignKeys = @ForeignKey(
                entity = Meeting.class,
                parentColumns = "id",
                childColumns = "meetingId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("meetingId")})
public class MeetingNotification {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long meetingId;
    private int minutesBefore;

    public MeetingNotification(long meetingId, int minutesBefore) {
        this.meetingId = meetingId;
        this.minutesBefore = minutesBefore;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(long meetingId) {
        this.meetingId = meetingId;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }

    public void setMinutesBefore(int minutesBefore) {
        this.minutesBefore = minutesBefore;
    }
} 