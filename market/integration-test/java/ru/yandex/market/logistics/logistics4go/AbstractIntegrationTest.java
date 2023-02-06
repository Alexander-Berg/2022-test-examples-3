package ru.yandex.market.logistics.logistics4go;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import io.restassured.RestAssured;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.logistics4go.client.ApiClient;
import ru.yandex.market.logistics.logistics4go.config.IntegrationTestConfig;
import ru.yandex.market.logistics.logistics4go.utils.QueueTaskChecker;
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
    classes = IntegrationTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc
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
@TestPropertySource("classpath:integration-test.properties")
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
public class AbstractIntegrationTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @LocalServerPort
    private int localPort;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    protected ApiClient apiClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestableClock clock;

    @Autowired
    protected QueueTaskChecker queueTaskChecker;

    @Autowired
    private CacheManager cacheManager;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor("ru.yandex.market.logistics.logistics4go");

    @BeforeEach
    public void beforeEachTest() {
        RequestContextHolder.createContext(IntegrationTestConfig.TEST_REQUEST_ID);
        RestAssured.port = localPort;
    }

    @AfterEach
    public void afterEachTest() {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);

        clock.clearFixed();
    }
}
