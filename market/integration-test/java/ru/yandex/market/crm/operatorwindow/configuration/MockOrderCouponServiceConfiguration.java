package ru.yandex.market.crm.operatorwindow.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.operatorwindow.services.loyalty.OrderCouponsService;
import ru.yandex.market.crm.operatorwindow.utils.MockCouponScriptServiceApi;

import static org.mockito.Mockito.mock;

@Configuration
public class MockOrderCouponServiceConfiguration {

    @Bean
    public OrderCouponsService orderCouponsService() {
        return mock(OrderCouponsService.class);
    }

    @Bean
    MockCouponScriptServiceApi mockCouponScriptServiceApi(OrderCouponsService api) {
        return new MockCouponScriptServiceApi(api);
    }
}
