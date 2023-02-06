package ru.yandex.market.logistics.logistics4shops;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import io.restassured.RestAssured;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.logistics4shops.client.ApiClient;
import ru.yandex.market.logistics.logistics4shops.config.IntegrationTestConfiguration;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.factory.LomFactory;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;
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
    classes = IntegrationTestConfiguration.class,
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
@TestPropertySource("classpath:integration-test.properties")
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@ParametersAreNonnullByDefault
public class AbstractIntegrationTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";
    protected static final String REQUEST_ID_PATTERN = "\\d{13}/[0-9a-z]{32}";

    @LocalServerPort
    private int localPort;

    @Autowired
    protected WebApplicationContext context;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    protected ApiClient apiClient;

    @Autowired
    protected TestableClock clock;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    protected ObjectMapper objectMapper;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor("ru.yandex.market.logistics.logistics4shops");

    @Autowired
    protected CheckouterFactory checkouterFactory;

    @Autowired
    protected LomFactory lomFactory;

    @BeforeEach
    public final void beforeEachTest() {
        RequestContextHolder.createContext(IntegrationTestConfiguration.TEST_REQUEST_ID);
        RestAssured.port = localPort;
    }

    /**
     * Меняет значение фичепроперти для текущего теста.
     * Использование этого метода исключает сайд-эффекты между тестами, связанные с фичепропертями.
     * Изначальные значения пропертей см. в {@code integration-test.properties}.
     *
     * @param property геттер для фичепроперти
     * @param value    значение, которое должно возвращаться
     */
    protected final <T> void setupFeature(Function<FeatureProperties, T> property, @Nullable T value) {
        Mockito.when(property.apply(featureProperties)).thenReturn(value);
    }

    @Nonnull
    protected Stream<TskvLogRecord<?>> getLogsAsRecords() {
        return backLogCaptor.getResults()
            .stream()
            .map(TskvLogRecord::parseFromStringTskv);
    }

    @Nonnull
    protected AbstractListAssert<?, List<? extends TskvLogRecord<?>>, TskvLogRecord<?>, ObjectAssert<TskvLogRecord<?>>>
    assertLogs() {
        return softly.assertThat(getLogsAsRecords());
    }
}
