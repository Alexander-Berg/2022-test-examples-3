package ru.yandex.market.logistics.management;

import java.util.Objects;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistics.management.configuration.Profiles;
import ru.yandex.market.logistics.management.configuration.TestContextConfiguration;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.producer.BuildWarehouseSegmentsProducer;
import ru.yandex.market.logistics.management.queue.producer.PartnerBillingRegistrationTaskProducer;
import ru.yandex.market.logistics.management.service.client.MarketIdService;
import ru.yandex.market.logistics.management.util.CleanDbTestExecutionListener;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.test.integration.jpa.HibernateQueriesExecutionListener;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

/**
 * @deprecated следует использовать {@link AbstractContextualAspectValidationTest} для новых тестовых классов.
 */
@Deprecated
@SpringBootTest(
    classes = {
        ApplicationMain.class,
        TestContextConfiguration.class,
    }
)
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        CleanDbTestExecutionListener.class,
        ServletTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class,
        ResetDatabaseTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        HibernateQueriesExecutionListener.class,
    }
)
@ActiveProfiles(Profiles.INTEGRATION_TEST)
@ContextConfiguration
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitQualifiedDatabaseConnection"}
)
public abstract class AbstractContextualTest {
    public static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";
    public static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected DataSource dataSource;

    @MockBean
    protected MarketIdService marketIdService;

    @MockBean
    protected DeliveryClient deliveryClient;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    protected ObjectMapper objectMapper;

    @SpyBean
    @Qualifier("partnerBillingClientCreationTaskProducer")
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> partnerBillingClientCreationTaskProducer;

    @SpyBean
    @Qualifier("partnerBillingClientLinkingTaskProducer")
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> partnerBillingClientLinkingTaskProducer;

    @Autowired
    protected BuildWarehouseSegmentsProducer buildWarehouseSegmentsProducer;

    @Captor
    protected ArgumentCaptor<Long> warehouseIdsArgumentCaptor;

    @BeforeEach
    protected void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").characterEncoding("utf-8"))
            .alwaysDo(log())
            .build();
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
        doNothing().when(buildWarehouseSegmentsProducer).produceTask(anyLong());
    }

    @AfterEach
    protected void tearDown() {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
        RequestContextHolder.clearContext();
    }

    protected void checkBuildWarehouseSegmentTask(Long... warehouseIds) {
        verify(buildWarehouseSegmentsProducer, atLeast(warehouseIds.length))
            .produceTask(warehouseIdsArgumentCaptor.capture());
        softly.assertThat(warehouseIdsArgumentCaptor.getAllValues()).containsOnly(warehouseIds);
    }
}
