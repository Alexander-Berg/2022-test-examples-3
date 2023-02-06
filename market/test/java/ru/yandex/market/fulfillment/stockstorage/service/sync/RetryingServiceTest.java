package ru.yandex.market.fulfillment.stockstorage.service.sync;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.cqrs.handler.command.MultipleStocksUpdatingServiceTest;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.ExecutionResult;
import ru.yandex.market.fulfillment.stockstorage.service.RetryingService;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;

import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryingServiceTest {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final int SECONDS_BETWEEN_RETRIES = 1;
    private final TestClass mock = mock(TestClass.class);
    private final RetryingService retryingService = new RetryingService(MAX_ATTEMPT_COUNT, SECONDS_BETWEEN_RETRIES,
            mock(SystemPropertyService.class));

    @Test
    public void checkPositiveScenario() {
        when(mock.callMe()).thenReturn(1);

        ExecutionResult<Integer> result = retryingService.execute(mock::callMe);

        Assert.assertTrue(result.isSuccessfulExecution());
        Assert.assertEquals(Integer.valueOf(1), result.getValue());

        //expected that no exceptions are thrown
        result.throwOnFailure(MultipleStocksUpdatingServiceTest.VerySpecificException::new);

        verify(mock).callMe();
    }

    @Test
    public void checkExceptionThrownOnce() {
        when(mock.callMe())
                .thenThrow(new RuntimeException())
                .thenThrow(new RuntimeException())
                .thenReturn(1);

        ExecutionResult<Integer> result = retryingService.execute(mock::callMe);

        Assert.assertTrue(result.isSuccessfulExecution());
        Assert.assertEquals(Integer.valueOf(1), result.getValue());

        //expected that no exceptions are thrown
        result.throwOnFailure(MultipleStocksUpdatingServiceTest.VerySpecificException::new);

        verify(mock, times(3)).callMe();
    }

    @Test
    public void testAttemptsCountExceeded() {
        when(mock.callMe()).thenThrow(new RuntimeException());

        ExecutionResult<Integer> result = retryingService.execute(mock::callMe);

        Assert.assertFalse(result.isSuccessfulExecution());
        Assert.assertNull(result.getValue());

        Assertions.assertThrows(MultipleStocksUpdatingServiceTest.VerySpecificException.class,
                () -> result.throwOnFailure(MultipleStocksUpdatingServiceTest.VerySpecificException::new));

        verify(mock, times(MAX_ATTEMPT_COUNT)).callMe();
    }

    @Test
    public void checkDelayBetweenRetries() {
        when(mock.callMe()).thenThrow(new RuntimeException());
        long time = System.currentTimeMillis();
        retryingService.execute(mock::callMe);
        double timeLeft = ((double) System.currentTimeMillis() - time) / 1000;
        Assert.assertThat(Double.valueOf(SECONDS_BETWEEN_RETRIES * MAX_ATTEMPT_COUNT), lessThan(timeLeft));
    }

    public interface TestClass {
        int callMe();
    }
}
