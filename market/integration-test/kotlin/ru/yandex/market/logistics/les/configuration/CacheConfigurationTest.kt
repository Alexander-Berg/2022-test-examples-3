package ru.yandex.market.logistics.les.configuration

import com.github.benmanes.caffeine.cache.Cache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import ru.yandex.market.logistics.les.AbstractContextualTest
import java.time.Duration

class CacheConfigurationTest : AbstractContextualTest() {

    companion object {
        private val expectedCacheNamesWithDurations = mapOf(
            "getRoutingCache" to Duration.ofMinutes(1),
            "getFlagCache" to Duration.ofMinutes(2),
            "getBooleanFlagCache" to Duration.ofMinutes(3),
            "payloadParsingCanBeSkippedQueuesCache" to Duration.ofMinutes(1),
        )
    }

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Test
    fun testCreatedCaches() {
        val cacheNames = cacheManager.cacheNames

        val actualCacheNamesWithDuration = cacheNames.mapNotNull { cacheManager.getCache(it) }
            .filterIsInstance<CaffeineCache>()
            .associate { it.name to (it.nativeCache as Cache).policy().expireAfterWrite().get().expiresAfter }

        assertThat(actualCacheNamesWithDuration).containsExactlyInAnyOrderEntriesOf(expectedCacheNamesWithDurations)
    }
}
