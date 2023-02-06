package ru.yandex.market.ydb.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.ydb.integration.context.config.TestYdbConfig;
import ru.yandex.market.ydb.integration.context.initializer.YdbContainerContextInitializer;

@ContextConfiguration(
        classes = TestYdbConfig.class,
        initializers = YdbContainerContextInitializer.class
)
@ExtendWith(SpringExtension.class)
public class ServiceTestBase {

    @Autowired
    protected TestableClock clock;
    @Autowired
    private DataCleaner dataCleaner;

    @AfterEach
    private void cleanData() {
        dataCleaner.cleanData();
        clock.clearFixed();
    }
}
