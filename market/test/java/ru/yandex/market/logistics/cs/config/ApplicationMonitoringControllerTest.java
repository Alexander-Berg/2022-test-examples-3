package ru.yandex.market.logistics.cs.config;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.ComplexMonitoring.Result;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;

class ApplicationMonitoringControllerTest {

    @Test
    void closeInvocation() throws InterruptedException {
        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
        ComplexMonitoring pingMonitoring = Mockito.mock(ComplexMonitoring.class);
        MonitoringUnit monitoringUnit = Mockito.mock(MonitoringUnit.class);
        Result result = Mockito.mock(Result.class);

        Mockito.when(pingMonitoring.createUnit(Mockito.anyString())).thenReturn(monitoringUnit);
        Mockito.when(pingMonitoring.getResult()).thenReturn(result);
        Mockito.when(result.getStatus()).thenReturn(MonitoringStatus.CRITICAL);

        ApplicationMonitoringController controller = new ApplicationMonitoringController(
            Mockito.any(), pingMonitoring, applicationContext
        );

        controller.finishContext(applicationContext, 0);
        controller.finishContext(applicationContext, 0);
        TimeUnit.SECONDS.sleep(1);
        Mockito.verify(applicationContext, Mockito.times(1)).close();
    }
}
