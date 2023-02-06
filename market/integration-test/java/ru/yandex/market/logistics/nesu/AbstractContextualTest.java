package ru.yandex.market.logistics.nesu;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.nesu.configuration.IntegrationTestConfiguration;
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
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
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
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@ParametersAreNonnullByDefault
public class AbstractContextualTest {
    public static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    protected static final String EMPTY_ARRAY = "[]";

    protected static final Pageable PAGE_DEFAULTS = new Pageable(0, 10, null);
    public static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestableClock clock;

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager cacheManager;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor("ru.yandex.market.logistics.nesu");

    @BeforeEach
    protected final void setupBase() {
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation -> {
            final String fileName = invocation.getArgument(0);
            return ResourceLocation.create("nesu", fileName);
        });
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
    }

    @AfterEach
    protected final void tearDownBase() {
        RequestContextHolder.clearContext();
        clock.clearFixed();
        cleanCache();
    }

    protected <T> void assertThatModelEquals(T expected, T actual, String... fieldsToIgnore) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(fieldsToIgnore)
            .ignoringFieldsMatchingRegexes(".*id", ".*created", ".*updated")
            .isEqualTo(expected);
    }

    @Nonnull
    protected MockHttpServletRequestBuilder request(HttpMethod method, String url, Object body) throws Exception {
        return MockMvcRequestBuilders.request(method, url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
    }

    protected void cleanCache() {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
    }
}

