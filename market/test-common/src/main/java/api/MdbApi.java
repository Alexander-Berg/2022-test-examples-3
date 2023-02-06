package api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MdbApi {

    @GET("/poller/intake/run")
    Call<ResponseBody> processIntakeShipments();

    @GET("/shipmentOrder/search")
    Call<ResponseBody> shipmentOrder(
        @Query("marketId") Long orderId
    );

    @GET("/register/search")
    Call<ResponseBody> registerSearch(
        @Query("id") Long registerId
    );

    @POST("/support/poller/registerFromSC")
    Call<ResponseBody> registerFromSC(
        @Query("from") Long from,
        @Query("to") Long to,
        @Query("hour") Integer hour
    );
}
