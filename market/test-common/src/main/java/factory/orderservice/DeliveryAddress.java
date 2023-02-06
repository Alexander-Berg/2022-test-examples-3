package factory.orderservice;

import ru.yandex.market.order_service.client.model.DeliveryAddressDto;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum DeliveryAddress {
    DEFAULT {
        @Override
        public DeliveryAddressDto getAddress() {
            return new DeliveryAddressDto()
                .city("Москва")
                .street("Льва Толстого")
                .house("16");
        }
    },
    ;

    public abstract DeliveryAddressDto getAddress();
}
