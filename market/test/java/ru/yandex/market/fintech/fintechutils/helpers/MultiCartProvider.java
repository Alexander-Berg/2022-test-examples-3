package ru.yandex.market.fintech.fintechutils.helpers;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.mj.generated.server.model.MultiCartDto;
import ru.yandex.mj.generated.server.model.OrderDto;

public final class MultiCartProvider {

    public static MultiCartDtoBuilder multiCart() {
        return MultiCartDtoBuilder.builder()
                .withHasPuid(true)
                .withPuid(99887766L)
                .withOrderYandexPlusUserFlag(true)
                .withMultiCart(List.of(OrderProvider.order().build()));
    }

    public static MultiCartDtoBuilder builder() {
        return new MultiCartDtoBuilder();
    }

    public static final class MultiCartDtoBuilder {
        private Boolean orderYandexPlusUserFlag;
        private Boolean hasPuid;
        private Long puid;
        private List<OrderDto> multiCart = new ArrayList<>();

        private MultiCartDtoBuilder() {
        }

        public static MultiCartDtoBuilder builder() {
            return new MultiCartDtoBuilder();
        }

        public MultiCartDtoBuilder withOrderYandexPlusUserFlag(Boolean orderYandexPlusUserFlag) {
            this.orderYandexPlusUserFlag = orderYandexPlusUserFlag;
            return this;
        }

        public MultiCartDtoBuilder withHasPuid(Boolean hasPuid) {
            this.hasPuid = hasPuid;
            return this;
        }

        public MultiCartDtoBuilder withPuid(Long puid) {
            this.puid = puid;
            return this;
        }

        public MultiCartDtoBuilder withMultiCart(List<OrderDto> multiCart) {
            this.multiCart = multiCart;
            return this;
        }

        public MultiCartDtoBuilder withMultiCart(OrderDto orderDto) {
            this.multiCart = List.of(orderDto);
            return this;
        }

        public MultiCartDto build() {
            MultiCartDto multiCartDto = new MultiCartDto();
            multiCartDto.setOrderYandexPlusUserFlag(orderYandexPlusUserFlag);
            multiCartDto.setHasPuid(hasPuid);
            multiCartDto.setPuid(puid);
            multiCartDto.setMultiCart(multiCart);
            return multiCartDto;
        }
    }
}
