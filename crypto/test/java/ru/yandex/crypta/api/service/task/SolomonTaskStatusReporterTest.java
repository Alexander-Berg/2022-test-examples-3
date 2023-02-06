package ru.yandex.crypta.api.service.task;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Unit;
import ru.yandex.crypta.service.task.SolomonTaskStatusReporter;
import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.misc.monica.solomon.sensors.PushSensorsData;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class SolomonTaskStatusReporterTest {

    @Test
    public void sendMetrics() {
        SolomonPusher mock = mock(SolomonPusher.class);
        SolomonTaskStatusReporter solomonReporter = new SolomonTaskStatusReporter(mock);

        ArgumentCaptor<PushSensorsData> captureReportedData = ArgumentCaptor.forClass(PushSensorsData.class);
        when(mock.push(captureReportedData.capture())) // capture passed argument for later check
                .thenReturn(CompletableFuture.completedFuture(Unit.U));

        solomonReporter.reportTaskStatus("MY_TASK", "SUCCESS", "test",
                Cf.map("date", "2018-12-11")
        );

        verify(mock).push(any());
        verifyNoMoreInteractions(mock);

        PushSensorsData reportedData = captureReportedData.getValue();
        assertFalse(reportedData.sensors.isEmpty());
        assertTrue(reportedData.sensors.get(0).value > 0);
    }

    @Test
    public void sendMetricsOnlyIfSuccess() {
        SolomonPusher mock = mock(SolomonPusher.class);
        SolomonTaskStatusReporter solomonReporter = new SolomonTaskStatusReporter(mock);

        solomonReporter.reportTaskStatus("sdf", "BAD_STATUS", "sdfsd",
                Cf.map("date", "2018-12-11")
        );

        // must not send anything
        verifyNoInteractions(mock);
    }

}
