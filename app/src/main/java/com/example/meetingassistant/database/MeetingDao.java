package com.example.meetingassistant.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface MeetingDao {
    @Query("SELECT COUNT(*) FROM meetings WHERE name = :name AND surname = :surname AND date = :date AND hour = :hour AND minute = :minute")
    int checkDuplicateMeeting(String name, String surname, long date, int hour, int minute);

    @Transaction
    default long insertMeeting(Meeting meeting) {
        // Sprawdź czy istnieje duplikat
        int count = checkDuplicateMeeting(
            meeting.getName(),
            meeting.getSurname(),
            meeting.getDate(),
            meeting.getHour(),
            meeting.getMinute()
        );
        
        if (count > 0) {
            throw new IllegalStateException("DUPLICATE_MEETING");
        }
        
        return insertMeetingInternal(meeting);
    }

    @Insert
    long insertMeetingInternal(Meeting meeting);

    @Update
    void updateMeeting(Meeting meeting);

    @Delete
    void deleteMeeting(Meeting meeting);

    @Query("SELECT * FROM meetings " +
           "ORDER BY " +
           "CASE WHEN (date/86400000 = strftime('%s', 'now')/86400 AND (hour * 3600 + minute * 60) < (strftime('%H', 'now') * 3600 + strftime('%M', 'now') * 60)) " +
           "     OR date/86400000 < strftime('%s', 'now')/86400 THEN 1 ELSE 0 END, " +
           "date ASC, hour ASC, minute ASC")
    List<Meeting> getAllMeetings();

    @Query("SELECT * FROM meetings WHERE id = :id")
    Meeting getMeetingById(long id);

    @Query("DELETE FROM meetings WHERE id = :id")
    void deleteMeetingById(long id);
}

