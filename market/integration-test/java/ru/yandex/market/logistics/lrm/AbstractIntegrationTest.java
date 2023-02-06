package ru.yandex.market.logistics.lrm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import io.restassured.RestAssured;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.lrm.client.ApiClient;
import ru.yandex.market.logistics.lrm.config.LrmTestConfiguration;
import ru.yandex.market.logistics.lrm.config.initializer.YdbInitializer;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.test.integration.jpa.HibernateQueriesExecutionListener;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(
    classes = LrmTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integration-test")
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    HibernateQueriesExecutionListener.class,
})
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@ContextConfiguration(initializers = YdbInitializer.class)
public class AbstractIntegrationTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";
    protected static final String SOURCE_FOR_LES = "lrm";
    protected static final String OUT_LES_QUEUE = "lrm_out";

    @LocalServerPort
    private int localPort;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    protected ApiClient apiClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestableClock clock;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor("ru.yandex.market.logistics.lrm");

    @BeforeEach
    public void beforeEachTest() {
        RequestContextHolder.createContext(LrmTestConfiguration.TEST_REQUEST_ID);
        RestAssured.port = localPort;
    }
}
