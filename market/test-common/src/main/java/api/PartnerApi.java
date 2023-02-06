package api;

import dto.requests.partner.PackOrderRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface PartnerApi {

    @PUT("/v2/campaigns/{campaignId}/orders/{orderId}/delivery/parcels/{parcelId}/boxes")
    Call<ResponseBody> packOrder(
        @Header("Cookie") String cookie,
        @Header("x-authorizationservice") String authorizationService,
        @Path("campaignId") Long campaignId,
        @Path("orderId") Long orderId,
        @Path("parcelId") Long parcelId,
        @Body PackOrderRequest packOrderBody
    );
}
