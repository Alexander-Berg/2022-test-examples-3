package api;

import dto.requests.wms.ChangeShipmentDateRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface WmsApi {
    @Headers({"X-Token: xxxxxxxxxxxxxxxxxxxxDimaKotovProstoKosmosSofTestxxxxxxxxxxxxxxxx"})
    @POST("/datacreator/order/changeShippingDate")
    Call<ResponseBody> changeShipmentDate(
        @Body ChangeShipmentDateRequest changeShipmentDateBody
    );

    @Headers({"username: ad4",
        "password: !234qweR",
        "Cookie: uid=f/2m4mDV8ainxBW9C4uXAg=="})
    @POST("/scheduler2/manage/job-group/orders/jobs/CalculateAndUpdateOrdersStatus/execute")
    Call<ResponseBody> calculateAndUpdateOrdersStatus();
}
