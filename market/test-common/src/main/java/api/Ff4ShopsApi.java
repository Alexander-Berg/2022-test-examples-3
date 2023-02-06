package api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.ff4shops.api.model.auth.ClientRole;
import ru.yandex.market.ff4shops.api.model.order.OrderRemovalPermissionsDto;

public interface Ff4ShopsApi {

    @GET("/orders/{orderId}/removalPermissions")
    Call<OrderRemovalPermissionsDto> getRemovalPermissions(
        @Path("id") long orderId,
        @Query("clientId") long clientId,
        @Query("shopId") long shopId,
        @Query("clientRole") ClientRole clientRole
    );
}
