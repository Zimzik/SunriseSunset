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
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zimzik.sunrisesunset.R;
import com.example.zimzik.sunrisesunset.data.network.RestRepo;
import com.example.zimzik.sunrisesunset.data.network.models.SunriseSunset;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;

import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextView mTvSunriseTime;
    private TextView mTvSunsetTime;
    private LocationManager mLocationManager;
    private AutoCompleteTextView mActvPlaces;
    private Disposable mDisposable;
    private RestRepo mRestRepo;
    //fot test
    private TextView tvLat;
    private TextView tvLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvSunriseTime = findViewById(R.id.et_sunrise_time);
        mTvSunsetTime = findViewById(R.id.et_sunset_time);
        mActvPlaces = findViewById(R.id.actv_places);
        mActvPlaces.setEnabled(false);


        // for test

        tvLat = findViewById(R.id.test_tv_lat);
        tvLon = findViewById(R.id.test_tv_lon);

        mRestRepo = new RestRepo();

        RadioGroup rgChooseLocation = findViewById(R.id.rg_choose_location);
        rgChooseLocation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_other_location) {
                mActvPlaces.setEnabled(true);
            } else {
                mActvPlaces.setEnabled(false);
            }
        });
        
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!permissionsGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);
        checkEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            refreshTime(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            checkEnabled();
        }

        @Override
        public void onProviderEnabled(String provider) {
            checkEnabled();
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

    private void setTime(String lat, String lon) {
        // TODO: 30.05.2018 make Disposable global and dispose in onStop/onDestroy
        mDisposable = mRestRepo.getSunriseSunsetApi()
                .getSunriseSunset(lat, lon)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(place -> {
                    mTvSunriseTime.setText(place.getSunrise());
                    mTvSunsetTime.setText(place.getSunset());
                    Log.i(TAG, place.getSunrise() + " " + place.getSunset());
                });

                /*gpsApi().getSunriseSunset(lat, lon)


                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(place -> {
                    mTvSunriseTime.setText(place.getSunrise());
                    mTvSunsetTime.setText(place.getSunset());
                    Log.i(TAG, place.getSunrise() + " " + place.getSunset());
                });*/
    }

    private void checkEnabled() {
      /*  if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            tvTitleGPS.setTextColor(Color.GREEN);
        } else {
            tvTitleGPS.setTextColor(Color.RED);
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            tvTitleNet.setTextColor(Color.GREEN);
        } else {
            tvTitleNet.setTextColor(Color.RED);
        }*/
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