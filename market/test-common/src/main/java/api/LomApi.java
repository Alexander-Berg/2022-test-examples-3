package api;

import java.util.Set;

import dto.responses.lom.admin.business_process.BusinessProcessesResponse;
import dto.responses.lom.admin.order.AdminLomOrderResponse;
import dto.responses.lom.admin.order.OrdersResponse;
import dto.responses.lom.admin.order.route.RouteResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.logistics.lom.model.dto.IdDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;

public interface LomApi {

    @POST("/orders")
    Call<OrderDto> createOrder(
        @Body WaybillOrderRequestDto orderDto
    );

    @PUT("/orders/search")
    Call<OrdersResponse> orderSearch(
        @Body OrderSearchFilter body
    );

    @GET("/orders/{orderId}")
    Call<OrderDto> getOrder(
        @Path("orderId") Long orderId,
        @Query("optionalParts") Set<OptionalOrderPart> optionalParts
    );

    @GET("/admin/orders/{orderId}")
    Call<AdminLomOrderResponse> getAdminLomOrder(
        @Path("orderId") Long orderId
    );

    @GET("/admin/routes/{routeId}")
    Call<RouteResponse> getAdminLomRoute(
        @Path("routeId") Long routeId
    );

    @GET("/admin/business-processes")
    Call<BusinessProcessesResponse> getAdminBusinessProcesses(
        @Query("orderId") Long orderId
    );

    @POST("/admin/business-processes/retry")
    Call<ResponseBody> retryBusinessProcess(
        @Body IdDto idDto
    );
}
