package ru.yandex.market.wms.servicebus;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
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

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.auth.AuthenticationInterceptor;

@SpringBootTest(
        classes = {IntegrationTestConfig.class})
@ActiveProfiles(Profiles.TEST)
@AutoConfigureMockMvc
@Transactional
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@DbUnitConfiguration(
        dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"clickHouseConnection", "serviceBusConnection"})
public class IntegrationTest extends BaseTest {

    public static final int MOCK_WEB_SERVER_PORT = 8993;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected DbConfigService dbConfigService;

    @MockBean
    protected StartrekClient startrekClient;

    @MockBean
    protected AuthenticationInterceptor interceptor;
}
