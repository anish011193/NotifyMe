package com.example.notifyme.fragments;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.example.notifyme.utils.Constants.AUTOCOMPLETE_REQUEST_CODE;
import static com.example.notifyme.utils.Constants.MAPVIEW_BUNDLE_KEY;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notifyme.R;
import com.example.notifyme.utils.GoogleDirection;
import com.example.notifyme.utils.ViewWeightAnimationWrapper;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.maps.android.SphericalUtil;

import org.w3c.dom.Document;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    //Constant
    private static final String TAG = "MapFragment";
    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0;
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1;
    private int mMapLayoutState = 0;
    //ui
    private MapView mMapView;
    private RelativeLayout mMapContainer;
    private LinearLayout topContainer;
    private TextView tvTimer, tvDurationDistance;
    private Button timerButton;
    private ImageButton ivSearch;
    //var
    private GoogleMap mGoogleMap;
    private PlacesClient placesClient;
    private GoogleDirection googleDirection;
    private CountDownTimer countDownTimer;
    private long timeLeftInMilliSeconds = 60000;
    private boolean isTimerRunning;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.mapView);
        mMapContainer = view.findViewById(R.id.map_area);
        topContainer = view.findViewById(R.id.topContainer);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvTimer.setVisibility(View.GONE);
        tvDurationDistance = view.findViewById(R.id.txtDurationDistance);
        tvDurationDistance.setVisibility(View.GONE);
        timerButton = view.findViewById(R.id.btTimer);
        timerButton.setVisibility(View.GONE);
        ivSearch = (ImageButton) view.findViewById(R.id.btn_search);
        ivSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchCalled();
            }
        });

        timerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvTimer.setVisibility(View.VISIBLE);
                startStopTimer();
            }
        });
        updateTimeText();
        ImageButton ivExpand = (ImageButton) view.findViewById(R.id.btn_full_screen_map);
        ivExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mMapLayoutState == MAP_LAYOUT_STATE_CONTRACTED) {
                    mMapLayoutState = MAP_LAYOUT_STATE_EXPANDED;
                    expandMapAnimation();
                } else if (mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED) {
                    mMapLayoutState = MAP_LAYOUT_STATE_CONTRACTED;
                    contractMapAnimation();
                }
            }
        });

        initGoogleMaps(savedInstanceState);
        initGooglePlaces();
        googleDirection = new GoogleDirection(getActivity());
        return view;
    }

    private void startStopTimer() {
        if (isTimerRunning) {
            stopTimer();
        } else
            startTimer();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMilliSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMilliSeconds = millisUntilFinished;
                updateTimeText();
            }

            @Override
            public void onFinish() {

            }
        }.start();
        timerButton.setText("Pause");
        isTimerRunning = true;
    }

    private void stopTimer() {
        countDownTimer.cancel();
        timerButton.setText("Start");
        isTimerRunning = false;
    }

    private void updateTimeText() {
        int min = (int) timeLeftInMilliSeconds / 60000;
        int seconds = (int) timeLeftInMilliSeconds % 60000 / 1000;

        String timeLeft = " " + min;
        timeLeft += ":";
        if (seconds < 10) {
            timeLeft += "0";
        }
        timeLeft += seconds;
        tvTimer.setText(timeLeft);
    }

    private void initGoogleMaps(Bundle savedInstanceState) {
        Bundle mapViewBuddle = null;
        if (savedInstanceState != null) {
            mapViewBuddle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBuddle);
        mMapView.getMapAsync(this);
    }

    private void initGooglePlaces() {
        String apiKey = getString(R.string.google_map_key);
        if (!Places.isInitialized()) {
            Places.initialize(getActivity().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(getActivity());
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        //autocompleteFragment.setTypeFilter(TypeFilter.CITIES);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId() + ", " + place.getAddress());
                Toast.makeText(getActivity(), "ID: " + place.getId() + "address:" + place.getAddress() + "Name:" + place.getName() + " latlong: " + place.getLatLng(), Toast.LENGTH_LONG).show();
                animateCamera(place.getLatLng(), place.getName());
                Location loc = getLastKnownLocation();
                LatLng userLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                getDistanceBtwTwoPoints(userLocation, place.getLatLng());
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e(TAG, status.toString());
                Toast.makeText(getActivity(), status.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onSearchCalled() {
        // Set the fields to specify which types of place data to return.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields).setCountry("IND") //NIGERIA
                .build(getActivity());
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId() + ", " + place.getAddress());
                Toast.makeText(getActivity(), "ID: " + place.getId() + "address:" + place.getAddress() + "Name:" + place.getName() + " latlong: " + place.getLatLng(), Toast.LENGTH_LONG).show();
                String address = place.getAddress();
                // do query with address

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Toast.makeText(getActivity(), "Error: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(false);
        mGoogleMap = map;
//        getData(mGoogleMap);
//        animateCamera();
    }


//    private void getData(GoogleMap mMap) {
//
//        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
//            @Override
//            public void onCameraIdle() {
//
//                LatLng center = mMap.getCameraPosition().target;
//                String sLatitude = String.format("%.6f", center.latitude);
//                String sLongitude = String.format("%.6f", center.longitude);
//                StringBuilder mLatLng = new StringBuilder();
//                mLatLng.append(sLatitude);
//                mLatLng.append("°");
//                mLatLng.append(" ");
//                mLatLng.append(sLongitude);
//                mLatLng.append("°");
//
//            }
//        });
//    }
//
//    @SuppressLint("MissingPermission")
//    private void animateCamera() {
//        Location location = getLastKnownLocation();
//        if (location != null) {
//
//            mGoogleMap.setMyLocationEnabled(false);
//            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
//            mGoogleMap.getUiSettings().setAllGesturesEnabled(true);
//            //delay is for after map loaded animation starts
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
//
//                }
//            }, 2000);
//        }
//    }

    @SuppressLint("MissingPermission")
    private void animateCamera(LatLng latLng, String placeName) {

        mGoogleMap.setMyLocationEnabled(false);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.getUiSettings().setAllGesturesEnabled(true);
        //delay is for after map loaded animation starts
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(placeName));
            }
        }, 2000);
    }

    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void getDistanceBtwTwoPoints(LatLng originLatLng, LatLng destinationLatLng) {

        String totalDistance = String.valueOf(SphericalUtil.computeDistanceBetween(originLatLng,destinationLatLng));
       // String totalDistance = String.valueOf(calculationByDistance(originLatLng,destinationLatLng));

        tvDurationDistance.setVisibility(View.VISIBLE);
        timerButton.setVisibility(View.VISIBLE);
        tvDurationDistance.setText("Distance: " + totalDistance + " " + "Time: " + totalDistance);

        googleDirection.setOnDirectionResponseListener(new GoogleDirection.OnDirectionResponseListener() {
            @Override
            public void onResponse(String status, Document doc, GoogleDirection gd) {
                Toast.makeText(getActivity().getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                if(!status.equalsIgnoreCase("REQUEST_DENIED")) {
                    googleDirection.animateDirection(mGoogleMap, googleDirection.getDirection(doc), GoogleDirection.SPEED_FAST,
                            true, true, true, false, null, false, true, new PolylineOptions().width(8).color(Color.RED));
                    mGoogleMap.addMarker(new MarkerOptions().position(originLatLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.cartman_cop)));

                    mGoogleMap.addMarker(new MarkerOptions().position(destinationLatLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.chef)));

                    String totalDistance = gd.getTotalDistanceText(doc);
                    String totalDuration = gd.getTotalDurationText(doc);
                    tvDurationDistance.setVisibility(View.VISIBLE);
                    timerButton.setVisibility(View.VISIBLE);
                    tvDurationDistance.setText("Distance: " + totalDuration + " " + "Time: " + totalDistance);
                }

            }
        });
        googleDirection.request(originLatLng, destinationLatLng, GoogleDirection.MODE_DRIVING);
    }

    private String calculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.d(TAG, "Radius Value " + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return kmInDec + "." + meterInDec;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private void expandMapAnimation() {
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                50,
                100);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(topContainer);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                50,
                0);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

    private void contractMapAnimation() {
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                100,
                50);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(topContainer);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                0,
                50);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

}