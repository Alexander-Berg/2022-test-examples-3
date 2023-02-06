package ru.yandex.market.crm.operatorwindow.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.operatorwindow.domain.order.OrderRules;
import ru.yandex.market.crm.operatorwindow.utils.MockOrderRules;

import static org.mockito.Mockito.mock;

@Configuration
public class MockOrderRulesConfiguration {

    @Bean
    public OrderRules orderRules() {
        return mock(OrderRules.class);
    }

    @Bean
    MockOrderRules mockOrderRules(OrderRules orderRules) {
        return new MockOrderRules(orderRules);
    }
}
