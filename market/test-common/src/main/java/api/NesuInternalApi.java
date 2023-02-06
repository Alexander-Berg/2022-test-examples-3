package api;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.logistics.nesu.client.model.SenderDto;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.logistics.nesu.client.model.page.Page;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentFilter;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentSearchDto;

public interface NesuInternalApi {

    @POST("internal/shops/register")
    Call<ResponseBody> registerShop(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body RegisterShopDto shopInfo
    );

    @POST("internal/shops/{shopId}/configure")
    Call<ResponseBody> configureShop(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("shopId") Long shopId,
        @Body ConfigureShopDto configureShopDto
    );

    @PUT("internal/shops/search")
    Call<List<ShopWithSendersDto>> searchShopWithSenders(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body ShopWithSendersFilter filter
    );

    @GET("internal/senders")
    Call<List<SenderDto>> getSenders(
        @Header("X-Ya-Service-Ticket") String tvmTicket
    );

    @GET("internal/sender/{senderId}")
    Call<SenderDto> getSender(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("senderId") Long senderId
    );

    @GET("internal/partner/shipments/search")
    Call<Page<PartnerShipmentSearchDto>> searchPartnerShipments(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("userId") Long userId,
        @Query("shopId") Long shopId,
        @Body PartnerShipmentFilter filter
    );

    @GET("internal/partner/shipments/{shipmentId}")
    Call<PartnerShipmentDto> getShipment(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("shipmentId") Long shipmentId,
        @Query("userId") Long userId,
        @Query("shopId") Long shopId
    );

    @POST("internal/partner/shipments/{shipmentId}/confirm")
    Call<ResponseBody> confirmShipment(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("shipmentId") Long shipmentId,
        @Query("userId") Long userId,
        @Query("shopId") Long shopId,
        @Body PartnerShipmentConfirmRequest request
    );

    @GET("internal/partner/shipments/{shipmentId}/transportation-waybill")
    Call<ResponseBody> generateTransportationWaybill(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("shipmentId") Long shipmentId,
        @Query("userId") Long userId,
        @Query("shopId") Long shopId
    );
}
