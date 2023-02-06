package api;


import java.util.List;

import javax.annotation.Nullable;

import dto.requests.nesu.FilterDORequest;
import dto.responses.nesu.DeliveryOptionsItem;
import dto.responses.nesu.ShipmentLogisticPoint;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NesuBackofficeApi {

    @PUT("back-office/delivery-options")
    Call<List<DeliveryOptionsItem>> getOptions(
            @Header("X-Ya-Service-Ticket") String tvmTicket,
            @Query("senderId") Long senderId,
            @Query("shopId") Long shopId,
            @Query("userId") Long userId,
            @Body FilterDORequest.FilterDORequestBuilder filterDORequest
    );

    @GET("/back-office/business/warehouses/{id}/available-shipment-options")
    Call<List<ShipmentLogisticPoint>> getAvailableShipmentOptions(
            @Header("X-Ya-Service-Ticket") String tvmTicket,
            @Path("id") Long partnerId,
            @Query("shopId") Long shopId,
            @Query("userId") Long userId,
            @Query("showDisabled") Boolean showDisabled,
            @Query("showOnlyReturnEnabled") @Nullable Boolean showOnlyReturnEnabled
    );
}
