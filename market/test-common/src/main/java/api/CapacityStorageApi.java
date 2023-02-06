package api;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

import ru.yandex.market.logistics.cs.domain.dto.InternalEventDto;

public interface CapacityStorageApi {

    @POST("/snapshot")
    Call<ResponseBody> snapshot();

    @GET("/admin/internal-order-events")
    Call<List<InternalEventDto>> getOrderEvents(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Query("orderId") Long orderId
    );
}
