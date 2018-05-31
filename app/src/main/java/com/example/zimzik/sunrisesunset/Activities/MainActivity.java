package com.example.zimzik.sunrisesunset.Activities;

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
    private Disposable mDisposable;
    private RestRepo mRestRepo;
    private Boolean mEnableLocationUpdates = true;

    //fot test
    private TextView tvLat;
    private TextView tvLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvSunriseTime = findViewById(R.id.et_sunrise_time);
        mTvSunsetTime = findViewById(R.id.et_sunset_time);
        mTvConnectionError = findViewById(R.id.et_connection_error);
        mPlaceAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        mPlaceAutocompleteFragment.getView().setVisibility(View.INVISIBLE);

        mPlaceAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place coordinates: lat " + place.getLatLng().latitude + ", lon " + place.getLatLng().longitude);
                setTime(formatCoordinate(place.getLatLng().latitude), formatCoordinate(place.getLatLng().longitude));
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        // for test

        tvLat = findViewById(R.id.test_tv_lat);
        tvLon = findViewById(R.id.test_tv_lon);

        mRestRepo = new RestRepo();

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

       // mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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

    private void enableRequestLocationUpdate() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);
    }

    private void disableRequestLocationUpdate() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(locationListener);
            mLocationManager = null;
        }
    }

    private LocationListener locationListener = new LocationListener() {

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

    private void refreshTime(@Nullable Location location) {
        String provider = location == null ? "" : location.getProvider();

        Log.i(TAG, provider + " " + formatLatitude(location) + " " + formatLongitude(location));

        String lat = formatLatitude(location);
        String lon = formatLongitude(location);
        tvLat.setText("lat: " + lat);
        tvLon.setText("lon: " + lon);
        setTime(lat, lon);
    }

    private String formatLatitude(Location location) {
        if (location == null) {
            return "";
        } else {
            return String.format(Locale.getDefault(), "%1$.6f", location.getLatitude());
        }
    }

    private String formatLongitude(Location location) {
        if (location == null) {
            return "";
        } else {
            return String.format(Locale.getDefault(), "%1$.6f", location.getLongitude() * -1);
        }
    }

    private String formatCoordinate(Double d) {
        return String.format(Locale.getDefault(), "%1$.6f", d);
    }

    private void setTime(String lat, String lon) {
        mDisposable = mRestRepo.getSunriseSunsetApi()
                .getSunriseSunset(lat, lon)
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
}