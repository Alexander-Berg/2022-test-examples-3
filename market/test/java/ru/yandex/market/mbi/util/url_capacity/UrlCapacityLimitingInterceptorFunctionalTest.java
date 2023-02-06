package ru.yandex.market.mbi.util.url_capacity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.partner.mvc.controller.url_capacity.UrlCapacityTestController;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.partner.util.url_capacity.UrlCapacityLimiterConfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональный тест интерцептора, смотрящий методы на тестовом контроллере
 *
 * @see UrlCapacityTestController
 */
public class UrlCapacityLimitingInterceptorFunctionalTest extends UrlCapacityLimitingFunctionalTest {

    @Autowired
    private EnvUrlCapacityLimitFlags urlCapacityLimitFlag;

    @Autowired
    private UrlCapacityLimiter urlCapacityLimiter;

    @BeforeEach
    public void init() {
        urlCapacityLimitFlag.reset();
        urlCapacityLimiter.clear();
    }

    @Test
    public void testGet() {
        setFlagEnabled(true);
        setLogsOnly(false);

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");

        verify(urlCapacityLimiter, times(1))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method@GET"));
        verify(urlCapacityLimiter, times(1))
                .requestProcessed(eq("url-capacity-test/test-method@GET"));
    }

    @Test
    public void testGetWithFlagDisabled() {
        setFlagEnabled(false);
        setLogsOnly(false);

        boolean result = environmentService.getBooleanValue(UrlCapacityLimiterConfig.ENABLED_VAR, false);

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");

        verify(urlCapacityLimiter, never())
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method@GET"));
        verify(urlCapacityLimiter, never())
                .requestProcessed(eq("url-capacity-test/test-method@GET"));
    }

    @Test
    public void testGetTwoMethods() {
        setFlagEnabled(true);
        setLogsOnly(false);

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");
        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");
        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method-2");
        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method-2");
        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method-2");

        verify(urlCapacityLimiter, times(2))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method@GET"));
        verify(urlCapacityLimiter, times(2))
                .requestProcessed(eq("url-capacity-test/test-method@GET"));
        verify(urlCapacityLimiter, times(3))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method-2@GET"));
        verify(urlCapacityLimiter, times(3))
                .requestProcessed(eq("url-capacity-test/test-method-2@GET"));

    }

    @Test
    public void testMethodWithException() {
        setFlagEnabled(true);
        setLogsOnly(false);

        assertThatThrownBy(() ->
                FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method-with-exception")
        ).isInstanceOf(Throwable.class);

        verify(urlCapacityLimiter, times(1))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method-with-exception@GET"));
        verify(urlCapacityLimiter, times(1))
                .requestProcessed(eq("url-capacity-test/test-method-with-exception@GET"));
    }

    @Test
    public void testMethodWithId() {
        setFlagEnabled(true);
        setLogsOnly(false);

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method-with-id/12345");
        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method-with-id/67890");

        //Разные параметры должны восприниматься одинаково
        verify(urlCapacityLimiter, times(2))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method-with-id/{partnerId}@GET"));
        verify(urlCapacityLimiter, times(2))
                .requestProcessed(eq("url-capacity-test/test-method-with-id/{partnerId}@GET"));
    }

    @Test
    public void testAsyncMethodIgnored() {
        setFlagEnabled(true);
        setLogsOnly(false);

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-async-method");

        //Асинхронные методы пропускаем
        verify(urlCapacityLimiter, never())
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-async-method/@GET"));
        verify(urlCapacityLimiter, never())
                .requestProcessed(eq("url-capacity-test/test-async-method@GET"));
    }

    @Test
    public void test429OnTooManyRequests() {
        setFlagEnabled(true);
        setLogsOnly(false);

        //Предварительно заполняем наш пул запросами
        int maxThreads = urlCapacityLimiter.getMaxCapacity();
        for (int i = 0; i < maxThreads; i++) {
            urlCapacityLimiter.tryProcessOneMoreRequest("url-capacity-test/test-method@GET");
        }

        //Пытаемся сделать еще один запрос
        assertThatThrownBy(() ->
                FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method")
        ).isInstanceOf(HttpClientErrorException.TooManyRequests.class);

        verify(urlCapacityLimiter, times(maxThreads + 1))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method@GET"));

        //Если счетчик не увеличивался, то и уменьшаться ему незачем
        verify(urlCapacityLimiter, never())
                .requestProcessed(eq("url-capacity-test/test-method@GET"));
    }

    @Test
    public void testNo429OnTooManyRequestsAndLogsOnly() {
        setFlagEnabled(true);
        setLogsOnly(true);

        //Предварительно заполняем наш пул запросами
        int maxThreads = urlCapacityLimiter.getMaxCapacity();
        for (int i = 0; i < maxThreads; i++) {
            urlCapacityLimiter.tryProcessOneMoreRequest("url-capacity-test/test-method@GET");
        }

        //Пытаемся сделать еще один запрос, исключения не вылетает
        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");

        verify(urlCapacityLimiter, times(maxThreads + 1))
                .tryProcessOneMoreRequest(eq("url-capacity-test/test-method@GET"));

        //Если счетчик не увеличивался, то и уменьшаться ему незачем
        verify(urlCapacityLimiter, never())
                .requestProcessed(eq("url-capacity-test/test-method@GET"));
    }

}
