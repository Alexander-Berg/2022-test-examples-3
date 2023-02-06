package api;

import java.util.Map;

import dto.responses.lavka.TristeroOrderResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface TristeroApi {

    @GET("/admin/parcels/v1/order")
    Call<TristeroOrderResponse> getLavkaOrderInfo(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Query("ref_order") Long orderId
    );

    @PUT("/admin/parcels/v1/parcel/set-state")
    Call<TristeroOrderResponse> setStatus(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Query("id") String parcelId,
        @Body Map<String, String> setStateBody
    );

}
