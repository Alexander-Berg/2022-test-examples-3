package api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import ru.yandex.market.logistics4shops.client.model.CreateExcludeOrderFromShipmentRequest;
import ru.yandex.market.logistics4shops.client.model.ExcludeOrderRequestListDto;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;
import ru.yandex.market.logistics4shops.client.model.OutboundsSearchRequest;

public interface L4SApi {
    @POST("/shipments/{shipmentId}/exclude-order")
    Call<ExcludeOrderRequestListDto> excludeOrdersFromShipment(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("shipmentId") Long shipmentId,
        @Body CreateExcludeOrderFromShipmentRequest request
    );

    @PUT("/outbounds/search")
    Call<OutboundsListDto> searchOutbounds(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body OutboundsSearchRequest request
    );
}
