package api;

import com.fasterxml.jackson.databind.JsonNode;
import dto.requests.partner.GetReferencePickupPointsRequest;
import dto.responses.logplatform.admin.request_get.RequestGetResponse;
import dto.responses.logplatform.admin.station_list.StationListResponse;
import dto.responses.logplatform.admin.station_tag_list.StationTagListResponse;
import dto.responses.logplatform.cancel_order.RequestIdDto;
import dto.responses.logplatform.create_order.ConfirmOrderResponse;
import dto.responses.logplatform.create_order.CreateOrderResponse;
import dto.responses.logplatform.create_order.OfferIdDto;
import dto.responses.logplatform.order_history.OrderStatusHistoryResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.delivery.response.GetReferencePickupPointsResponse;

public interface LogPlatformTaxiApi {

    @POST("/platform/ds")
    @Headers({"Content-Type: application/xml"})
    Call<ResponseWrapper<GetReferencePickupPointsResponse>> getReferencePickupPoints(
        @Body GetReferencePickupPointsRequest body
    );

    //Просмотреть станции
    @GET("api/admin/station/list")
    Call<StationListResponse> getListOfStations(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("limit") Long limit,
        @Query("dump") String dump,
        @Query("capacity") String capacity,
        @Query("station_id") String stationId,
        @Query("operator_id") String operatorId
    );

    //Просмотр тегов по станции
    @GET("/api/admin/station/tag/list")
    @Headers({"Authorization: system-admin"})
    Call<StationTagListResponse> getTagsOfStation(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("ids") String ids
    );

    //Освободить тег и удалить
    @GET("platform/supply/tags/clean")
    Call<ResponseBody> cleanTagAndDelete(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("tag_ids") String tagIds
    );

    //Активация по request_id - сама находит теги, активирует их (получается, что теги = тег1 со станции БЕРУ и тег2
    // со станции С7, а request_id = id заказа в системе такси)
    @GET("platform/supply/tags/activate_by_request")
    Call<ResponseBody> findAndActivationTags(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("steps_count") Long stepsCount,
        @Query("request_id") String requestId
    );

    //Вся информация по заказу
    @GET("api/admin/request/get")
    Call<RequestGetResponse> getInformationByRequest(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("request_id") String requestId
    );

    @POST("api/b2b/platform/offers/create")
    @Headers({"Content-Type: application/json"})
    Call<CreateOrderResponse> createOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("X-B2B-Client-id") String authorization,
        @Body JsonNode createOrderRequest
    );

    @POST("api/b2b/platform/offers/confirm")
    @Headers({"Content-Type: application/json"})
    Call<ConfirmOrderResponse> confirmOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("X-B2B-Client-id") String authorization,
        @Body OfferIdDto offerIdDto
    );

    @POST("api/b2b/platform/request/cancel")
    @Headers({"Content-Type: application/json"})
    Call<ResponseBody> cancelOrder(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("X-B2B-Client-id") String authorization,
        @Body RequestIdDto requestIdDto
    );

    @GET("api/b2b/platform/request/history")
    @Headers({"Content-Type: application/json"})
    Call<OrderStatusHistoryResponse> getOrderStatusHistory(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Header("X-B2B-Client-id") String authorization,
        @Query("request_id") String requestId
    );
}
