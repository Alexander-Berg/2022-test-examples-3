package ru.yandex.market.wms.receiving;


import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.BaseIntegrationTest;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.receiving.config.ReceivingIntegrationTestConfig;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@SpringBootTest(classes = {BaseTestConfig.class, ReceivingIntegrationTestConfig.class})
@ActiveProfiles(Profiles.TEST)
@Transactional
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {
                "wmwhseConnection",
                "archiveWmwhseConnection",
                "enterpriseConnection",
                "scprdd1DboConnection"
})
public class ReceivingIntegrationTest extends BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
}
