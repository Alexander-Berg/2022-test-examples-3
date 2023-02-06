package ru.yandex.market.logistics.yard.base;


import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ResourceUtils;

import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.logistics.yard.config.IntegrationTestConfig;
import ru.yandex.market.logistics.yard.config.logbroker.FfwfEventConsumerProperties;
import ru.yandex.market.logistics.yard.config.logbroker.LogbrokerClientEventConsumerProperties;
import ru.yandex.market.logistics.yard.config.logbroker.LogbrokerEventConsumerProperties;
import ru.yandex.market.logistics.yard.mbi.CustomMbiClient;
import ru.yandex.market.logistics.yard_v2.config.staff.StaffApiClient;
import ru.yandex.market.tpl.common.dsm.client.api.DriverApi;
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
        databaseConnection = {"dbUnitDatabaseConnection", "dbqueueDatabaseConnection"},
        dataSetLoader = NullableColumnsDataSetLoader.class
)
@TestPropertySource("classpath:application-test.properties")
public abstract class AbstractContextualTest extends BaseIntegrationTest {

    private ObjectMapper objectMapper;

    @Autowired
    protected MockMvc mockMvc;

    @MockBean(reset = MockReset.BEFORE)
    protected FulfillmentWorkflowClientApi ffWfApiClient;

    @MockBean
    protected StaffApiClient staffApiClient;

    @MockBean(name = "ffwfTvmTicketProvider")
    protected TvmTicketProvider tvmTicketProvider;

    @MockBean(name = "calendaringTvmTicketProvider")
    protected TvmTicketProvider calendaringTvmTicketProvider;

    @MockBean(name = "lbkxTvmTicketProvider")
    protected TvmTicketProvider lbkxTvmTicketProvider;

    @MockBean(name = "yaSmsTvmTicketProvider")
    protected TvmTicketProvider yaSmsTvmTicketProvider;

    @MockBean(name = "staffApiTvmTicketProvider")
    protected TvmTicketProvider staffApiTvmTicketProvider;

    @MockBean(name = "goZoraTvmTicketProvider")
    protected TvmTicketProvider goZoraTvmTicketProvider;

    @MockBean(name = "pechkinTvmTicketProvider")
    protected TvmTicketProvider pechkinTvmTicketProvider;

    @MockBean
    protected LogbrokerEventConsumerProperties logbrokerEventConsumerProperties;

    @MockBean
    protected LogbrokerClientEventConsumerProperties logbrokerClientEventConsumerProperties;

    @MockBean
    protected FfwfEventConsumerProperties ffwfEventConsumerProperties;

    @MockBean
    protected LogbrokerReader logbrokerReader;

    @MockBean
    protected LMSClient lmsClient;

    @MockBean
    protected CustomMbiClient mbiApiClient;

    @MockBean
    protected DriverApi driverApi;

    @BeforeEach
    public void beforeEach() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    protected <T> T readFromJson(String filename, Class<T> typedClass) throws IOException {
        File file = ResourceUtils.getFile(filename);
        return objectMapper.readValue(file, typedClass);
    }

}
