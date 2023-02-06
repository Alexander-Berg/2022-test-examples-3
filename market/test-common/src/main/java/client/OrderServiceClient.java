package client;

import api.OrderServiceApi;
import lombok.SneakyThrows;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.order_service.client.model.ActorType;
import ru.yandex.market.order_service.client.model.ChangeOrderStatus;
import ru.yandex.market.order_service.client.model.ChangeOrderStatusResponse;
import ru.yandex.market.order_service.client.model.CreateExternalOrderRequest;
import ru.yandex.market.order_service.client.model.CreateExternalOrderResponse;
import ru.yandex.market.order_service.client.model.GetDeliveryOptionsRequest;
import ru.yandex.market.order_service.client.model.GetDeliveryOptionsResponse;
import ru.yandex.market.order_service.client.model.GetOrderLogisticsResponse;
import ru.yandex.market.order_service.client.model.OrderSubStatus2;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/mbios.properties")
public class OrderServiceClient {
    private final OrderServiceApi orderServiceApi;

    @Property("mbios.host")
    private String host;

    public OrderServiceClient() {
        PropertyLoader.newInstance().populate(this);
        orderServiceApi = RETROFIT.getRetrofit(host).create(OrderServiceApi.class);
    }

    @SneakyThrows
    public GetDeliveryOptionsResponse getDeliveryOptions(long shopId, GetDeliveryOptionsRequest request) {
        return orderServiceApi.getDeliveryOptions(TVM.INSTANCE.getServiceTicket(TVM.MBIOS), shopId, request)
            .execute()
            .body();
    }

    @SneakyThrows
    public CreateExternalOrderResponse createOrder(
        long shopId,
        CreateExternalOrderRequest request
    ) {
        return orderServiceApi.createOrder(TVM.INSTANCE.getServiceTicket(TVM.MBIOS), shopId, request)
            .execute()
            .body();
    }

    @SneakyThrows
    public GetOrderLogisticsResponse getOrder(long shopId, long orderId) {
        return orderServiceApi.getOrder(
            TVM.INSTANCE.getServiceTicket(TVM.MBIOS),
            shopId,
            orderId
        )
            .execute()
            .body();
    }

    @SneakyThrows
    public ChangeOrderStatusResponse cancelOrder(long shopId, long orderId, OrderSubStatus2 subStatus) {
        return orderServiceApi.changeOrderStatus(
            TVM.INSTANCE.getServiceTicket(TVM.MBIOS),
            shopId,
            orderId,
            ChangeOrderStatus.CANCELLED,
            ActorType.PI,
            123L,
            subStatus
        )
            .execute()
            .body();
    }
}
