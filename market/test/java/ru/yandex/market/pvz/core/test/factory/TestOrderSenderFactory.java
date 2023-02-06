package ru.yandex.market.pvz.core.test.factory;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.order.model.sender.OrderSender;
import ru.yandex.market.pvz.core.domain.order.model.sender.OrderSenderRepository;

public class TestOrderSenderFactory extends TestObjectFactory {

    @Autowired
    private OrderSenderRepository orderSenderRepository;

    public OrderSender createOrderSender() {
        return orderSenderRepository.save(buildOrderSender(OrderSenderParams.builder().build()));
    }

    public OrderSender createOrderSender(OrderSenderParams params) {
        return orderSenderRepository.save(buildOrderSender(params));
    }

    private OrderSender buildOrderSender(OrderSenderParams params) {
        return OrderSender.builder()
                .yandexId(params.getYandexId())
                .incorporation(params.getIncorporation())
                .phone(params.getPhone())
                .build();
    }

    @Data
    @Builder
    public static class OrderSenderParams {

        public static final String DEFAULT_INCORPORATION = "ООО Яндекс Маркет";
        public static final String DEFAULT_PHONE = "+7 800 555-35-35";

        @Builder.Default
        private String yandexId = randomString(16);

        @Builder.Default
        private String incorporation = DEFAULT_INCORPORATION;

        @Builder.Default
        private String phone = DEFAULT_PHONE;
    }
}
