package ru.yandex.market.wms.taskrouter.config

import okhttp3.mockwebserver.MockWebServer
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestExecutionListeners
import ru.yandex.market.wms.common.spring.IntegrationTest

@Import(TestConfig::class)
@TestExecutionListeners(WithSecurityContextTestExecutionListener::class)
open class BaseTest : IntegrationTest() {

    companion object {

        @JvmStatic
        val ttsServer = MockWebServer()

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("tts.host") { ttsServer.url("/").toUrl().toString() }
            registry.add("tts.warehouse-name") { "SOF" }
            registry.add("wms.taskrouter.tvm.serviceId") { 1 }
            registry.add("wms.taskrouter.tvm.secret") { "" }
            registry.add("wms.taskrouter.tvm.cache.dir") { "" }
            registry.add("wms.taskrouter.tvm.serviceId") { 1 }
            registry.add("tts.tvm.serviceId") { 1 }
        }
    }
}
