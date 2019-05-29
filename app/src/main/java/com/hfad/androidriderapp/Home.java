package com.hfad.androidriderapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.hfad.androidriderapp.Common.Common;
import com.hfad.androidriderapp.Helper.CustomInfoWindow;
import com.hfad.androidriderapp.Model.FCMResponse;
import com.hfad.androidriderapp.Model.Notification;
import com.hfad.androidriderapp.Model.Rider;
import com.hfad.androidriderapp.Model.Sender;
import com.hfad.androidriderapp.Model.Token;
import com.hfad.androidriderapp.Remote.IFCMService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    SupportMapFragment mapFragment;


    //Location
    private GoogleMap mMap;

    //Play Services
    private static final int MY_PERMISSION_REQUEST_CODE = 7192; //EDMT DEV Birthday
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193; //Madam

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL = 5000; //5secs
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference ref;
    GeoFire geoFire;

    Marker mUserMarker,markerDestination;

    //Bottomsheet
    ImageView imgExpandable;
    BottomSheetRiderFragment mBottomSheet;
    Button btnPickupRequest;

    boolean isDriverFound=false;
    String driverId="";
    int radius = 1; //1km
    int distance = 1; //3km
    private static final int LIMIT = 3;

    //Send Alert
    IFCMService mService;

    //Presence system
    DatabaseReference driverAvailable;

    PlaceAutocompleteFragment place_location,place_destination;

    String mPlaceLocation,mPlaceDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        mService = Common.getFCMService();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //maps
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

       /* //Geo Fire
        ref = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        geoFire = new GeoFire(ref);
        */


        //Initialise view
        imgExpandable = (ImageView)findViewById(R.id.imgExpandable);


        btnPickupRequest = (Button)findViewById(R.id.btnPickupRequest);
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isDriverFound)
                    requestPickupHere(FirebaseAuth.getInstance().getCurrentUser().getUid());
                else
                    sendRequestToDriver(driverId);
            }
        });

        place_destination = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_destination);
        place_location = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_location);
        /*typeFilter = new AutocompleteFilter().Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                    .setTypeFilter(3)
                    .build();*/

        //Event
        place_location.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceLocation = place.getAddress().toString();
                //Remove old Marker
                mMap.clear();

                //Add Marker at new location
                mUserMarker = mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                                .title("Pickup Here"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),150.f));
            }

            @Override
            public void onError(Status status) {

            }
        });
        place_destination.setOnPlaceSelectedListener(new PlaceSelectionListener() {


            @Override
            public void onPlaceSelected(Place place) {
                mPlaceDestination = place.getAddress().toString();

                //Add new destination Marker
                mMap.addMarker(new MarkerOptions()
                                .position(place.getLatLng())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                                .title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15.0f));

                //Show information at the bottom
                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstance(mPlaceLocation,mPlaceDestination,false);
                mBottomSheet.show(getSupportFragmentManager(),mBottomSheet.getTag());
            }

            @Override
            public void onError(Status status) {

            }
        });


        setUpLocation();

        updateFirebaseToken();
    }

    private void updateFirebaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);

    }

    private void sendRequestToDriver(String driverId) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.token_tbl);

        tokens.orderByKey().equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapShot:dataSnapshot.getChildren())
                        {
                            Token token = postSnapShot.getValue(Token.class); //Get Token object from database with key

                            //Make raw Payload = convert LatLng to json
                            String json_lat_lng = new Gson().toJson(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                            String riderToken = FirebaseInstanceId.getInstance().getToken();
                            Notification data = new Notification(riderToken,json_lat_lng); //Send to driver to deserialize again
                            Sender content = new Sender(token.getToken(),data); //Send received data to Token

                            mService.sendMessage(content)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body().success == 1)
                                                Toast.makeText(Home.this, "Request sent!", Toast.LENGTH_SHORT).show();
                                            else
                                                Toast.makeText(Home.this, "Failed !", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable throwable) {
                                            Log.e("ERROR", throwable.getMessage());

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void requestPickupHere(String uid) {
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

        if (mUserMarker.isVisible())
            mUserMarker.remove();
        //Add new Marker
        mUserMarker = mMap.addMarker(new MarkerOptions()
                .title("Pickup Here")
                .snippet("")
                .position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mUserMarker.showInfoWindow();

        btnPickupRequest.setText("Getting your Driver...");

        findDriver();
    }

    private void findDriver() {
        final DatabaseReference drivers= FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers = new GeoFire(drivers);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),
                radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                //If Found(Driver)
                if (!isDriverFound)
                {
                    isDriverFound = true;driverId = key;
                    btnPickupRequest.setText("CALL DRIVER");
                    //Toast.makeText(Home.this, ""+key, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //if still no drivers found, increase distance
                if (!isDriverFound && radius < LIMIT)
                {
                    radius++;findDriver();
                }
                else
                {
                    Toast.makeText(Home.this,"No available driver near you", Toast.LENGTH_SHORT).show();
                    btnPickupRequest.setText("REQUEST PICKUP");
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    //Press Ctrl+O


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (checkPlayServices()){
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void setUpLocation() {

        //Copy code from Driver App
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //Request runtime permission
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            if (checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null){


            //Presence system
            driverAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
            driverAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //Incase of any change from driver table, reload all drivers available
                    loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


            final double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();



                        loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                Log.d("OBI.M", String.format("Your location was changed : %f / %f", latitude, longitude));
        }
        else{
            Log.d("OBI.M","Cannot get your location");
        }
    }

    private void loadAllAvailableDriver(final LatLng location) {

        //Add Marker
       //Clear all map to delete old position of divers
        mMap.clear();
        mUserMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .position(location)
                .title(String.format("You")));

        //Move Camera to This Position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f));

        //Load all available drivers in 3km distance
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.latitude,location.longitude),distance);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                //Use key to get email from table Users
                //Table Users: table where driver registers account and update information
                //Open Driver app to confirm table name
                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //Since Rider and User model is same properties
                                //We can use Rider model to get User here
                                Rider rider = dataSnapshot.getValue(Rider.class);

                                //Add driver to map
                                mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude,location.longitude))
                                                .flat(true)
                                                .title(rider.getName())
                                                .snippet("Phone : "+rider.getPhone())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT) //Distance found within 3km
                {
                    distance++;
                    loadAllAvailableDriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (resultCode != ConnectionResult.SUCCESS){
                if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                    GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
                else {
                    Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return false;
            }
            return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {


        try {
            boolean isSuccess = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this,R.raw.my_map_style)
            );

            if (!isSuccess)
                Log.e("ERROR","Map style load failed !!");
        }
        catch (Resources.NotFoundException ex)
        {
            ex.printStackTrace();
        }

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //First, check marker Destination
                //If not null, remove available marker
                if (markerDestination != null)
                    markerDestination.remove();
                markerDestination = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                .position(latLng)
                .title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15.0f));

                //Show Bottom Sheet
                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstance(String.format("%f,%f",mLastLocation.getLatitude(),mLastLocation.getLongitude()),
                        String.format("%f,%f",latLng.latitude,latLng.longitude),
                        true);
                mBottomSheet.show(getSupportFragmentManager(),mBottomSheet.getTag());
            }
        });

        /*//Add sample Marker
        googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(37.7750, -122.4183))
                    .title("San Francisco"));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7750, -122.4183),12));
*/    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }
}
