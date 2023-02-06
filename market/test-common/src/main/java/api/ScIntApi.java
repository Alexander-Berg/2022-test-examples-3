package api;

import java.util.List;

import dto.responses.scapi.orders.ApiOrderDto;
import dto.responses.scint.manualcells.ScRoute;
import dto.responses.scintmanualcells.GetCellsResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ScIntApi {

    @GET("/manual/cells/getCellsForZone")
    Call<GetCellsResponse> getCellsForZone(
        @Query("scId") Long scId,
        @Query("zoneId") Long zoneId,
        @Header("Authorization") String token
    );

    @GET("/manual/routes")
    Call<List<ScRoute>> getRoutes(
        @Query("scId") Long scId,
        @Query("routeType") String routeType,
        @Header("Authorization") String token
    );

    @GET("/manual/routes/{routeId}")
    Call<ScRoute> getRouteInfo(
        @Path("routeId") long routeId,
        @Query("scId") Long scId,
        @Header("Authorization") String token
    );

    @PUT("/manual/cells/clear")
    Call<ResponseBody> clearCell(
        @Query("id") Long cellId,
        @Header("Authorization") String token
    );

    @GET("/manual/orders")
    Call<ApiOrderDto> getOrder(
        @Query("scId") Long scId,
        @Query("externalId") String externalId,
        @Header("Authorization") String token
    );

    @POST("/manual/orders/acceptAndSort")
    Call<ResponseBody> acceptAndSortOrder(
        @Query("externalOrderId") String externalOrderId,
        @Query("externalPlaceId") String externalPlaceId,
        @Query("scId") Long scId,
        @Query("cellId") Long cellId,
        @Header("Authorization") String token
    );

    @POST("/manual/orders/ship")
    Call<ResponseBody> shipOrder(
        @Query("externalOrderId") String externalOrderId,
        @Query("externalPlaceId") String externalPlaceId,
        @Query("scId") Long scId,
        @Header("Authorization") String token
    );

    @POST("/manual/orders/accept")
    Call<ResponseBody> acceptOrder(
        @Query("externalOrderId") String externalOrderId,
        @Query("externalPlaceId") String externalPlaceId,
        @Query("scId") Long scId,
        @Header("Authorization") String token
    );

    @POST("/manual/orders/sort")
    Call<ResponseBody> sortOrder(
        @Query("externalOrderId") String externalOrderId,
        @Query("externalPlaceId") String externalPlaceId,
        @Query("scId") Long scId,
        @Query("cellId") Long cellId,
        @Header("Authorization") String token
    );

    @POST("/manual/job/sendSegmentFfStatusHistoryToSqs")
    Call<ResponseBody> sendSegmentFfStatusHistoryToSqs(
        @Header("Authorization") String token
    );

    @PUT("/internal/partners/{scId}/inbounds/{externalIdOrInfoListCode}/performAction/{action}")
    Call<ResponseBody> performInboundAction(
        @Path("scId") Long scId,
        @Path("externalIdOrInfoListCode") String inboundId,
        @Path("action") String action,
        @Header("Authorization") String token
    );

    @PUT("/internal/partners/{scId}/inbounds/{externalIdOrInfoListCode}/carArrived")
    Call<ResponseBody> carArrived(
        @Path("scId") Long scId,
        @Path("externalIdOrInfoListCode") String inboundId,
        @Body Object carInfo,
        @Header("Authorization") String token
    );

    @PUT("/manual/inbounds/fixInbound")
    Call<ResponseBody> fixInbound(
        @Query("externalId") String inboundExternalId,
        @Header("Authorization") String token
    );
}
