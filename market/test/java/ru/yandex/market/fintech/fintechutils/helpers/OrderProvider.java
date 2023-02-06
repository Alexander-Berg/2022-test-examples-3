package ru.yandex.market.fintech.fintechutils.helpers;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.mj.generated.server.model.OrderDeliveryDto;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderItemDto;

public final class OrderProvider {

    public static OrderDtoBuilder order() {
        return OrderDtoBuilder.builder()
                .withOrderId(0L)
                .withLabel("label1")
                .withSupplierType(OrderDto.SupplierTypeEnum.THIRD_PARTY)
                .withGmv("2000")
                .withDelivery(OrderDeliveryProvider.delivery().build())
                .withItems(List.of(OrderItemProvider.item().build()))
                .withBusinessScheme(OrderDto.BusinessSchemeEnum.DROPSHIP);
    }

    public static OrderDtoBuilder builder() {
        return new OrderDtoBuilder();
    }

    public static final class OrderDtoBuilder {
        private Long orderId;
        private String label;
        private OrderDto.SupplierTypeEnum supplierType;
        private String gmv;
        private OrderDeliveryDto delivery;
        private List<OrderItemDto> items = new ArrayList<>();
        private OrderDto.BusinessSchemeEnum businessScheme;

        private OrderDtoBuilder() {
        }

        public static OrderDtoBuilder builder() {
            return new OrderDtoBuilder();
        }

        public OrderDtoBuilder withOrderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderDtoBuilder withLabel(String label) {
            this.label = label;
            return this;
        }

        public OrderDtoBuilder withSupplierType(OrderDto.SupplierTypeEnum supplierType) {
            this.supplierType = supplierType;
            return this;
        }

        public OrderDtoBuilder withGmv(String gmv) {
            this.gmv = gmv;
            return this;
        }

        public OrderDtoBuilder withDelivery(OrderDeliveryDto delivery) {
            this.delivery = delivery;
            return this;
        }

        public OrderDtoBuilder withItems(List<OrderItemDto> items) {
            this.items = items;
            return this;
        }

        public OrderDtoBuilder withBusinessScheme(OrderDto.BusinessSchemeEnum businessScheme) {
            this.businessScheme = businessScheme;
            return this;
        }

        public OrderDto build() {
            OrderDto orderDto = new OrderDto();
            orderDto.setOrderId(orderId);
            orderDto.setLabel(label);
            orderDto.setSupplierType(supplierType);
            orderDto.setGmv(gmv);
            orderDto.setDelivery(delivery);
            orderDto.setItems(items);
            orderDto.setBusinessScheme(businessScheme);
            return orderDto;
        }
    }
}
