package com.example.zimzik.sunrisesunset.data.network.apis;

import com.example.zimzik.sunrisesunset.data.network.models.SunriseSunset;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SunriseSunsetApi {
    @GET("json")
    Single<SunriseSunset> getSunriseSunset(@Query("lat") String lat, @Query("lng") String lon);
}
