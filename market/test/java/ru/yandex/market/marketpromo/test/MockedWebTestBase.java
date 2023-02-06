package ru.yandex.market.marketpromo.test;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.marketpromo.core.application.context.CategoryInterfacePromo;
import ru.yandex.market.marketpromo.core.test.config.ApplicationCoreTaskBasicSupportConfig;
import ru.yandex.market.marketpromo.core.test.context.initializer.YdbContainerContextInitializer;
import ru.yandex.market.marketpromo.core.utils.RequestContextUtils;
import ru.yandex.market.marketpromo.misc.ExtendedClock;
import ru.yandex.market.marketpromo.security.SystemUserUtils;
import ru.yandex.market.marketpromo.test.config.ApplicationTestConfig;
import ru.yandex.market.ydb.integration.DataCleaner;

@SpringBootTest(
        classes = {
                ApplicationTestConfig.class,
                ApplicationCoreTaskBasicSupportConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ContextConfiguration(
        initializers = YdbContainerContextInitializer.class
)
@ExtendWith(SpringExtension.class)
public abstract class MockedWebTestBase {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ExtendedClock clock;

    @Autowired
    @CategoryInterfacePromo
    protected ObjectMapper objectMapper;

    @Autowired
    private DataCleaner dataCleaner;

    public MockedWebTestBase() {
        SystemUserUtils.loginAsSystem();
        RequestContextUtils.setupContext(Map.of());
    }

    @AfterEach
    protected void cleanData() {
        dataCleaner.cleanData();
    }
}
