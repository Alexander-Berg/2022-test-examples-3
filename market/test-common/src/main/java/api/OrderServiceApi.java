package api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.order_service.client.model.ActorType;
import ru.yandex.market.order_service.client.model.ChangeOrderStatus;
import ru.yandex.market.order_service.client.model.ChangeOrderStatusResponse;
import ru.yandex.market.order_service.client.model.CreateExternalOrderRequest;
import ru.yandex.market.order_service.client.model.CreateExternalOrderResponse;
import ru.yandex.market.order_service.client.model.GetDeliveryOptionsRequest;
import ru.yandex.market.order_service.client.model.GetDeliveryOptionsResponse;
import ru.yandex.market.order_service.client.model.GetOrderLogisticsResponse;
import ru.yandex.market.order_service.client.model.OrderSubStatus2;

public interface OrderServiceApi {
    @POST("/partners/{partnerId}/common/logistics/delivery/get-delivery-options")
    Call<GetDeliveryOptionsResponse> getDeliveryOptions(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("partnerId") Long partnerId,
        @Body GetDeliveryOptionsRequest getDeliveryOptionsRequest
    );

    @POST("/partners/{partnerId}/common/orders/external")
    Call<CreateExternalOrderResponse> createOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("partnerId") Long partnerId,
        @Body CreateExternalOrderRequest createExternalOrderRequest
    );

    @GET("/partners/{partnerId}/logistics/orders/{orderId}")
    Call<GetOrderLogisticsResponse> getOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("partnerId") Long partnerId,
        @Path("orderId") Long orderId
    );

    @POST("/partners/{partnerId}/common/orders/{orderId}/status")
    Call<ChangeOrderStatusResponse> changeOrderStatus(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("partnerId") Long partnerId,
        @Path("orderId") Long orderId,
        @Query("status") ChangeOrderStatus changeOrderStatus,
        @Query("actor") ActorType actor,
        @Query("actorId") Long actorId,
        @Query("substatus") OrderSubStatus2 substatus
    );
}
