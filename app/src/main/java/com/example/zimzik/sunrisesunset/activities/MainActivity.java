package com.example.zimzik.sunrisesunset.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zimzik.sunrisesunset.R;
import com.example.zimzik.sunrisesunset.data.network.RestRepo;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

import java.util.Locale;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextView mTvSunriseTime;
    private TextView mTvSunsetTime;
    private TextView mTvConnectionError;
    private PlaceAutocompleteFragment mPlaceAutocompleteFragment;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Disposable mDisposable;
    private RestRepo mRestRepo;
    private Boolean mEnableLocationUpdates = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvSunriseTime = findViewById(R.id.et_sunrise_time);
        mTvSunsetTime = findViewById(R.id.et_sunset_time);
        mTvConnectionError = findViewById(R.id.et_connection_error);

        // initialization location listener
        mLocationListener = initLocationListener();

        mPlaceAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        Objects.requireNonNull(mPlaceAutocompleteFragment.getView()).setVisibility(View.INVISIBLE);

        //  Handle when user choose in widget another place from search list
        mPlaceAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place coordinates: lat " + place.getLatLng().latitude + ", lon " + place.getLatLng().longitude);
                setTime(formatCoordinate(place.getLatLng().latitude), formatCoordinate(place.getLatLng().longitude * -1));
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        mRestRepo = new RestRepo();

        // change action when user change radiobutton
        RadioGroup rgChooseLocation = findViewById(R.id.rg_choose_location);
        rgChooseLocation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_other_location) {
                mPlaceAutocompleteFragment.getView().setVisibility(View.VISIBLE);
                disableRequestLocationUpdate();
                mEnableLocationUpdates = false;
            } else {
                mPlaceAutocompleteFragment.getView().setVisibility(View.INVISIBLE);
                enableRequestLocationUpdate();
                mEnableLocationUpdates = true;
            }
        });

        if (!permissionsGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mEnableLocationUpdates) {
            enableRequestLocationUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableRequestLocationUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do smth
            } else {
                Toast.makeText(this, "You didn't give permission to access device location", Toast.LENGTH_LONG).show();
            }
        }
    }

    private LocationListener initLocationListener() {
        return new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                refreshTime(location);
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                refreshTime(mLocationManager.getLastKnownLocation(provider));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };
    }

    // Enable request location update. Coordinates of current place refresh every 10 seconds
    private void enableRequestLocationUpdate() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, mLocationListener);
        mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                mLocationListener);
    }

    // Disable request location update
    private void disableRequestLocationUpdate() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager = null;
        }
    }
    // Formatting coordinate string to suitable format for transfer to URL
    private void refreshTime(@Nullable Location location) {
        String provider = location == null ? "" : location.getProvider();
        String lat = location == null ? "" : formatCoordinate(location.getLatitude());
        String lon = location == null ? "" : formatCoordinate(location.getLongitude() * -1);

        Log.i(TAG, provider + " " + lat + " " + lon);
        setTime(lat, lon);
    }


    private String formatCoordinate(Double d) {
        return String.format(Locale.getDefault(), "%1$.6f", d);
    }

    // Request time data from server and set response to appropriate TextViews
    private void setTime(String lat, String lon) {
        mDisposable = mRestRepo
                .getSunriseSunsetApi(lat, lon)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(place -> {
                    mTvConnectionError.setText("");
                    mTvSunriseTime.setText(place.getSunrise());
                    mTvSunsetTime.setText(place.getSunset());
                    Log.i(TAG, place.getSunrise() + " " + place.getSunset());
                }, throwable -> mTvConnectionError.setText(R.string.error_internet_connection));
    }

    private Boolean permissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}