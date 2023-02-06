package ru.yandex.market.marketpromo.web.controller;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class MonitoringControllerTest extends MockedWebTestBase {

    @Test
    void shouldCloseInvocation() throws InterruptedException {
        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
        ComplexMonitoring pingMonitoring = Mockito.mock(ComplexMonitoring.class);
        MonitoringUnit monitoringUnit = Mockito.mock(MonitoringUnit.class);
        ComplexMonitoring.Result result = Mockito.mock(ComplexMonitoring.Result.class);

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

    @Test
    void shouldReturnOkOnPing() throws Exception {
        assertThat(mockMvc.perform(get("/ping")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), is("0;OK"));
    }
}
