package ru.yandex.market.logistics.iris.configuration;

import java.math.BigDecimal;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.StocksService;
import ru.yandex.market.logistics.iris.service.export.MdsRepository;
import ru.yandex.market.logistics.iris.service.logbroker.producer.LogBrokerPushService;
import ru.yandex.market.logistics.iris.utils.HibernateQueriesExecutionListener;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static ru.yandex.market.logistics.iris.utils.RequestFactoryWrapper.wrapInBufferedRequestFactory;


@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    HibernateQueriesExecutionListener.class
})
@ActiveProfiles({
    ActivateEmbeddedPg.EMBEDDED_PG,
})
@DatabaseSetup
@DbUnitConfiguration
@TestPropertySource("classpath:application-integration-test.properties")
public abstract class AbstractContextualTest extends BaseIntegrationTest {


    @Autowired
    protected MockMvc mockMvc;

    /**
     * Явно указываем, что необходимо рестартануть до теста,
     * т.к. клиент вызывается в рамках PostConstruct'а WarehouseSyncService'а.
     */
    @MockBean(reset = MockReset.BEFORE)
    protected LMSClient lmsClient;

    @MockBean(reset = MockReset.BEFORE)
    protected FulfillmentClient fulfillmentClient;

    @MockBean(reset = MockReset.BEFORE)
    protected StocksService stocksService;

    @MockBean(reset = MockReset.BEFORE)
    protected Yt yt;

    @MockBean(reset = MockReset.BEFORE)
    protected LogBrokerPushService logBrokerPushService;

    @MockBean(reset = MockReset.BEFORE)
    protected MdsRepository mdsRepository;

    @MockBean(reset = MockReset.BEFORE)
    protected MbiApiClient mbiApiClient;

    protected final String httpOperationWithResult(MockHttpServletRequestBuilder httpOperation,
                                                   ResultMatcher... matchers) throws Exception {
        ResultActions resultActions = mockMvc.perform(httpOperation);
        for (ResultMatcher matcher : matchers) {
            resultActions.andExpect(matcher);
        }

        return resultActions
            .andReturn().getResponse()
            .getContentAsString();
    }

    public MockRestServiceServer createMockRestServiceServer(RestTemplate restTemplate) {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        wrapInBufferedRequestFactory(restTemplate);
        return mockServer;
    }

    protected BigDecimal toBigDecimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale);
    }
}
