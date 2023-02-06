package ru.yandex.market.kotopes

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.context.ConfigurableApplicationContext
import ru.yandex.market.application.monitoring.ComplexMonitoring
import ru.yandex.market.application.monitoring.MonitoringStatus
import ru.yandex.market.application.monitoring.MonitoringUnit
import ru.yandex.market.javaframework.main.config.ApplicationMonitoringController
import java.util.concurrent.TimeUnit

class ApplicationMonitoringControllerTest {
    @Test
    @Disabled
    fun closeInvocation() {
        val applicationContext = mock(ConfigurableApplicationContext::class.java)
        val pingMonitoring = mock(ComplexMonitoring::class.java)
        val monitoringUnit = mock(MonitoringUnit::class.java)
        val result = mock(ComplexMonitoring.Result::class.java)
        `when`(pingMonitoring.createUnit(anyString())).thenReturn(monitoringUnit)
        `when`(pingMonitoring.result).thenReturn(result)
        `when`(result.status).thenReturn(MonitoringStatus.CRITICAL)
        val controller = ApplicationMonitoringController(any(), pingMonitoring, applicationContext)
        controller.finishContext(applicationContext, 0)
        controller.finishContext(applicationContext, 0)
        TimeUnit.SECONDS.sleep(1)
        verify(applicationContext, times(1)).close()
    }
}
