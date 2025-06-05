package com.example.meetingassistant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meetingassistant.database.Meeting;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeetingMapAdapter extends RecyclerView.Adapter<MeetingMapAdapter.ViewHolder> {
    private static final String TAG = "MeetingMapAdapter";
    private final List<Meeting> meetings;
    private final Context context;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;
    private final FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private final Handler mainHandler;
    private LocationCallback locationCallback;
    private final Geocoder geocoder;

    public MeetingMapAdapter(Context context, List<Meeting> meetings) {
        this.context = context;
        this.meetings = meetings;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.geocoder = new Geocoder(context, Locale.getDefault());

        setupLocationCallback();
        updateCurrentLocation();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = location;
                    notifyDataSetChanged();
                }
            }
        };
    }

    private void calculateAndShowTravelTime(@NonNull ViewHolder holder, String address, Date meetingDate) {
        if (currentLocation == null) {
            holder.estimatedTime.setText("Oczekiwanie na lokalizację...");
            holder.departureTime.setText("");
            updateCurrentLocation();
            return;
        }

        try {
            // Dodaj kraj do adresu dla lepszego wyszukiwania
            String fullAddress = address + ", Polska";
            List<Address> addresses = geocoder.getFromLocationName(fullAddress, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address destination = addresses.get(0);
                
                // Oblicz odległość między punktami
                float[] results = new float[1];
                Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(),
                    results
                );

                // Konwertuj metry na kilometry
                float distanceInKm = results[0] / 1000;

                // Szacowany czas podróży (zakładamy średnią prędkość 30 km/h w mieście)
                float estimatedTimeHours = distanceInKm / 30.0f;
                int estimatedMinutes = Math.round(estimatedTimeHours * 60);

                // Dodaj 20% czasu na korki i inne opóźnienia
                estimatedMinutes = Math.round(estimatedMinutes * 1.2f);
                
                // Dodaj 5 minut na znalezienie parkingu i dojście
                estimatedMinutes += 5;

                // Formatuj czas podróży
                String timeFormat;
                if (estimatedMinutes >= 60) {
                    int hours = estimatedMinutes / 60;
                    int remainingMinutes = estimatedMinutes % 60;
                    timeFormat = String.format(Locale.getDefault(), "%dh %dmin", hours, remainingMinutes);
                } else {
                    timeFormat = String.format(Locale.getDefault(), "%dmin", estimatedMinutes);
                }

                // Pokaż szacowany czas podróży
                String estimatedTimeText = String.format(Locale.getDefault(),
                    "Szacowany czas podróży: %s (%,.1f km)",
                    timeFormat, distanceInKm);
                holder.estimatedTime.setText(estimatedTimeText);

                // Oblicz i pokaż sugerowany czas wyjazdu
                Calendar meetingTime = Calendar.getInstance();
                meetingTime.setTime(meetingDate);
                meetingTime.add(Calendar.MINUTE, -estimatedMinutes);
                
                String departureText = String.format(Locale.getDefault(),
                    "Sugerowana godzina wyjazdu: %s",
                    this.timeFormat.format(meetingTime.getTime()));
                holder.departureTime.setText(departureText);
            } else {
                holder.estimatedTime.setText("Nie można znaleźć adresu");
                holder.departureTime.setText("");
                Log.e(TAG, "Geocoding failed for address: " + fullAddress);
            }
        } catch (IOException e) {
            holder.estimatedTime.setText("Błąd wyszukiwania adresu");
            holder.departureTime.setText("");
            Log.e(TAG, "Geocoding error: " + e.getMessage());
        }
    }

    private void updateCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    notifyDataSetChanged();
                }
                startLocationUpdates();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting location: " + e.getMessage());
                startLocationUpdates();
            });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopLocationUpdates();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meeting_map, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);
        String fullName = meeting.getName() + " " + meeting.getSurname();
        String address = String.format(Locale.getDefault(), "%s %s, %s",
                meeting.getStreet(),
                meeting.getHouseNumber(),
                meeting.getCity());

        holder.meetingTitle.setText(fullName);
        holder.meetingAddress.setText(address);

        // Formatowanie daty spotkania
        Date meetingDate = new Date(meeting.getDate());
        holder.meetingDateTime.setText(dateFormat.format(meetingDate));

        // Oblicz i pokaż szacowany czas podróży
        calculateAndShowTravelTime(holder, address, meetingDate);

        // Pokaż na mapie
        holder.showOnMapButton.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Toast.makeText(context,
                        "Zainstaluj Google Maps aby zobaczyć lokalizację",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Rozpocznij nawigację
        holder.startNavigationButton.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(address));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Toast.makeText(context,
                        "Zainstaluj Google Maps aby rozpocząć nawigację",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public void updateMeetings(List<Meeting> newMeetings) {
        meetings.clear();
        meetings.addAll(newMeetings);
        updateCurrentLocation();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView meetingTitle;
        final TextView meetingAddress;
        final TextView meetingDateTime;
        final TextView estimatedTime;
        final TextView departureTime;
        final Button showOnMapButton;
        final Button startNavigationButton;

        ViewHolder(View view) {
            super(view);
            meetingTitle = view.findViewById(R.id.meetingTitle);
            meetingAddress = view.findViewById(R.id.meetingAddress);
            meetingDateTime = view.findViewById(R.id.meetingDateTime);
            estimatedTime = view.findViewById(R.id.estimatedTime);
            departureTime = view.findViewById(R.id.departureTime);
            showOnMapButton = view.findViewById(R.id.showOnMapButton);
            startNavigationButton = view.findViewById(R.id.startNavigationButton);
        }
    }
} 