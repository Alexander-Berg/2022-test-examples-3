package ru.yandex.market.mboc.tms.executors.monitorings;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.mboc.common.services.msku.sync.YtImportOperations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class MskuImportDelayMonitoringExecutorTest {
    private YtImportOperations ytImportOperationsMock;
    private ComplexMonitoring complexMonitoring = new ComplexMonitoring();
    private MskuImportDelayMonitoringExecutor executor;

    @Before
    public void setup() {
        ytImportOperationsMock = Mockito.mock(YtImportOperations.class);
        complexMonitoring = new ComplexMonitoring();
        executor = new MskuImportDelayMonitoringExecutor(
            ytImportOperationsMock,
            complexMonitoring
        );
    }

    @Test
    public void shouldFailedAfter24hDelay() {
        when(ytImportOperationsMock.lastImportedSessionTime()).thenReturn(
            Instant.now().minus(25, ChronoUnit.HOURS)
        );
        executor.execute();
        assertEquals(MonitoringStatus.CRITICAL, complexMonitoring.getResult().getStatus());
    }

    @Test
    public void shouldOkBefore24hAwait() {
        when(ytImportOperationsMock.lastImportedSessionTime()).thenReturn(
            Instant.now().minus(23, ChronoUnit.HOURS)
        );
        executor.execute();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());
    }

    @Test
    public void shouldOkAfterFixMonitoring() {
        when(ytImportOperationsMock.lastImportedSessionTime())
            .thenReturn(Instant.now().minus(25, ChronoUnit.HOURS))
            .thenReturn(Instant.now().minus(0, ChronoUnit.HOURS));
        executor.execute();
        assertEquals(MonitoringStatus.CRITICAL, complexMonitoring.getResult().getStatus());
        executor.execute();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());
    }
}
