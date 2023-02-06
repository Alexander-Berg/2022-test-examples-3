package ru.yandex.market.billing.overdraft;

import java.time.Duration;
import java.time.Instant;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тесты для {@link OverdraftControlExecutor}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OverdraftControlExecutorTest {

    @Mock
    private OverdraftControlService overdraftControlService;

    @Captor
    private ArgumentCaptor<Instant> captor;

    /**
     * Тест не мега важный, но фиксирует вызов с now.
     */
    @Test
    void test_executorStartsWithNow() {
        new OverdraftControlExecutor(overdraftControlService).doJob(null);
        verify(overdraftControlService, times(1))
                .loadAndProcessOverdrafts(captor.capture());

        Instant expected = Instant.now();
        assertTrue(Duration.between(expected, captor.getValue()).getSeconds() < 10L);
    }
}