package ru.yandex.market.mapi

import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.TestPropertySource
import ru.yandex.market.mapi.client.cms.TemplatorClient
import ru.yandex.market.mapi.core.AbstractSpringTest
import ru.yandex.market.mapi.engine.pumpkin.PumpkinStorage

/**
 * Base mapi test - no DB beans initialized.
 * @author Ilya Kislitsyn / ilyakis@ / 14.06.2022
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = [MapiMockConfig::class]
)
@TestPropertySource("classpath:/test-application.properties")
abstract class AbstractMapiBaseTest : AbstractSpringTest() {

    @Autowired
    private lateinit var templatorClient: TemplatorClient

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var pumpkinStorage: PumpkinStorage

    @AfterEach
    fun clearData() {
        invalidateCache()
        pumpkinStorage.clearCache()
    }

    private fun invalidateCache() {
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }
}
