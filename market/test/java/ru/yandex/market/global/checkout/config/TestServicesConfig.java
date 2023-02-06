package ru.yandex.market.global.checkout.config;

import java.time.Clock;

import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.global.checkout.domain.order.OrderDeliveryRepository;
import ru.yandex.market.global.checkout.domain.order.OrderItemRepository;
import ru.yandex.market.global.checkout.domain.order.OrderPaymentRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.factory.TestCartFactory;
import ru.yandex.market.global.checkout.factory.TestElasticOfferFactory;
import ru.yandex.market.global.checkout.factory.TestGetUnitedOfferFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.factory.TestReferralFactory;
import ru.yandex.market.global.checkout.factory.TestShopFactory;
import ru.yandex.market.global.checkout.factory.TestUserFactory;
import ru.yandex.market.global.checkout.util.TestDataService;

@Configuration
public class TestServicesConfig {
    @Bean
    TestDataService testDataService(
            DSLContext dslContext,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderDeliveryRepository orderDeliveryRepository,
            OrderPaymentRepository orderPaymentRepository
    ) {
        return new TestDataService(
                dslContext,
                orderRepository,
                orderItemRepository,
                orderDeliveryRepository,
                orderPaymentRepository
        );
    }

    @Bean
    TestUserFactory testUserFactory() {
        return new TestUserFactory();
    }

    @Bean
    TestOrderFactory testOrderFactory() {
        return new TestOrderFactory();
    }

    @Bean
    TestPromoFactory testPromoFactory() {
        return new TestPromoFactory();
    }

    @Bean
    TestReferralFactory testReferralFactory() {
        return new TestReferralFactory();
    }

    @Bean
    TestShopFactory testShopFactory(Clock clock) {
        return new TestShopFactory(clock);
    }

    @Bean
    TestCartFactory testCartFactory() {
        return new TestCartFactory();
    }

    @Bean
    TestGetUnitedOfferFactory testGetUnitedOfferFactory() {
        return new TestGetUnitedOfferFactory();
    }

    @Bean
    TestElasticOfferFactory testElasticOfferFactory() {
        return new TestElasticOfferFactory();
    }
}
