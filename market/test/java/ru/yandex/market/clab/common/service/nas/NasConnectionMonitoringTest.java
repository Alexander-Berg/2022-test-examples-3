package ru.yandex.market.clab.common.service.nas;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.12.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class NasConnectionMonitoringTest {
    @Mock
    private NasService nasService;
    private NasConnectionMonitoring nasConnectionMonitoring;

    @Before
    public void before() {
        nasConnectionMonitoring = new NasConnectionMonitoring(nasService);
    }

    @Test
    public void testOk() {
        when(nasService.list(anyString())).thenReturn(Collections.emptyList());

        ComplexMonitoring.Result result = nasConnectionMonitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    @Test
    public void testException() {
        RuntimeException exception = new RuntimeException("connection exception");
        when(nasService.list(anyString())).thenThrow(exception);

        assertThatThrownBy(nasConnectionMonitoring::check).isEqualTo(exception);
    }
}
