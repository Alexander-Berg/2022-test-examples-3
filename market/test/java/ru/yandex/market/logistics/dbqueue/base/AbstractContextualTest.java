package ru.yandex.market.logistics.dbqueue.base;


import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.logistics.dbqueue.config.IntegrationTestConfig;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;

@WebAppConfiguration
@AutoConfigureMockMvc(addFilters = false)
@SpringBootTest(
        classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        ResetDatabaseTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class
})
@ActiveProfiles({
        ActivateEmbeddedPg.EMBEDDED_PG,
})
@CleanDatabase
@DbUnitConfiguration(
        dataSetLoader = NullableColumnsDataSetLoader.class
)
@TestPropertySource("classpath:application-test.properties")
public abstract class AbstractContextualTest extends BaseIntegrationTest {

}
