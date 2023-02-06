package ru.yandex.market.mcrm.http.internal;

import javax.inject.Inject;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.request.trace.Module;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = HttpClientFactoryTestConfiguration.class)
@TestPropertySource("classpath:/ru/yandex/market/mcrm/http/internal/external.properties")
@ActiveProfiles("test")
public class HttpClientFactoryTest {

    @Inject
    HttpClientFactoryImpl factory;
    @Inject
    TvmServiceMockImpl tvmService;

    @BeforeEach
    public void setUp() {
        tvmService.reset();
    }

    @Test
    public void testService1_connectTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                17, conf.getConnectTimeout(),
                "Должны получить значение свойства external.service1.connectTimeout из external.properties");
    }

    @Test
    public void testService1_maxConnections() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                19, conf.getMaxConnections(),
                "Должны получить значение свойства external.service1.maxConnections из external.properties");
    }

    @Test
    public void testService1_maxRedirects() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                2, conf.getMaxRedirects(), "Должны получить значение свойства external.service1.maxRedirects из " +
                        "external.properties");
    }

    @Test
    public void testService1_module() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                Module.MARKET_CONTENT_API, conf.getModule(),
                "Должны получить значение свойства external.service1.module из external.properties");
    }

    @Test
    public void testService1_readTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                11, conf.getReadTimeout(), "Должны получить значение свойства external.service1.readTimeout из external.properties");
    }

    @Test
    public void testService1_requestTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                13, conf.getRequestTimeout(), "Должны получить значение свойства external.service1.requestTimeout из external.properties");
    }

    @Test
    public void testService1_url() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                "http://example.com/service1", conf.getBaseUrl(), "Должны получить значение свойства external" +
                        ".service1.url из external.properties");
    }

    @Test
    public void testService2_default_connectTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                7, conf.getConnectTimeout(), "Должны получить значение свойства external.default.connectTimeout из external.properties");
    }

    @Test
    public void testService2_default_maxConnections() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                2, conf.getMaxConnections(), "Должны получить значение свойства external.default.maxConnections из external.properties");
    }

    @Test
    public void testService2_default_maxRedirects() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                1, conf.getMaxRedirects(), "Должны получить значение свойства external.default.maxRedirects из external.properties");
    }

    @Test
    public void testService2_default_readTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                3, conf.getReadTimeout(), "Должны получить значение свойства external.default.readTimeout из external.properties");
    }

    @Test
    public void testService2_default_requestTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                5, conf.getRequestTimeout(), "Должны получить значение свойства external.default.requestTimeout из external.properties");
    }

    @Test
    public void testService2_retryTask_no() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                0, conf.getRetryTaskConfiguration().getMaxAttemptCount(),
                "Не должны получить RetryHttpRequestFilter т.к. значение external.service2.retryTask" +
                        ".maxAttemptCount = 0 ");
    }

    private <T> T find(Iterable<?> values, Class<T> clazz) {
        return (T) Iterables.find(values, Predicates.instanceOf(clazz), null);
    }

    @Test
    public void testService2_tvm_no() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        TvmHttpRequestFilter filter = find(conf.getFilters(), TvmHttpRequestFilter.class);
        Assertions.assertNull(filter, "Не должны получить TvmHttpRequestFilter т.к. значение external.service2.tvm" +
                ".application не" +
                " установлено ");
    }

    @Test
    public void testService3_default_retryTask_delay() {
        HttpClientConfiguration conf = factory.getConfiguration("service3");

        Assertions.assertEquals(
                29, conf.getRetryTaskConfiguration().getDelay(), "Должны получить значение свойства external.default.retryTask.delay из external.properties");
    }

    @Test
    public void testService3_default_retryTask_handler() {
        HttpClientConfiguration conf = factory.getConfiguration("service3");

        Assertions.assertEquals(
                "retryHttpHandler", conf.getRetryTaskConfiguration().getHandler(),
                "Должны получить значение свойства external.default.retryTask.handler из external" +
                        ".properties");
    }

    @Test
    public void testService3_default_retryTask_initialDelay() {
        HttpClientConfiguration conf = factory.getConfiguration("service3");

        Assertions.assertEquals(
                23, conf.getRetryTaskConfiguration().getInitialDelay(), "Должны получить значение свойства external" +
                        ".default.retryTask.initialDelay из external" +
                        ".properties");
    }

    @Test
    public void testService3_retryTask_maxAttempt() {
        HttpClientConfiguration conf = factory.getConfiguration("service3");

        Assertions.assertEquals(
                31, conf.getRetryTaskConfiguration().getMaxAttemptCount(), "Должны получить значение свойства " +
                        "external.service3.retryTask.maxAttemptCount из " +
                        "external.properties");
    }

    @Test
    public void testService4_tvm_application() {
        HttpClientConfiguration conf = factory.getConfiguration("service4");

        TvmHttpRequestFilter filter = find(conf.getFilters(), TvmHttpRequestFilter.class);
        Assertions.assertNotNull(filter,
                "Должны получить TvmHttpRequestFilter т.к. значение external.service4.tvm.application " +
                        "установлено ");
        Assertions.assertEquals("37", filter.getApplicationId(), "Должны получить значение свойства external.service4.tvm.application из external" +
                ".properties");
    }

    @Test
    public void testService4_tvm_applicationRegistered() {
        factory.getConfiguration("service4");

        Assertions.assertTrue(tvmService.getApplications().contains("37"), "Должны зарегистрировать в TvmService приложение, указанное в external.service4.tvm" +
                ".application");
    }

}
