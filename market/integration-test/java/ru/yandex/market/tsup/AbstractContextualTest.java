package ru.yandex.market.tsup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.tsup.config.IntegrationTestConfig;
import ru.yandex.market.tsup.config.LocalPropertiesConfig;
import ru.yandex.market.tsup.core.cache.sly_cacher.CacheProvider;
import ru.yandex.market.tsup.domain.entity.tsup_properties.TsupProperty;
import ru.yandex.market.tsup.domain.entity.tsup_properties.TsupPropertyKey;
import ru.yandex.market.tsup.repository.mappers.TsupPropertyMapper;
import ru.yandex.market.tsup.util.Profiles;

@AutoConfigureMockMvc(secure = false)
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = IntegrationTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"spring.main.allow-bean-definition-overriding=true"}
)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
})
@ComponentScan({
    "ru.yandex.market.tsup.dbqueue",
    "ru.yandex.market.tsup.service",
    "ru.yandex.market.tsup.facade",
    "ru.yandex.market.tsup.controller",
    "ru.yandex.market.tsup.core",
    "ru.yandex.market.tsup.util.converter.mappers",
})
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({Profiles.INTEGRATION_TEST, Profiles.EAGER_BEAN_INIT})
@TestPropertySource({
    "classpath:application.properties",
})
@Import(
    LocalPropertiesConfig.class
)
public class AbstractContextualTest {

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected TestableClock clock;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected CacheProvider cacheProvider;
    @Autowired
    protected RedisConnectionFactory redisConnectionFactory;
    @Autowired
    protected TsupPropertyMapper tsupPropertyMapper;
    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void invalidateCache() {
        redisConnectionFactory.getConnection().flushAll();
    }

    protected <T> void assertThatModelEquals(T expected, T actual, String... fieldsToIgnore) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(fieldsToIgnore)
            .ignoringFieldsMatchingRegexes(".*id", ".*created", ".*updated")
            .isEqualTo(expected);
    }

    @SneakyThrows
    protected String toJson(Object any) {
        return objectMapper.writeValueAsString(any);
    }

    @SneakyThrows
    protected <T> T toObject(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    protected void setProperty(TsupPropertyKey key, boolean value) {
        setProperty(key, String.valueOf(value));
    }

    protected void setProperty(TsupPropertyKey key, String value) {
        TsupProperty property = tsupPropertyMapper.findByKey(key);
        if (property == null) {
            tsupPropertyMapper.insert(new TsupProperty(null, key, value));
        } else {
            tsupPropertyMapper.update(property.getId(), value);
        }
    }
}
