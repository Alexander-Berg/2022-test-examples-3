package ru.yandex.market.logistics.lom;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.configuration.IntegrationTestConfiguration;
import ru.yandex.market.logistics.lom.configuration.properties.OrderCancellationProperties;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.test.integration.jpa.HibernateQueriesExecutionListener;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.config.name=integration-test"
)
@AutoConfigureMockMvc(secure = false)
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
@ComponentScan({
    "ru.yandex.market.logistics.lom.admin",
    "ru.yandex.market.logistics.lom.checker",
    "ru.yandex.market.logistics.lom.controller",
    "ru.yandex.market.logistics.lom.converter",
    "ru.yandex.market.logistics.lom.facade",
    "ru.yandex.market.logistics.lom.jobs",
    "ru.yandex.market.logistics.lom.repository",
    "ru.yandex.market.logistics.lom.service",
    "ru.yandex.market.logistics.lom.specification",
    "ru.yandex.market.logistics.lom.validators",
    "ru.yandex.market.logistics.lom.configuration.properties",
    "ru.yandex.market.logistics.lom.lms",
})
@TestPropertySource("classpath:integration-test.properties")
@Slf4j
@ComponentScan("ru.yandex.market.logistics.lom.utils.ydb.converter")
public class AbstractContextualCommonTest {

    public static final long ORDER_ID = 1L;
    public static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    protected MockRestServiceServer mockServer;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestableClock clock;

    @Autowired
    protected OrderCancellationProperties orderCancellationProperties;

    @Autowired
    protected QueueTaskChecker queueTaskChecker;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor("ru.yandex.market.logistics.lom");

    @BeforeEach
    public void resourceLocationFactoryMock(TestInfo testInfo) {
        log.info(
            "Started {}.{}.{}",
            testInfo.getTestClass().map(Class::getSimpleName).orElse("<no class>"),
            testInfo.getTestMethod().map(Method::getName).orElse("<no method>"),
            testInfo.getDisplayName()
        );

        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation -> {
            final String fileName = invocation.getArgument(0);
            return ResourceLocation.create("lom-doc-test", fileName);
        });
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

        // init order cancellation properties
        orderCancellationProperties.setCreateCancellationOrderRequestByCheckpoints(true);
        orderCancellationProperties.setSkipWaitingCheckpointsFallbackOnApiCancellationFail(false);
        orderCancellationProperties.setConfirmCancellationByApiWithoutCheckpoints(false);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
        RequestContextHolder.clearContext();
        clock.clearFixed();

        log.info(
            "Finished {}.{}.{}",
            testInfo.getTestClass().map(Class::getSimpleName).orElse("<no class>"),
            testInfo.getTestMethod().map(Method::getName).orElse("<no method>"),
            testInfo.getDisplayName()
        );
    }

    @SneakyThrows
    protected void assertOrderHistoryNeverChanged(long orderId) {
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, orderId, 0);
    }

    @Nonnull
    protected MockHttpServletRequestBuilder request(HttpMethod method, String url, Object body) throws Exception {
        return MockMvcRequestBuilders.request(method, url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
    }
}
