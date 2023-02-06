package ru.yandex.market.antifraud.orders.util;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.antifraud.orders.service.logging.AntifraudLoggingUtil;
import ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@SuppressWarnings("unchecked")
public class RetriableAsyncFunctionsTest {

    @Before
    public void setUp() {
        RequestContextHolder.setContext(new RequestContext(AntifraudTestUtils.REQUEST_ID));
    }

    @Test
    public void retries() {
        AntifraudLoggingUtil.logAsyncResult("test", null);
        var function = getFunctionWithDelays();
        var retriableAsyncFunction = RetriableAsyncFunctions.withTimeoutFunctionBuilder(function, Duration.ofMillis(20))
            .retryCount(4)
            .build();
        assertThat(retriableAsyncFunction.apply("key")).succeedsWithin(100, TimeUnit.MILLISECONDS)
            .isIn(15, 25, 35, 45);
        Mockito.verify(function, times(4)).apply("key");
    }

    @Test
    public void limitRetries() {
        AntifraudLoggingUtil.logAsyncResult("test", null);
        var function = getFunctionWithDelays();
        var retriableAsyncFunction = RetriableAsyncFunctions.withTimeoutFunctionBuilder(function, Duration.ofMillis(20))
            .retryCount(3)
            .build();
        assertThat(retriableAsyncFunction.apply("key").handle((r, e) -> e)).succeedsWithin(90, TimeUnit.MILLISECONDS, THROWABLE)
            .isInstanceOf(TimeoutException.class);
        Mockito.verify(function, times(3)).apply("key");
    }

    @Test
    public void requestContextIsPresent() {
        AntifraudLoggingUtil.logAsyncResult("test", null);
        var function = getFunctionWithDelays();
        var retriableAsyncFunction = RetriableAsyncFunctions.withTimeoutFunctionBuilder(function, Duration.ofMillis(20))
                .retryCount(4)
                .build();

        assertThat(retriableAsyncFunction.apply("key").handle((res, err) -> {
            RequestContext actualContext = RequestContextHolder.getContext();
            return Optional.ofNullable(actualContext).map(RequestContext::getRequestId).orElse("-1");
        })).succeedsWithin(100, TimeUnit.MILLISECONDS).isEqualTo(AntifraudTestUtils.REQUEST_ID);
        Mockito.verify(function, times(4)).apply("key");
    }

    private AsyncFunction<String, Integer> getFunctionWithDelays() {
        AsyncFunction<String, Integer> function = Mockito.mock(AsyncFunction.class);

        Mockito.when(function.apply(any()))
            .then(returnDelayed(45))
            .then(returnDelayed(35))
            .then(returnDelayed(25))
            .then(returnDelayed(15))
            .then(returnDelayed(5));
        return function;
    }

    private Answer<CompletableFuture<Integer>> returnDelayed(int delay) {
        return inv -> CompletableFuture.supplyAsync(() -> delay, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS));
    }
}
