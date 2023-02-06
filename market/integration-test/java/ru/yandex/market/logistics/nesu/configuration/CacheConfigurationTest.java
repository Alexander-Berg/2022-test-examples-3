package ru.yandex.market.logistics.nesu.configuration;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
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

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.configuration.CacheConfiguration;
import ru.yandex.market.logistics.nesu.service.lms.LogisticsPointService;
import ru.yandex.market.logistics.nesu.service.lms.VirtualPartnerService;
import ru.yandex.market.logistics.nesu.service.mbi.MbiService;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheConfigurationTest extends AbstractContextualTest {
    @Autowired
    private MbiService mbiService;

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Autowired
    private VirtualPartnerService virtualPartnerService;

    @Autowired
    private LogisticsPointService logisticsPointService;

    @Autowired
    private MeterRegistry meterRegistry;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("cachedCalls")
    @DisplayName("Тесты кэшей")
    void testCache(String cacheName, Consumer<CacheConfigurationTest> cachedCall) {
        CaffeineCache cache = getCaffeineCache(cacheName);
        CacheStats stats = cache.getNativeCache().stats();
        getHitRateMetric(cacheName);

        //Перый промах, затем 9 попаданий
        IntStream.rangeClosed(1, 10).forEach(i -> cachedCall.accept(this));
        validateHitRateMetric(cacheName, 0.9);
        cache.clear();

        //Промах, отображается в метриках
        cachedCall.accept(this);
        validateHitRateMetric(cacheName, 0.0);

        //Попадание, отображается в метриках
        cachedCall.accept(this);
        validateHitRateMetric(cacheName, 1.0);

        validateCache(cache, stats);
    }

    private static Stream<Arguments> cachedCalls() {
        return Stream.<Pair<String, Consumer<CacheConfigurationTest>>>of(
            Pair.of(
                CacheConfiguration.MBI_SHOP_ACCESS,
                cacheConfigurationTest -> cacheConfigurationTest.mbiService.hasAccess(0L, 0L)
            ),
            Pair.of(
                CacheConfiguration.LMS_VIRTUAL_PARTNERS,
                cacheConfigurationTest -> cacheConfigurationTest.virtualPartnerService.searchVirtualPartners(
                    Set.of(),
                    Function.identity()
                )
            ),
            Pair.of(
                CacheConfiguration.LMS_HIDDEN_PARTNERS,
                cacheConfigurationTest -> cacheConfigurationTest.virtualPartnerService.searchHiddenPartners(Set.of())
            ),
            Pair.of(
                CacheConfiguration.LMS_LOGISTICS_POINTS,
                cacheConfigurationTest -> cacheConfigurationTest.logisticsPointService.searchLogisticsPoints(
                    LogisticsPointFilter.newBuilder().build()
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
        softly.assertThat(cache.getNativeCache().stats().minus(stats).requestCount()).isEqualTo(12);
        softly.assertThat(cache.getNativeCache().stats().minus(stats).hitCount()).isEqualTo(10);
    }

    @Nonnull
    private Double getHitRateMetric(String name) {
        return meterRegistry.get("cache.hit_rate").tags("cache", name).gauge().measure().iterator().next().getValue();
    }

    private void validateHitRateMetric(String name, Double value) {
        softly.assertThat(getHitRateMetric(name)).isEqualTo(value);
    }
}
