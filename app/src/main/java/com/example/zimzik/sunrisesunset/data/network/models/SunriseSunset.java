package com.example.zimzik.sunrisesunset.data.network.models;

import com.google.gson.annotations.SerializedName;

public class SunriseSunset {

    @SerializedName("results")
    private Results mResults;

    @SerializedName("status")
    private String mStatus;

    public String getStatus() {
        return mStatus;
    }

    public String getSunrise() {
        return mResults.mSunrise;
    }

    public String getSunset() {
        return mResults.mSunset;
    }

    private class Results {
        @SerializedName("sunrise")
        private String mSunrise;

        @SerializedName("sunset")
        private String mSunset;
    }
}
