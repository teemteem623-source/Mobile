package com.example.hitcapp.api;

import com.example.hitcapp.models.Region;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RegionApiService {

    @GET("api/v2/p/")
    Call<List<Region.Province>> getProvinces();

    @GET("api/v2/p/{code}?depth=3")
    Call<Region.Province> getProvinceDetail(@Path("code") int code);
}
