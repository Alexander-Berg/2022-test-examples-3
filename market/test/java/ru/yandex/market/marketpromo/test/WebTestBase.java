package ru.yandex.market.marketpromo.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.marketpromo.core.application.context.CategoryInterfacePromo;
import ru.yandex.market.marketpromo.core.test.config.ApplicationCoreTaskBasicSupportConfig;
import ru.yandex.market.marketpromo.core.test.context.initializer.YdbContainerContextInitializer;
import ru.yandex.market.marketpromo.test.config.ApplicationTestConfig;
import ru.yandex.market.ydb.integration.DataCleaner;

@SpringBootTest(
        classes = {
                ApplicationTestConfig.class,
                ApplicationCoreTaskBasicSupportConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(
        initializers = YdbContainerContextInitializer.class
)
public abstract class WebTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    @CategoryInterfacePromo
    protected ObjectMapper objectMapper;

    @Autowired
    private DataCleaner dataCleaner;

    @AfterEach
    private void cleanData() {
        dataCleaner.cleanData();
    }
}
