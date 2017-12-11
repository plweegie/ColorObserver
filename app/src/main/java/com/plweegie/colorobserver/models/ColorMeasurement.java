package com.plweegie.colorobserver.models;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ColorMeasurement {

    @SerializedName("hue")
    @Expose
    private float hue;
    @SerializedName("saturation")
    @Expose
    private float saturation;
    @SerializedName("intensity")
    @Expose
    private float intensity;
    @SerializedName("bitmap_as_string")
    @Expose
    private String bitmapAsString;

    public ColorMeasurement() {
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public String getBitmapAsString() {
        return bitmapAsString;
    }

    public void setBitmapAsString(String imgAsString) {
        this.bitmapAsString = imgAsString;
    }
}
