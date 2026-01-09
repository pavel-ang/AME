package edu.urv.mobileembeded.model;

import com.google.gson.annotations.SerializedName;

public class Weather {

    @SerializedName("description")
    private String description;

    @SerializedName("icon")
    private String icon;

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
