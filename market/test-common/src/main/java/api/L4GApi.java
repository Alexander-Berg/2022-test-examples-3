package api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

import ru.yandex.market.logistics4go.client.model.CancelOrderResponse;
import ru.yandex.market.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics4go.client.model.CreateOrderResponse;
import ru.yandex.market.logistics4go.client.model.GetOrderResponse;

public interface L4GApi {
    @GET("orders/{orderId}")
    Call<GetOrderResponse> getOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId
    );

    @POST("orders")
    Call<CreateOrderResponse> createOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body CreateOrderRequest request
    );

    @DELETE("orders/{orderId}")
    Call<CancelOrderResponse> cancelOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId
    );
}
