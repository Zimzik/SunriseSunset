package com.example.zimzik.sunrisesunset.data.network;

import android.util.Log;

import com.example.zimzik.sunrisesunset.data.network.apis.SunriseSunsetApi;
import com.example.zimzik.sunrisesunset.data.network.models.SunriseSunset;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestRepo {
    private static final String TAG = RestRepo.class.getSimpleName();
    private Retrofit sRetrofit;



    public RestRepo() {
        /*HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log -> Log.d(TAG, log));
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);*/

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor())
                .build();

        sRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.sunrise-sunset.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
    }

    public SunriseSunsetApi getSunriseSunsetApi() {
        return sRetrofit.create(SunriseSunsetApi.class);
    }
}
