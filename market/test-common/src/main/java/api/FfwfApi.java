package api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import ru.yandex.market.ff.client.dto.CreateSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;

public interface FfwfApi {

    @GET("/requests/{id}")
    Call<ShopRequestDetailsDTO> getRequest(
        @Path("id") long id
    );

    @Headers(
        {
            "User-Roles: SHOP_REQUESTS_CALENDARING",
            "User-Login: Autotest"
        }
    )
    @POST("/upload-request/supply")
    Call<ShopRequestDTO> createXdocRequest(
        @Body CreateSupplyRequestDTO createSupplyRequestDTO
    );

}
