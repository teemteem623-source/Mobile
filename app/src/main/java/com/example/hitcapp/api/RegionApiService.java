package com.example.hitcapp.api;

import com.example.hitcapp.models.Region;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RegionApiService {
    @GET("p/")
    Call<List<Region.Province>> getProvinces();

    @GET("p/{code}")
    Call<Region.Province> getProvinceDetail(@Path("code") int code, @Query("depth") int depth);
}
