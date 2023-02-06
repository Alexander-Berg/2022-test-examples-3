package ru.yandex.market.promoboss.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.promoboss.config.DbConfiguration;
import ru.yandex.market.promoboss.service.TimeService;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(
        value = {
                DependencyInjectionTestExecutionListener.class,
                DbUnitTestExecutionListener.class
        }
)
@ContextConfiguration(
        initializers = PGaaSZonkyInitializer.class
)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestPropertySource(locations = {
        "classpath:test_properties/pg_test.properties",
        "classpath:test_properties/yt_test.properties",
        "classpath:test_properties/application.properties"
})
@DbUnitDataSet(
        nonTruncatedTables = {
                "public.databasechangelog",
                "public.databasechangeloglock"
        }
)
@Import({
        DbConfiguration.class
})
@AutoConfigureMockMvc
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = "datatypeFactory",
                value = "ru.yandex.market.promoboss.pg.ExtendedPostgresqlDataTypeFactory"
        )
)
@MockBean(classes = TimeService.class)
public abstract class AbstractIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
