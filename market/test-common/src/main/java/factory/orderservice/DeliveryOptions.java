package factory.orderservice;

import java.time.OffsetDateTime;

import ru.yandex.market.order_service.client.model.GetDeliveryOptionsRequest;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum DeliveryOptions {
    DEFAULT {
        @Override
        public GetDeliveryOptionsRequest getRequest() {
            return new GetDeliveryOptionsRequest()
                .deliveryStartTime(OffsetDateTime.now())
                .deliveryAddress(DeliveryAddress.DEFAULT.getAddress())
                .deliveryItems(Items.DEFAULT.getItems());
        }
    },
    ;

    public abstract GetDeliveryOptionsRequest getRequest();
}
