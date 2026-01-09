package edu.urv.mobileembeded.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CurrentWeatherResponse {

    @SerializedName("main")
    private Main main;

    @SerializedName("weather")
    private List<Weather> weather;

    public Main getMain() {
        return main;
    }

    public List<Weather> getWeather() {
        return weather;
    }
}
