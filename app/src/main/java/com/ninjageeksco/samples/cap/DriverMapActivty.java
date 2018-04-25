package com.ninjageeksco.samples.cap;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriverMapActivty extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {


    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
     Location lasLocation;
    LocationRequest locationRequest;
    private Button driverLogout,driverSettings;
    private FirebaseAuth mAuth;
    private FirebaseUser cUser;
    private DatabaseReference driversAvailable,driversWorking,assignedCustomer,assignedCustomerPickUpRef;
    private GeoFire geoFiredriversAvailable,geoFiredriversWorking;
    private Boolean driverLogoutStatus = false;
    private String userID,customerID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map_activty);

         mAuth = FirebaseAuth.getInstance();
         cUser = mAuth.getCurrentUser();

         userID = mAuth.getCurrentUser().getUid();


         driverLogout = findViewById(R.id.driver_logout_btn);
         driverSettings = findViewById(R.id.driver_settings_btn);

         driverLogout.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 driverLogoutStatus = true;
                 DissconnectTheDriver();
                mAuth.signOut();
                SendUserToWelcomeActivity();
             }
         });

         driverSettings.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

             }
         });


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GetAssignedCustomerRequest();
    }

    private void GetAssignedCustomerRequest() {
        assignedCustomer = FirebaseDatabase.getInstance().getReference().child("Users")
        .child("Drivers").child(userID).child("CustomerRideID");
        assignedCustomer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    customerID = dataSnapshot.getValue().toString();
                    GetAssignedCustomerPickUpLocation();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void GetAssignedCustomerPickUpLocation() {
        assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Customers")
                .child(customerID).child("l");
        assignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0 ,locationLang = 0;
                    if (customerLocationMap.get(0) != null){
                        locationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }if (customerLocationMap.get(1) != null){
                        locationLang = Double.parseDouble(customerLocationMap.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLang);
                    mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Pick Up Location"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {
            lasLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            driversAvailable = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
            driversWorking = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            geoFiredriversAvailable = new GeoFire(driversAvailable);
            geoFiredriversWorking = new GeoFire(driversAvailable);

            switch (customerID) {
                case "" :
                    geoFiredriversAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new
                        GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                            }
                        }) ;
                    geoFiredriversWorking.removeLocation(userID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });
                break;
                default :
                    geoFiredriversWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new
                            GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                    geoFiredriversAvailable.removeLocation(userID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });
                    break;
            }
            }
    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DissconnectTheDriver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DissconnectTheDriver();
    }
    private void SendUserToWelcomeActivity() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void DissconnectTheDriver() {
        if (!driverLogoutStatus){
            geoFiredriversAvailable.removeLocation(userID, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                }
            });
        }
    }
}
