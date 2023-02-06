package ru.yandex.market.logistics.cs;

import java.time.LocalDateTime;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistics.cs.config.CacheConfiguration;
import ru.yandex.market.logistics.cs.config.IntegrationTestConfig;
import ru.yandex.market.logistics.cs.util.DateTimeTestUtils;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.test.integration.jpa.HibernateQueriesExecutionListener;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@AutoConfigureMockMvc
@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(
    classes = IntegrationTestConfig.class,
    webEnvironment = WebEnvironment.MOCK
)
@TestPropertySource("classpath:application-integration-test.properties")
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
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitDatabaseConnection"
)
public abstract class AbstractIntegrationTest {
    public static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @MockBean
    protected LomClient lomClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    public void after() {
        cacheManager.getCache(CacheConfiguration.GET_PARTNER_CARGO_TYPE_FACTOR_CACHE).clear();
    }

    protected <T> void assertEntitiesEqual(T expected, T actual) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .withComparatorForType(DateTimeTestUtils.microsecondsComparator(), LocalDateTime.class)
            .isEqualTo(expected);
    }
}
