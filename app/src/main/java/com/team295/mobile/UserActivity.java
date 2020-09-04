package com.team295.mobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class UserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastKnownLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int LOC_PERMISSION_REQ_CODE = 23;
    private static final int LOC_ENABLED_CHECK_CODE = 24;
    private Geocoder geocoder;
    private TextView locationTv;
    private MaterialSearchBar searchBar;
    private ActionBarDrawerToggle actionBarDrawerToggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,toolbar,
                R.string.open_nav_drawer, R.string.close_nav_drawer);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();


        locationTv = findViewById(R.id.location_tv);
        searchBar = findViewById(R.id.search_bar);
        searchBar.setHint("Search Hospital Here");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(UserActivity.this);
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        checkLocationPermissions();
        setSearchBarListener();
    }

    private void setSearchBarListener() {
        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                Log.d("TEXTCHANGED", "HERE");

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text.toString(), true, null, true);

            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    searchBar.disableSearch();
                }
            }
        });

        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("TEXTCHANGED", s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("TEXTCHANGED", s.toString());

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("TEXTCHANGED", s.toString());

            }
        });

        searchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                Log.d("TEXTCHANGED", "");

            }

            @Override
            public void OnItemDeleteListener(int position, View v) {
                Log.d("TEXTCHANGED", "ITEM DELETED");

            }
        });
    }


    private void checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_PERMISSION_REQ_CODE);

            }
            checkLocationSettings();
        }
        checkLocationSettings();
    }


    private void checkLocationSettings() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder locationSettingsBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(UserActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(locationSettingsBuilder.build());
        task.addOnSuccessListener(UserActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });

        task.addOnFailureListener(UserActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    try {
                        resolvableApiException.startResolutionForResult(UserActivity.this, LOC_ENABLED_CHECK_CODE);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }


    private void getDeviceLocation() {
        fusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                convertToAddress(lastKnownLocation);
                            } else {
                                locationRequest = LocationRequest.create();
                                locationRequest.setInterval(1000);
                                locationRequest.setFastestInterval(500);
                                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                locationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        if (locationResult != null) {
                                            lastKnownLocation = locationResult.getLastLocation();
                                            convertToAddress(lastKnownLocation);
                                            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                                        }
                                    }
                                };
                                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        }
                    }
                });
    }


    private void convertToAddress(Location myLoc) {
        try {
            List<Address> addressList = geocoder.getFromLocation(myLoc.getLatitude(), myLoc.getLongitude(), 1);
            if (!addressList.isEmpty()) {
                String myAddress = addressList.get(0).getAddressLine(0);
                Log.d("MYADDRESS", myAddress);
                locationTv.setText(String.valueOf(myAddress));

            } else {
                Log.d("EMPTY", "LIST");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOC_PERMISSION_REQ_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationSettings();
                }
                break;
            default:
                Log.d("TEXTCHANGED", "");
                break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOC_ENABLED_CHECK_CODE && resultCode == RESULT_OK) {
            getDeviceLocation();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.login_super:
                Log.d("YOUSELCTED", "SUPER");
                break;
            case R.id.register_super:
               startActivity(new Intent(UserActivity.this, RegisterActivity.class));
                break;
                default:
                    break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }
}


