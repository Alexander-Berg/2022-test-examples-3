package ru.yandex.market.wms.inventory

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ConfigurableApplicationContext
import ru.yandex.market.application.monitoring.ComplexMonitoring
import ru.yandex.market.application.monitoring.MonitoringStatus
import ru.yandex.market.application.monitoring.MonitoringUnit
import ru.yandex.market.javaframework.main.config.ApplicationMonitoringController
import java.util.concurrent.TimeUnit

class ApplicationMonitoringControllerTest {
    @Test
    @Throws(InterruptedException::class)
    fun closeInvocation() {
        val applicationContext = Mockito.mock(
                ConfigurableApplicationContext::class.java
        )
        val pingMonitoring = Mockito.mock(ComplexMonitoring::class.java)
        val monitoringUnit = Mockito.mock(MonitoringUnit::class.java)
        val result = Mockito.mock(ComplexMonitoring.Result::class.java)
        Mockito.`when`(pingMonitoring.createUnit(Mockito.anyString())).thenReturn(monitoringUnit)
        Mockito.`when`(pingMonitoring.result).thenReturn(result)
        Mockito.`when`(result.status).thenReturn(MonitoringStatus.CRITICAL)
        val controller = ApplicationMonitoringController(
                Mockito.any(), pingMonitoring, applicationContext
        )
        controller.finishContext(applicationContext, 0)
        controller.finishContext(applicationContext, 0)
        TimeUnit.SECONDS.sleep(1)
        Mockito.verify(applicationContext, Mockito.times(1)).close()
    }
}
