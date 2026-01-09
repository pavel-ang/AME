package edu.urv.mobileembeded.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ForecastResponse {

    @SerializedName("list")
    private List<CurrentWeatherResponse> list;

    @SerializedName("city")
    private City city;

    public List<CurrentWeatherResponse> getList() {
        return list;
    }

    public City getCity() {
        return city;
    }
}
