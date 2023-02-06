package client;

import java.time.LocalDate;
import java.time.LocalTime;

import api.LgwApi;
import dto.responses.lgw.TasksResponse;
import dto.responses.lgw.message.get_order.GetOrderRequest;
import dto.responses.lgw.message.get_order.Order;
import dto.responses.lgw.task.TaskResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderDeliveryDateRequest;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath({"delivery/lgw.properties"})
public class LgwClient {

    private final LgwApi lgwApi;
    @Property("lgw.host")
    private String host;

    public LgwClient() {
        PropertyLoader.newInstance().populate(this);
        lgwApi = RETROFIT.getRetrofit(host).create(LgwApi.class);
    }

    @SneakyThrows
    public TasksResponse getTasks(String entityId) {
        Response<TasksResponse> execute = lgwApi.getTasks(TVM.INSTANCE.getServiceTicket(TVM.LGW), entityId, 100)
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос получения таксок с entityId = " + entityId);
        Assertions.assertNotNull(execute.body(), "Не удалось получить задачи с лгв entityId=" + entityId);
        return execute.body();
    }

    @SneakyThrows
    public TaskResponse getTask(Long taskId) {
        Response<TaskResponse> execute = lgwApi.getTask(TVM.INSTANCE.getServiceTicket(TVM.LGW), taskId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить задачу с лгв taskId=" + taskId);
        Assertions.assertNotNull(execute.body(), "Не удалось получить задачу с лгв taskId=" + taskId);
        return execute.body();
    }

    @SneakyThrows
    public void postUpdateDeliveryDate(String orderId,
                                       long partnerId,
                                       String deliveryId,
                                       LocalDate dateTime,
                                       LocalTime fromTime,
                                       LocalTime endTime) {
        TimeInterval timeInterval = null;
        if (fromTime != null && endTime != null) {
            timeInterval = TimeInterval.of(fromTime, endTime);
        }
        OrderDeliveryDate orderDeliveryDate = new OrderDeliveryDate(
            ResourceId.builder()
                .setYandexId(orderId)
                .setPartnerId(deliveryId)
                .build(),
            DateTime.fromLocalDateTime(dateTime.atStartOfDay()),
            timeInterval,
            ""
        );

        UpdateOrderDeliveryDateRequest request = new UpdateOrderDeliveryDateRequest(
            orderDeliveryDate,
            new Partner(partnerId),
            0L
        );
        Response<Void> execute = lgwApi.postUpdateDeliveryDate(TVM.INSTANCE.getServiceTicket(TVM.LGW), request)
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "");
    }

    @SneakyThrows
    public Order dsGetOrder(GetOrderRequest getOrderRequest) {
        Response<Order> response = lgwApi.dsGetOrder(
            TVM.INSTANCE.getServiceTicket(TVM.LGW),
            getOrderRequest
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Неуспешный запрос получения заказа по DS-Api");
        Assertions.assertNotNull(response.body(), "Пустой ответ при получении заказа по DS-Api");
        return response.body();
    }
}
