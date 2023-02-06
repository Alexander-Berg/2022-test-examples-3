package ru.yandex.market.mbi.orderservice.tms

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import ru.yandex.market.common.test.junit.JupiterDbUnitTest
import ru.yandex.market.mbi.helpers.YTCleanerExtension
import ru.yandex.market.mbi.orderservice.tms.config.FunctionalTestConfig
import ru.yandex.market.mbi.orderservice.tms.config.LocalYTConfig
import ru.yandex.market.mbi.orderservice.tms.config.SpringApplicationConfig
import ru.yandex.market.yt.client.YtDynamicTableClientFactory

@SpringBootTest(classes = [SpringApplicationConfig::class])
@SpringJUnitConfig(classes = [FunctionalTestConfig::class])
@ActiveProfiles(profiles = ["functionalTest", "development", "localYt"])
@TestPropertySource(locations = ["classpath:functional-test.properties"])
@Import(LocalYTConfig::class)
@ExtendWith(YTCleanerExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class FunctionalTest : JupiterDbUnitTest() {

    @Autowired
    lateinit var initializer: YtDynamicTableClientFactory.OnDemandInitializer

    @BeforeAll
    fun createSchema() {
        initializer.configureDynamicTables()
    }
}
