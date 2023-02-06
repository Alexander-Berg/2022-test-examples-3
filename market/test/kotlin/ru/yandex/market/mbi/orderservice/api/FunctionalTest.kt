package ru.yandex.market.mbi.orderservice.api

import org.apache.http.client.utils.URIBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import ru.yandex.market.common.test.junit.JupiterDbUnitTest
import ru.yandex.market.mbi.helpers.YTCleanerExtension
import ru.yandex.market.mbi.orderservice.api.config.FunctionalTestConfig
import ru.yandex.market.mbi.orderservice.api.config.LocalYTConfig
import ru.yandex.market.mbi.orderservice.api.config.SpringApplicationConfig
import ru.yandex.market.yt.client.YtDynamicTableClientFactory
import java.net.URI

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [SpringApplicationConfig::class])
@SpringJUnitConfig(classes = [FunctionalTestConfig::class])
@ActiveProfiles(profiles = ["functionalTest", "development", "localYt"])
@TestPropertySource(locations = ["classpath:functional-test.properties"])
@Import(LocalYTConfig::class)
@ExtendWith(YTCleanerExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Configuration
open class FunctionalTest : JupiterDbUnitTest() {

    @LocalServerPort
    private var port = 0

    val baseUrl: String
        get() = "http://localhost:$port"

    protected fun getUri(path: String, params: Map<String, String>): URI =
        getUri(path, params.entries.map { it.key to it.value })

    protected fun getUri(path: String, params: List<Pair<String, String>>): URI {
        val uriBuilder = URIBuilder(baseUrl)
        uriBuilder.path = path
        params.forEach { (param: String, value: String) ->
            uriBuilder.addParameter(
                param,
                value
            )
        }
        return uriBuilder.build()
    }


    @Autowired
    lateinit var initializer: YtDynamicTableClientFactory.OnDemandInitializer

    @BeforeAll
    fun createSchema() {
        initializer.configureDynamicTables()
    }
}
