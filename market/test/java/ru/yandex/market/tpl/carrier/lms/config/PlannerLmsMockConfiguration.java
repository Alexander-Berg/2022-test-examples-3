package ru.yandex.market.tpl.carrier.lms.config;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.management.client.LMSClient;

@MockBean(
        LMSClient.class
)
@Configuration
public class PlannerLmsMockConfiguration {
}
