package api;

import dto.requests.checkouter.CancellationRequest;
import dto.requests.checkouter.UpdateDeliveryRequest;
import dto.requests.checkouter.cart.CartRequest;
import dto.requests.checkouter.checkout.CheckoutRequest;
import dto.responses.checkouter.ReturnResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.checkout.checkouter.balance.model.BalanceStatus;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.returns.Return;

public interface CheckouterApi {

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Headers({"X-Hit-Rate-Group: UNLIMIT"})
    @POST("/cart")
    Call<ResponseBody> cart(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("X-Market-Rearrfactors") String rearrFactors,
        @Query("showHiddenPaymentOptions") Boolean showHiddenPaymentOptions,
        @Query("uid") Long uid,
        @Query("allowPrepaidForNoAuth") String allowPrepaidForNoAuth,
        @Query("rgb") String rgb,
        @Query("minifyOutlets") Boolean minifyOutlets,
        @Query("force-delivery-id") Long forceDeliveryId,
        @Body CartRequest cartBody
    );

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Headers({"X-Hit-Rate-Group: UNLIMIT"})
    @POST("/checkout")
    Call<ResponseBody> checkout(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("X-Market-Rearrfactors") String rearrFactors,
        @Query("showHiddenPaymentOptions") Boolean showHiddenPaymentOptions,
        @Query("uid") Long uid,
        @Query("allowPrepaidForNoAuth") String allowPrepaidForNoAuth,
        @Query("rgb") String rgb,
        @Query("minifyOutlets") Boolean minifyOutlets,
        @Query("force-delivery-id") Long forceDeliveryId,
        @Query("sandbox") Boolean sandbox,
        @Query("context") String context,
        @Body CheckoutRequest checkoutBody
    );

    @SuppressWarnings("checkstyle:ParameterNumber")
    @POST("/orders/{orderId}/payment")
    Call<ResponseBody> pay(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("Authorization") String authToken,
        @Header("X-Hit-Rate-Group") String rateGroup,
        @Path("orderId") Long orderId,
        @Query("uid") Long uid,
        @Query("returnPath") String returnPath,
        @Query("sandbox") Boolean sandbox
    );

    @POST("/payments/{paymentId}/notify")
    Call<ResponseBody> notifyFake(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("Authorization") String authToken,
        @Header("X-Hit-Rate-Group") String rateGroup,
        @Path("paymentId") String paymentId,
        @Query("sandbox") Boolean sandbox,
        @Query("status") BalanceStatus status
    );

    @POST("/orders/{orderId}/cancellation-request")
    Call<ResponseBody> cancellationRequest(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("shopId") Long shopId,
        @Body CancellationRequest cancellationBody
    );

    @POST("/orders/{orderId}/status")
    Call<ResponseBody> changeOrderStatus(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("shopId") Long shopId,
        @Query("status") OrderStatus status,
        @Query("substatus") OrderSubstatus substatus
    );

    @GET("/orders/{orderId}")
    Call<ResponseBody> getOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("partials") OptionalOrderPart[] partials
    );

    @POST("/orders/{orderId}/delivery")
    Call<ResponseBody> updateDelivery(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("rgb") Color rgb,
        @Query("shopId") Long shopId,
        @Body UpdateDeliveryRequest deliveryServiceId
    );

    @SuppressWarnings("checkstyle:ParameterNumber")
    @POST("/orders/{orderId}/edit")
    Call<ResponseBody> edit(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("rgb") Color rgb,
        @Query("shopId") Long shopId,
        @Query("businessId") Long businessId,
        @Body OrderEditRequest orderEditRequest
    );

    @SuppressWarnings("checkstyle:ParameterNumber")
    @POST("/orders/{orderId}/edit-options")
    Call<ResponseBody> getEditOptions(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("rgb") Color rgb,
        @Query("shopId") Long shopId,
        @Query("businessId") Long businessId,
        @Body OrderEditOptionsRequest orderEditOptionsRequest
    );

    @POST("/orders/{orderId}/returns/create")
    Call<ReturnResponse> initReturn(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("orderId") Long orderId,
        @Query("clientRole") String clientRole,
        @Query("clientId") Long clientId,
        @Query("rgb") Color rgb,
        @Query("uid") Long uid,
        @Body Return returnRequest
    );
}
