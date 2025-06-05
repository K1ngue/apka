package com.example.meetingassistant.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "meetings",
        indices = {
            @Index(value = {"name", "surname", "date", "hour", "minute"}, unique = true)
        })
public class Meeting {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String surname;
    private String street;
    private String houseNumber;
    private String city;
    private long date;
    private int hour;
    private int minute;

    public Meeting(String name, String surname, String street, String houseNumber, String city, long date, int hour, int minute) {
        this.name = name;
        this.surname = surname;
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.date = date;
        this.hour = hour;
        this.minute = minute;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public String getAddress() {
        return street + " " + houseNumber + ", " + city;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meeting meeting = (Meeting) o;
        return date == meeting.date &&
                hour == meeting.hour &&
                minute == meeting.minute &&
                Objects.equals(name, meeting.name) &&
                Objects.equals(surname, meeting.surname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, surname, date, hour, minute);
    }
}
