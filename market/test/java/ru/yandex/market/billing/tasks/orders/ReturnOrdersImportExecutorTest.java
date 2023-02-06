package ru.yandex.market.billing.tasks.orders;

import java.time.Duration;
import java.time.Instant;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.billing.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link ReturnOrdersImportExecutor}
 */
@ExtendWith(MockitoExtension.class)
class ReturnOrdersImportExecutorTest extends FunctionalTest {

    @Mock
    private ImportReturnOrdersService service;

    @Captor
    private ArgumentCaptor<Instant> captor;

    @Test
    void testExpectedInstant() {
        new ReturnOrdersImportExecutor(service).doJob(null);
        Mockito.verify(service).importData(captor.capture());

        Instant expected = Instant.now();
        assertTrue(Duration.between(expected, captor.getValue()).getSeconds() < 10L);
    }
}
