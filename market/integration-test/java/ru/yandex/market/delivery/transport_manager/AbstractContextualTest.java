package ru.yandex.market.delivery.transport_manager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.config.IntegrationTestConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.util.Profiles;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = IntegrationTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    ApplicationEventsTestExecutionListener.class,
})
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@ActiveProfiles(Profiles.INTEGRATION_TEST)
@ComponentScan({
    "ru.yandex.market.delivery.transport_manager.controller",
    "ru.yandex.market.delivery.transport_manager.service",
    "ru.yandex.market.delivery.transport_manager.queue",
    "ru.yandex.market.delivery.transport_manager.facade",
    "ru.yandex.market.delivery.transport_manager.task",
    "ru.yandex.market.delivery.transport_manager.event",
    "ru.yandex.market.delivery.transport_manager.interactor",
    "ru.yandex.market.delivery.transport_manager.util",
    "ru.yandex.market.delivery.gruzin.controller",
    "ru.yandex.market.delivery.gruzin.facade",
    "ru.yandex.market.delivery.gruzin.converter",
    "ru.yandex.market.delivery.gruzin.service",
    "ru.yandex.market.delivery.gruzin.task",
})
public class AbstractContextualTest {

    public static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected TmPropertyService propertyService;
    @Autowired
    protected TestableClock clock;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected <T> void assertThatModelEquals(T expected, T actual, String... fieldsToIgnore) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(fieldsToIgnore)
            .ignoringFieldsMatchingRegexes(".*id", ".*created", ".*updated")
            .isEqualTo(expected);
    }

    protected <T> void assertContainsExactlyInAnyOrder(
        List<T> actual,
        T... expected
    ) {
        softly.assertThat(actual)
            .usingElementComparatorIgnoringFields("id", "created", "updated")
            .containsExactlyInAnyOrder(expected);
    }

    protected static Instant toInstant(String dateTime) {
        return LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant();
    }

    protected void mockProperty(TmPropertyKey key, Object value) {
        Mockito
            .when(propertyService.get(key))
            .thenReturn(value);
        if (value instanceof Boolean) {
            Mockito
                .when(propertyService.getBoolean(key))
                .thenReturn((Boolean) value);
        }
        if (value instanceof Integer) {
            Mockito
                .when(propertyService.getInt(key))
                .thenReturn((Integer) value);
        }
        if (value instanceof Long) {
            Mockito
                .when(propertyService.getLong(key))
                .thenReturn((Long) value);
        }
        if (value instanceof Map) {
            Mockito
                .when(propertyService.getMap(key))
                .thenReturn((Map<String, Object>) value);
        }
    }

    protected void restartTransportationSequences(int valueToStartWith) {
        updateSequence("transportation_unit", valueToStartWith);
        updateSequence("transportation", valueToStartWith);
        updateSequence("movement", valueToStartWith);
    }

    protected void updateSequence(String tableName, int valueToStartWith) {
        jdbcTemplate.execute(String.format("ALTER SEQUENCE %s_id_seq RESTART WITH %d", tableName, valueToStartWith));
    }

    public static ResultHandler setResponseCharesetEncoding(String encoding) {
        return result -> result.getResponse().setCharacterEncoding(encoding);
    }
}
