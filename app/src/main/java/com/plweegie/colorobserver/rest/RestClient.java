package com.plweegie.colorobserver.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RestClient {

    private static final String BASE_URL = "http://192.168.0.6:8000/";
    private DjangoColorsAPI mService;

    public RestClient() {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        mService = retrofit.create(DjangoColorsAPI.class);
    }

    public DjangoColorsAPI getApiService() {
        return mService;
    }
}
