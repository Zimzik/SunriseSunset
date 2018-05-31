package com.example.zimzik.sunrisesunset.data.network;

import com.example.zimzik.sunrisesunset.data.network.apis.SunriseSunsetApi;
import com.example.zimzik.sunrisesunset.data.network.models.SunriseSunset;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestRepo {
    private static final String TAG = RestRepo.class.getSimpleName();
    private Retrofit mRetrofit;



    public RestRepo() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor())
                .build();

        mRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.sunrise-sunset.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
    }

    public Single<SunriseSunset> getSunriseSunsetApi(String lat, String lon) {
        return mRetrofit.create(SunriseSunsetApi.class).getSunriseSunset(lat, lon);
    }
}
