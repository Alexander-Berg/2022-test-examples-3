package factory.orderservice;

import java.util.List;

import ru.yandex.market.order_service.client.model.DeliveryItemDto;
import ru.yandex.market.order_service.client.model.Dimensions;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum Items {
    DEFAULT {
        @Override
        public List<DeliveryItemDto> getItems() {
            return List.of(
                new DeliveryItemDto()
                    .ssku("217176139.alisa3p")
                    .warehouseId(171L)
                    .feedId(475690L)
                    .offerName("dd")
                    .categoryId(1L)
                    .msku(217176139L)
                    .requiredCount(3)
                    .price(111000L)
                    .cargoTypes(List.of(950))
                    .categories(List.of(1L))
                    .dimensions(
                        new Dimensions()
                            .weight(10)
                            .depth(25)
                            .width(13)
                            .height(13)
                    )
            );
        }
    },
    ;

    public abstract List<DeliveryItemDto> getItems();
}
