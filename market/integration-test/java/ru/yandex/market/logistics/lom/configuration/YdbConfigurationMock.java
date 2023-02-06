package ru.yandex.market.logistics.lom.configuration;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.ydb.integration.YdbTemplate;

@Configuration
@MockBean(YdbTemplate.class)
public class YdbConfigurationMock {
}
