package api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface TaxiApi {

    @Headers("Content-Type: application/json")
    @POST("/api/b2b/external_platform/transfer/activate")
    Call<ResponseBody> transferActivate(
        @Query("transfer_id") String transferId,
        @Query("request_id") String requestId
    );
}
