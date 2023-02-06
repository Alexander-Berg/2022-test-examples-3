package ru.yandex.market.replenishment.autoorder.integration.test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.application.properties.AppPropertyContextInitializer;
import ru.yandex.replenishment.autoorder.integration.test.config.PostgresDataSourceConfig;
import ru.yandex.replenishment.autoorder.integration.test.config.YqlDataSourceConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {PostgresDataSourceConfig.class, YqlDataSourceConfig.class},
        initializers = AppPropertyContextInitializer.class)
public abstract class AbstractIntegrationTest {
    protected static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);
}
