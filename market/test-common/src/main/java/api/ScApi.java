package api;

import dto.requests.scapi.ApiSortableSortRequest;
import dto.requests.scapi.InboundsLinkRequest;
import dto.responses.inbounds.ScInboundsAcceptResponse;
import dto.responses.scapi.orders.ApiOrderDto;
import dto.responses.scapi.sortable.SortableSort;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ScApi {

    @GET("/api/inbounds/{externalId}/accept")
    Call<ScInboundsAcceptResponse> inboundsAccept(
        @Path("externalId") String inboundYandexId,
        @Header("Authorization") String token
    );

    @POST("/api/inbounds/{externalId}/link")
    Call<ResponseBody> inboundsLink(
        @Path("externalId") String inboundId,
        @Body InboundsLinkRequest inboundsLinkBody,
        @Header("Authorization") String token
    );

    @PUT("/api/orders/accept")
    Call<ApiOrderDto> ordersAccept(
        @Body Object sortableId,
        @Header("Authorization") String token
    );

    @PUT("/api/orders/{id}")
    Call<ApiOrderDto> sortOrder(
        @Path("id") Long id,
        @Body Object cellId,
        @Header("Authorization") String token
    );

    @PUT("/api/inbounds/{externalId}/fixInbound")
    Call<ResponseBody> fixInbound(
        @Path("externalId") String inboundId,
        @Header("Authorization") String token
    );

    @PUT("/api/sortable/beta/sort")
    Call<SortableSort> sortableSort(
        @Body ApiSortableSortRequest sortableSortRequest,
        @Header("Authorization") String token
    );
}
