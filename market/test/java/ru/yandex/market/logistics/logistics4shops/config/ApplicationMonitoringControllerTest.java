package ru.yandex.market.logistics.logistics4shops.config;

import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.monitoring.health.HealthService;

class ApplicationMonitoringControllerTest extends AbstractTest {

    @Test
    @DisplayName("Вызов close отрабатывает корректно")
    public void closeInvocation() throws InterruptedException {
        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
        ComplexMonitoring pingMonitoring = Mockito.spy(ComplexMonitoring.class);
        ComplexMonitoring.Result result = Mockito.mock(ComplexMonitoring.Result.class);

        Mockito.when(pingMonitoring.getResult()).thenReturn(result);
        Mockito.when(result.getStatus()).thenReturn(MonitoringStatus.CRITICAL);

        ApplicationMonitoringController controller = new ApplicationMonitoringController(
            Mockito.mock(ComplexMonitoring.class),
            pingMonitoring,
            Mockito.mock(HealthService.class),
            applicationContext,
            1
        );

        IntConsumer exitCodeConsumer = Mockito.mock(IntConsumer.class);

        controller.finishApplication(exitCodeConsumer);
        controller.finishApplication(exitCodeConsumer);
        TimeUnit.MILLISECONDS.sleep(1500);
        Mockito.verify(applicationContext, Mockito.times(1)).close();
        Mockito.verify(exitCodeConsumer, Mockito.times(1)).accept(Mockito.anyInt());
    }

    @Test
    @DisplayName("Вызов ping провоцирует обновление хелсчека")
    void pingInvocation() {
        ComplexMonitoring pingMonitoring = Mockito.spy(ComplexMonitoring.class);
        HealthService healthService = Mockito.mock(HealthService.class);
        ComplexMonitoring.Result result = new ComplexMonitoring.Result(MonitoringStatus.CRITICAL, "some msg");
        Mockito.when(pingMonitoring.getResult()).thenReturn(result);

        ApplicationMonitoringController controller = new ApplicationMonitoringController(
            pingMonitoring,
            pingMonitoring,
            healthService,
            Mockito.mock(ApplicationContext.class),
            5
        );

        softly.assertThat(controller.ping())
            .isEqualTo(new ResponseEntity<>("2;some msg", HttpStatus.INTERNAL_SERVER_ERROR));
        Mockito.verify(healthService).updateChecks();
    }
}
