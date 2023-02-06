package ru.yandex.market.olap2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import lombok.SneakyThrows;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.FileCopyUtils;

import ru.yandex.market.olap2.config.ChUnionsConfig;
import ru.yandex.market.olap2.config.EmbeddedPostgresConfiguration;
import ru.yandex.market.olap2.config.ExecutorPoolConfig;
import ru.yandex.market.olap2.config.SlaMonitoringConfig;
import ru.yandex.market.olap2.config.TestBeansConfiguration;
import ru.yandex.market.olap2.config.TestPostgresConfiguration;
import ru.yandex.market.olap2.config.YtClusterPerCubeConfig;
import ru.yandex.market.olap2.model.YtCluster;

import static com.google.common.collect.Sets.newHashSet;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpVersion.HTTP_1_0;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@RunWith(SpringRunner.class)
@Import({
        EmbeddedPostgresConfiguration.class,
        LiquibaseAutoConfiguration.LiquibaseConfiguration.class,

        TestBeansConfiguration.class,

        TestPostgresConfiguration.class,

        SlaMonitoringConfig.class,
        YtClusterPerCubeConfig.class,
        ExecutorPoolConfig.class,
        ChUnionsConfig.class
})
@ComponentScan(value =
        {
                "ru.yandex.market.olap2.dao",
                "ru.yandex.market.olap2.load",
                "ru.yandex.market.olap2.model",
                "ru.yandex.market.olap2.step",
                "ru.yandex.market.olap2.sla",
                "ru.yandex.market.olap2.controller",
                "ru.yandex.market.olap2.util"
        })
@TestExecutionListeners({
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@TestPropertySource("classpath:application-integration-test.properties")
@DbUnitConfiguration(databaseConnection = "dbUnitDatabaseConnection")
public abstract class BaseIntegrationTest {

    protected final static YtCluster HAHN = new YtCluster("hahn");
    protected final static Set<YtCluster> LIVE_CLUSTERS = newHashSet(HAHN);

    @SneakyThrows
    protected static String extractContentAsString(HttpPost post) {
        InputStream inputStream = post.getEntity().getContent();
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining());
    }

    /**
     * Ожидание завершения фьючи из каптора или исключение о превышении времени ожидания.
     */
    protected static void waitForFuture(long timeoutMillis, ResultCaptor<Future<?>> result) {
        long startMillis = System.currentTimeMillis();
        while (result.getResult() == null || !result.getResult().isDone()) {
            if (System.currentTimeMillis() - startMillis > timeoutMillis) {
                throw new RuntimeException("Waiting timeout reached. " +
                        (result.getResult() == null ? "Invocation did not happen" : "Invocation happened"));
            }
        }
    }

    protected static MockResponse buildResponse(int statusCode) {
        return new MockResponse(
                new BasicStatusLine(
                        HTTP_1_0,
                        statusCode,
                        "reason"
                )
        );
    }

    protected static MockResponse buildResponse(int statusCode, String content) {
        MockResponse response = buildResponse(statusCode);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(content.getBytes(UTF_8)));
        response.setEntity(entity);
        return response;
    }

    @SneakyThrows
    protected static String loadResourceAsString(String which) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(which);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    @SneakyThrows
    protected static void assertJson(String expected, String actual, JSONCompareMode jsonCompareMode) {
        assertEquals(expected, actual, jsonCompareMode);
    }

    /**
     * Не очень удобно оказалось работать с {@code CloseableHttpResponse},
     * кажется, это единственный способ создать нужный объект.
     */
    protected static class MockResponse extends BasicHttpResponse implements CloseableHttpResponse {

        public MockResponse(StatusLine statusline) {
            super(statusline);
        }

        @Override
        public void close() {

        }
    }

    public class ResultCaptor<T> implements Answer {
        private T result = null;

        public T getResult() {
            return result;
        }

        @Override
        public T answer(InvocationOnMock invocationOnMock) throws Throwable {
            result = (T) invocationOnMock.callRealMethod();
            return result;
        }
    }
}
