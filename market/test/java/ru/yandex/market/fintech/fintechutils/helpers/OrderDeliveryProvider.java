package ru.yandex.market.fintech.fintechutils.helpers;


import java.time.LocalDate;

import ru.yandex.mj.generated.server.model.OrderDeliveryDto;

public final class OrderDeliveryProvider {

    public static OrderDeliveryDtoBuilder delivery() {
        return OrderDeliveryDtoBuilder.builder()
                .withRegionId(213L)
                .withDeliveryDate(LocalDate.now().plusDays(3))
                .withDeliveryType(OrderDeliveryDto.DeliveryTypeEnum.COURIER_DELIVERY);
    }

    public static final class OrderDeliveryDtoBuilder {
        private Long regionId;
        private LocalDate deliveryDate;
        private OrderDeliveryDto.DeliveryTypeEnum deliveryType;

        private OrderDeliveryDtoBuilder() {
        }

        public static OrderDeliveryDtoBuilder builder() {
            return new OrderDeliveryDtoBuilder();
        }

        public OrderDeliveryDtoBuilder withRegionId(Long regionId) {
            this.regionId = regionId;
            return this;
        }

        public OrderDeliveryDtoBuilder withDeliveryDate(LocalDate deliveryDate) {
            this.deliveryDate = deliveryDate;
            return this;
        }

        public OrderDeliveryDtoBuilder withDeliveryType(OrderDeliveryDto.DeliveryTypeEnum deliveryType) {
            this.deliveryType = deliveryType;
            return this;
        }

        public OrderDeliveryDto build() {
            OrderDeliveryDto orderDeliveryDto = new OrderDeliveryDto();
            orderDeliveryDto.setRegionId(regionId);
            orderDeliveryDto.setDeliveryDate(deliveryDate);
            orderDeliveryDto.setDeliveryType(deliveryType);
            return orderDeliveryDto;
        }
    }
}
