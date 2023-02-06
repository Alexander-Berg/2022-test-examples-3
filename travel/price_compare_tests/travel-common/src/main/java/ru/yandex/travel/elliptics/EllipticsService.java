package ru.yandex.travel.elliptics;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface EllipticsService {

    @POST("upload/{fileName}")
    Call<ResponseBody> upload(@Path("fileName") String fileName, @Body RequestBody file);

    @GET("travel-indexer/{fileName}")
    Call<ResponseBody> getFromS3(@Path("fileName") String fileName);

}
