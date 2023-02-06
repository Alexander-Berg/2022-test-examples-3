package ru.yandex.market.wms.test;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.wms.radiator.core.redis.TestRedisConfiguration;

@SpringBootTest(classes = IntegrationTestBackendConfiguration.class)
@ActiveProfiles("test")
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class
})
@Import({TestRedisConfiguration.class})
@ComponentScan({
        "ru.yandex.market.wms.radiator.core.config.client",
        "ru.yandex.market.wms.radiator.core.config.properties",
        "ru.yandex.market.wms.radiator.core.monitoring",
        "ru.yandex.market.wms.radiator.service.stocks",
        "ru.yandex.market.wms.radiator.repository",
        "ru.yandex.market.wms.radiator.service.config",
        "ru.yandex.market.wms.radiator.service.stocks.push",
        "ru.yandex.market.wms.radiator.service.stocks.redis"
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
    databaseConnection = {"wh1Connection", "wh2Connection"})
@TestPropertySource({"classpath:application-test.properties"})
public abstract class IntegrationTestBackend {

    @Autowired
    protected MockMvc mockMvc;
}
