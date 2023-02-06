package ru.yandex.market.mapi.configprovider

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.application.monitoring.ComplexMonitoring
import ru.yandex.market.application.monitoring.MonitoringStatus
import ru.yandex.market.mapi.AbstractMapiTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdmConfigRefresherTest : AbstractMapiTest() {

    private lateinit var configRefresher: AdmConfigRefresher

    @Autowired
    private lateinit var configProviders: List<RefresheableConfigProvider>

    @Autowired
    private lateinit var monitoring: ComplexMonitoring

    @BeforeEach
    fun setup() {
        configRefresher = AdmConfigRefresher(configProviders, monitoring)
    }

    @Test
    fun testAllInjectedProvidersWereRefreshed() {
        configRefresher.afterPropertiesSet()
        configProviders.forEach {
            verify(it).refreshConfig()
        }

        assertEquals(MonitoringStatus.OK, monitoring.result.status)
    }

    @Test
    fun testRefreshFunCallsAllInjectedProviders() {
        configRefresher.refreshConfigs()
        configProviders.forEach {
            verify(it).refreshConfig()
        }

        assertEquals(MonitoringStatus.OK, monitoring.result.status)
    }

    @Test
    fun testErrorWhileRefreshing() {
        val anyConfig = configProviders[0]
        whenever(anyConfig.refreshConfig()).doThrow(RuntimeException("test exception"))

        configRefresher.refreshConfigs()

        assertEquals(MonitoringStatus.CRITICAL, monitoring.result.status)
    }

    @Test
    fun testMultiErrorsWhileRefreshing() {
        configProviders.forEachIndexed { index, provider ->
            whenever(provider.refreshConfig()).doThrow(RuntimeException("test exception $index"))
        }

        configRefresher.refreshConfigs()

        assertEquals(MonitoringStatus.CRITICAL, monitoring.result.status)

        val rawResponse = mvcCall(
            requestBuilder = MockMvcRequestBuilders.get("/monitoring"),
            expected = MockMvcResultMatchers.status().is5xxServerError,
            expectedType = MediaType.parseMediaType("text/plain;charset=UTF-8")
        )

        // only one exception is used
        assertTrue {
            rawResponse.endsWith(", test exception 0}")
        }
    }
}
