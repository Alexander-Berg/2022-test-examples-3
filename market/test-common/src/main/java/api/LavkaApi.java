package api;

import dto.requests.lavka.LavkaCreateOrderRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LavkaApi {

    @POST(" /admin/orders/v1/make")
    Call<ResponseBody> makeOrder(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Header("X-Idempotency-Token") String token,
        @Body LavkaCreateOrderRequest makeOrder
    );
}
