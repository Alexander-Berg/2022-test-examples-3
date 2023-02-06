package ru.yandex.market.mcrm.http.internal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.util.Futures;
import ru.yandex.market.mcrm.http.Http;
import ru.yandex.market.mcrm.http.HttpRequestChain;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.HttpStatus;

public class InitialRequestFilterTest {

    HttpClientConfiguration configuration;
    Http request;
    HttpRequestChain chain;
    ScheduledExecutorService executor;

    /**
     * Тестируем сценарий, когда в конфигурации указано делать перезапросы и первый запрос заканчивается неудачей,
     * а второй успешный.
     */
    @Test
    public void failureAndSuccessResponse() throws Exception {
        // настройка системы
        configuration.setRetryMaxAttemptCount(1);

        HttpResponse failureResponse = httpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        HttpResponse successResponse = httpResponse(HttpStatus.OK);

        HttpRequestChain failureChain = requestChain(failureResponse);
        HttpRequestChain successChain = requestChain(successResponse);

        Mockito.when(chain.clone())
                .thenReturn(failureChain)
                .thenReturn(successChain);

        // вызов системы
        InitialRequestFilter filter = new InitialRequestFilter(configuration, executor);
        CompletableFuture<HttpResponse> result = filter.doFilter(null, request, chain);

        // проверка утверждений
        Assertions.assertEquals(successResponse,
                result.get(), "Должны получить успешный результат т.к. включены перезапросы");
    }

    private HttpResponse httpResponse(HttpStatus status) {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getHttpStatus()).thenReturn(status);
        return response;
    }

    private HttpRequestChain requestChain(HttpResponse response) {
        HttpRequestChain chain = Mockito.mock(HttpRequestChain.class);
        Mockito.when(chain.doFilter(Mockito.any(), Mockito.any()))
                .thenReturn(Futures.newSucceededFuture(response));
        return chain;
    }

    @BeforeEach
    public void setUp() {
        executor = Executors.newScheduledThreadPool(1);

        configuration = new HttpClientConfiguration();
        configuration.setBaseUrl("http://example.com/path");
        configuration.setRetryDelay(1);
        configuration.setRetryResponseCodes(Arrays.asList(Range.closed(429, 429), Range.atLeast(500)));

        request = Http.get();

        chain = Mockito.mock(HttpRequestChain.class);
    }

    /**
     * Простой сценарий: делаем один запрос в результате которого возникает {@link Exception исключение}.
     * Перезапросы отключены.
     */
    @Test
    public void singleExceptional() {
        // настройка системы
        configuration.setRetryMaxAttemptCount(0);

        Exception exception = new RuntimeException();

        HttpRequestChain failureChain = Mockito.mock(HttpRequestChain.class);
        Mockito.when(failureChain.doFilter(Mockito.any(), Mockito.any()))
                .thenThrow(exception);

        Mockito.when(chain.clone()).thenReturn(failureChain);

        // вызов системы
        InitialRequestFilter filter = new InitialRequestFilter(configuration, executor);

        Throwable actualException = null;
        try {
            filter.doFilter(null, request, chain).get();
        } catch (Throwable t) {
            actualException = t;
        }

        // проверка утверждений
        Assertions.assertNotNull(actualException, "Должны получить исключение т.к. chain выбросил исключение");
        List<Throwable> suppressed = Arrays.asList(actualException.getCause().getSuppressed());
        Assertions.assertTrue(
                suppressed.contains(exception), "В списке suppressed должны получить исключение, выброшеннои chain");
    }

    /**
     * Простой сценарий: делаем один запрос в результате которого возpащается {@link CompletableFuture} с ошибкой.
     * Перезапросы отключены.
     */
    @Test
    public void singleExceptionalFuture() {
        // настройка системы
        configuration.setRetryMaxAttemptCount(0);

        Exception exception = new RuntimeException();

        HttpRequestChain failureChain = Mockito.mock(HttpRequestChain.class);
        Mockito.when(failureChain.doFilter(Mockito.any(), Mockito.any()))
                .thenReturn(Futures.newFailedFuture(exception));

        Mockito.when(chain.clone()).thenReturn(failureChain);

        // вызов системы
        InitialRequestFilter filter = new InitialRequestFilter(configuration, executor);

        Throwable actualException = null;
        try {
            filter.doFilter(null, request, chain).get();
        } catch (Throwable t) {
            actualException = t;
        }

        // проверка утверждений
        Assertions.assertNotNull(actualException, "Должны получить исключение т.к. chain вернул ошибку");
        List<Throwable> suppressed = Arrays.asList(actualException.getCause().getSuppressed());
        Assertions.assertTrue(
                suppressed.contains(exception), "В списке suppressed должны получить исключение, выброшеннои chain");
    }

    /**
     * Простой сценарий: делаем один запрос и получаем результат с ошибкой. Перезапросы отключены.
     */
    @Test
    public void singleFailureResponse() throws Exception {
        // настройка системы
        configuration.setRetryMaxAttemptCount(0);

        HttpResponse failureResponse = httpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        HttpRequestChain failureChain = requestChain(failureResponse);

        Mockito.when(chain.clone()).thenReturn(failureChain);

        // вызов системы
        InitialRequestFilter filter = new InitialRequestFilter(configuration, executor);
        CompletableFuture<HttpResponse> result = filter.doFilter(null, request, chain);

        // проверка утверждений
        Assertions.assertEquals(
                failureResponse, result.get(), "Должны получить результат без преобразовании т.к. в конфигурации " +
                        "указано не делать " +
                        "повторов");
    }

    /**
     * Простой сценарий: первый же вызов возвращает успешно выполненный запрос со статусом 200
     */
    @Test
    public void singleSuccessfulResponse() throws Exception {
        // настройка системы
        configuration.setRetryMaxAttemptCount(0);

        HttpResponse successResponse = httpResponse(HttpStatus.OK);
        HttpRequestChain successChain = requestChain(successResponse);

        Mockito.when(chain.clone()).thenReturn(successChain);

        // вызов системы
        InitialRequestFilter filter = new InitialRequestFilter(configuration, executor);
        CompletableFuture<HttpResponse> result = filter.doFilter(null, request, chain);

        // проверка утверждений
        Assertions.assertEquals(successResponse, result.get(), "Должны получить результат без преобразований");
    }


}
