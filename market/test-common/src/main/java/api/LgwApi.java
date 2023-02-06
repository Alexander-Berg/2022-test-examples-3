package api;

import dto.responses.lgw.TasksResponse;
import dto.responses.lgw.message.get_order.GetOrderRequest;
import dto.responses.lgw.message.get_order.Order;
import dto.responses.lgw.task.TaskResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderDeliveryDateRequest;

public interface LgwApi {

    @GET("/support/tasks")
    Call<TasksResponse> getTasks(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Query("entityId") String entityId,
        @Query("size") int size
    );

    @GET("/support/tasks/{taskId}")
    Call<TaskResponse> getTask(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Path("taskId") Long taskId
    );

    @POST("/task/delivery/order-delivery-date/update")
    Call<Void> postUpdateDeliveryDate(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Body UpdateOrderDeliveryDateRequest deliveryDateRequest
    );

    @POST("/delivery/getOrder")
    Call<Order> dsGetOrder(
        @Header("X-Ya-Service-Ticket") String ticket,
        @Body GetOrderRequest getOrderRequest
    );
}
