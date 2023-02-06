package api;

import java.util.List;

import dto.requests.tpl.pvz.OrderStatusUpdateDto;
import dto.requests.tpl.pvz.ReceiveCreateDto;
import dto.requests.tpl.pvz.VerifyCodeDto;
import dto.responses.tpl.pvz.OrderPageDto;
import dto.responses.tpl.pvz.PickupPointRequestData;
import dto.responses.tpl.pvz.PvzOrderDto;
import dto.responses.tpl.pvz.PvzReturnDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TplPvzApi {

    @PATCH("/manual/orders/updateStatus")
    Call<List<PvzOrderDto>> updatePvzOrderStatus(@Body List<OrderStatusUpdateDto> statusUpdateDtos);

    @PATCH("v1/pi/pickup-points/{pvzId}/orders/{id}/verify-code")
    Call<PvzOrderDto> verifyCodeForPvzOrder(
        @Path("pvzId") String pvzId,
        @Path("id") String id,
        @Body VerifyCodeDto verifyCodeDto
    );

    @PATCH("v1/pi/pickup-points/{pvzId}/return-requests/{id}/receive")
    Call<PvzReturnDto> receiveReturn(
        @Path("pvzId") Long pvzId,
        @Path("id") Long returnId
    );

    @POST("/v1/pi/pickup-points/{pvzId}/shipments-receive")
    Call<PickupPointRequestData> receiveOrder(
        @Path("pvzId") Long pvzId,
        @Header("x-user-uid") String user,
        @Body ReceiveCreateDto receiveCreateDto
    );

    @GET("v1/pi/pickup-points/{pvzId}/orders/page")
    Call<OrderPageDto> getOrder(
        @Path("pvzId") Long pvzId,
        @Query("externalId") String externalId
    );
}
