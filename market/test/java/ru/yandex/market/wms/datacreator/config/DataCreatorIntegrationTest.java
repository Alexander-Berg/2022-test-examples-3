package ru.yandex.market.wms.datacreator.config;

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

import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@SpringBootTest(classes = DataCreatorIntegrationTestConfig.class)
@ActiveProfiles(Profiles.TEST)
@Transactional
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc(addFilters = false)
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"wmwhse1Connection"})
public class DataCreatorIntegrationTest extends BaseTest {

    @Autowired
    protected MockMvc mockMvc;
}
