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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lasLocation;
    LocationRequest locationRequest;
    Marker driverMarker;
    private Button customerLogout,customerSettings,customerCallACar;
    private FirebaseAuth mAuth;
    private FirebaseUser cUser;
    private String userID,driverID;
    private DatabaseReference customersAvailable,driverLocationReference,driverReference,driverAvilable;
    private GeoFire geoFire;
    private Boolean driverLogoutStatus = false, driverFound = false;
    private LatLng customerPickUpLocation;
    private int radius = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        mAuth = FirebaseAuth.getInstance();
        cUser = mAuth.getCurrentUser();

        userID = mAuth.getCurrentUser().getUid();
        customersAvailable = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        driverLocationReference = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        driverAvilable = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
        geoFire = new GeoFire(customersAvailable);

        customerLogout = findViewById(R.id.customer_logout_btn);
        customerSettings = findViewById(R.id.customer_settings_btn);
        customerCallACar = findViewById(R.id.customer_call_acar_btn);

        customerLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                LogoutCustomer();
                SendUserToWelcomeActivity();
            }
        });
        customerSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

            customerCallACar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                     geoFire.setLocation(userID, new GeoLocation(lasLocation.getLatitude(),lasLocation.getLongitude()),new
                        GeoFire.CompletionListener(){
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                customerPickUpLocation = new LatLng(lasLocation.getLatitude(),lasLocation.getLongitude());
                      mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("Pick Up From Here"));
                            }
                        });
            customerCallACar.setText("Waiting for A Car");
           GetClosestDriver();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void GetClosestDriver() {
        GeoFire geoFiree = new GeoFire(driverLocationReference);
        GeoQuery geoQuery = geoFiree.queryAtLocation(new GeoLocation(lasLocation.getLatitude(),
                lasLocation.getLongitude()),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound){
                    driverFound = true;
                    driverID = key;
                    driverReference = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverID);
                    HashMap hashMap = new HashMap();
                    hashMap.put("CustomerRideID",userID);
                    driverReference.updateChildren(hashMap);
                    GettingDriverLocation();
                }}
            @Override
            public void onKeyExited(String key) {}
            @Override
            public void onKeyMoved(String key, GeoLocation location) {}
            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                radius ++;
                GetClosestDriver();}}
            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });    }

    private void GettingDriverLocation() {
        driverAvilable.child(driverID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            List<Object> driverLocationMap = (List<Object> )dataSnapshot.getValue();
                            double locationLat = 0 ,locationLang = 0;
                            customerCallACar.setText("Driver Found");
                            if (driverLocationMap.get(0) != null){
                                locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }if (driverLocationMap.get(1) != null){
                                locationLang = Double.parseDouble(driverLocationMap.get(1).toString());
                            }
                            LatLng driverLatLng = new LatLng(locationLat,locationLang);
                            if (driverMarker != null){
                                driverMarker.remove();
                            }
                            Location location1 = new Location("");
                            location1.setLongitude(customerPickUpLocation.longitude);
                            location1.setLatitude(customerPickUpLocation.latitude);

                            Location location2 = new Location("");
                            location2.setLongitude(driverLatLng.longitude);
                            location2.setLatitude(driverLatLng.latitude);

                            float distance = location1.distanceTo(location2);
                            customerCallACar.setText("Driver Found : " + String.valueOf(distance));
                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver"));
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

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
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
        lasLocation = location;
        LatLng latLng =  new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));


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
        LogoutCustomer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogoutCustomer();
    }
    private void SendUserToWelcomeActivity() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void LogoutCustomer() {
        if (!driverLogoutStatus){
            geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                }
            });
        }
    }
}
