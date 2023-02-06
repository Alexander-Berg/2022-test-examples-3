package client;

import api.L4GApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.logistics4go.client.model.CancelOrderResponse;
import ru.yandex.market.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics4go.client.model.CreateOrderResponse;
import ru.yandex.market.logistics4go.client.model.GetOrderResponse;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/l4g.properties")
@Slf4j
public class L4GClient {
    private final L4GApi l4GApi;
    @Property("l4g.host")
    private String host;

    public L4GClient() {
        PropertyLoader.newInstance().populate(this);
        l4GApi = RETROFIT.getRetrofit(host).create(L4GApi.class);
    }

    @SneakyThrows
    public GetOrderResponse getOrder(Long orderId) {
        Response<GetOrderResponse> response = l4GApi.getOrder(
                TVM.INSTANCE.getServiceTicket(TVM.L4G),
                orderId
            )
            .execute();
        Assertions.assertTrue(
            response.isSuccessful(),
            "Запрос получения заказа " + orderId + " не успешен"
        );
        Assertions.assertNotNull(
            response.body(),
            "Пустое тело ответа на запрос получения заказа " + orderId
        );
        return response.body();
    }

    @SneakyThrows
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Response<CreateOrderResponse> response = l4GApi.createOrder(
                TVM.INSTANCE.getServiceTicket(TVM.L4G),
                request
            )
            .execute();
        Assertions.assertTrue(
            response.isSuccessful(),
            "Запрос создания заказа не успешен"
        );
        Assertions.assertNotNull(
            response.body(),
            "Пустое тело ответа на запрос создания заказа"
        );
        return response.body();
    }

    @SneakyThrows
    public CancelOrderResponse cancelOrder(long orderId) {
        Response<CancelOrderResponse> response = l4GApi.cancelOrder(
                TVM.INSTANCE.getServiceTicket(TVM.L4G),
                orderId
            )
            .execute();
        Assertions.assertTrue(
            response.isSuccessful(),
            "Запрос отмены заказа " + orderId + " не успешен"
        );
        Assertions.assertNotNull(
            response.body(),
            "Пустое тело ответа на запрос отмены заказа " + orderId
        );
        return response.body();
    }
}
