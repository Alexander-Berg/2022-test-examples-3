package ru.yandex.market.wms.scheduler.config;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.BaseIntegrationTest;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.scheduler.order.status.calculate.dao.SkuWithQtyDao;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@SpringBootTest(classes = SchedulerIntegrationTestConfig.class)
@ActiveProfiles(Profiles.TEST)
@Transactional
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(
        dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"wmwhseConnection", "schedulerConnection", "archiveConnection", "clickHouseConnection",
                "scprdi1Connection", "scprdd1Connection"}
)
public class SchedulerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    @Autowired
    protected SkuWithQtyDao skuWithQtyDao;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(skuWithQtyDao);
    }
}
