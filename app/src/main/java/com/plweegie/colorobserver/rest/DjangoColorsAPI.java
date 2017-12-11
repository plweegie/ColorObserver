package com.plweegie.colorobserver.rest;

import com.plweegie.colorobserver.models.ColorMeasurement;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;


public interface DjangoColorsAPI {

    @Headers("Content-Type: application/json")
    @POST("colors/")
    Call<ResponseBody> createColorEntry(@Body ColorMeasurement color);

}
