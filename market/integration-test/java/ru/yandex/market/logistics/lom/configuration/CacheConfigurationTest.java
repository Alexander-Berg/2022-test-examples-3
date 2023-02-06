package ru.yandex.market.logistics.lom.configuration;

import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.LogbrokerSourceLockType;
import ru.yandex.market.logistics.lom.service.order.history.LogbrokerSourceService;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheConfigurationTest extends AbstractContextualTest {
    @Autowired
    private LogbrokerSourceService logbrokerSourceService;

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("cachedCalls")
    @DisplayName("Тесты кэшей")
    void testCache(String cacheName, Consumer<CacheConfigurationTest> cachedCall) {
        CaffeineCache cache = getCaffeineCache(cacheName);
        CacheStats stats = cache.getNativeCache().stats();

        //Перый промах, затем 9 попаданий
        IntStream.rangeClosed(1, 10).forEach(i -> cachedCall.accept(this));
        cache.clear();

        validateCache(cache, stats);
    }

    private static Stream<Arguments> cachedCalls() {
        return Stream.<Pair<String, Consumer<CacheConfigurationTest>>>of(
            Pair.of(
                CacheConfiguration.GET_PRODUCER_CONFIG_BY_SOURCE_ID,
                cacheConfigurationTest -> cacheConfigurationTest.logbrokerSourceService.getAsyncProducerConfig(1)
            ),
            Pair.of(
                CacheConfiguration.GET_LOGBROKER_SOURCES_LOCKS,
                cacheConfigurationTest -> cacheConfigurationTest.logbrokerSourceService.getLogbrokerSourcesLocks(
                    LogbrokerSourceLockType.EXPORT_EVENTS
                )
            )
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    private CaffeineCache getCaffeineCache(String cacheName) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        assertThat(cache).isInstanceOf(CaffeineCache.class);
        return (CaffeineCache) cache;
    }

    private void validateCache(CaffeineCache cache, CacheStats stats) {
        softly.assertThat(cache.getNativeCache().stats().minus(stats).requestCount()).isEqualTo(10);
        softly.assertThat(cache.getNativeCache().stats().minus(stats).hitCount()).isEqualTo(9);
    }
}
