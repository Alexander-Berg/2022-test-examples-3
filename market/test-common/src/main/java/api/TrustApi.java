package api;

import dto.requests.trust.TrustSupplyDataRequest;
import dto.responses.trust.TrustResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TrustApi {

    @POST("/supply_payment_data")
    Call<TrustResponse> supplyPaymentData(
        @Body TrustSupplyDataRequest supplyPaymentDataBody
    );
}
