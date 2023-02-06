package ru.yandex.market.tsup.core.cache.sly_cacher.invalidation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.cache.sly_cacher.CacheProvider;
import ru.yandex.market.tsup.core.cache.sly_cacher.CachingKey;
import ru.yandex.market.tsup.core.cache.sly_cacher.SlyCached;
import ru.yandex.market.tsup.core.cache.sly_cacher.service.CacheReadWriteService;
import ru.yandex.market.tsup.core.data_provider.provider.TestDataProvider;
import ru.yandex.market.tsup.core.data_provider.provider.TestProviderFilter;

class DefaultInvalidationStrategyTest extends AbstractContextualTest {
    @Autowired
    private DefaultInvalidationStrategy strategy;

    @Autowired
    private TestDataProvider dataProvider;

    @Autowired
    private TestableClock clock;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private CacheProvider cacheProvider;

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(lmsClient);
    }

    @DatabaseSetup(
        "/repository/data_provider_log/default_cache_strategy.xml"
    )
    @Test
    void processLogRecords() throws InterruptedException {
        CachingKey keyToUpdate = CacheReadWriteService
            .crearteCachingKey(dataProvider, new TestProviderFilter().setP(2L));
        CachingKey keyToDelete = CacheReadWriteService
            .crearteCachingKey(dataProvider, new TestProviderFilter().setP(4L));
        CachingKey keyToKeep = CacheReadWriteService
            .crearteCachingKey(dataProvider, new TestProviderFilter().setP(5L));

        cacheProvider.set(keyToDelete, "abcd");
        cacheProvider.set(keyToKeep, "abcd");

        clock.setFixed(Instant.parse("2021-11-02T16:01:00Z"), ZoneOffset.UTC);

        SlyCached annotation = Mockito.mock(SlyCached.class);
        Mockito.when(annotation.cacheTtl()).thenReturn(4);
        Mockito.when(annotation.cacheTtlTimeUnit()).thenReturn(TimeUnit.HOURS);

        strategy.processLogRecords(dataProvider, TestProviderFilter.class, annotation);
        Thread.sleep(1_000);

        Mockito.verify(lmsClient).getLogisticsPoint(2L);
        softly.assertThat(cacheProvider.exists(keyToUpdate)).isTrue();

        softly.assertThat(cacheProvider.exists(keyToDelete)).isFalse();
        softly.assertThat(cacheProvider.exists(keyToKeep)).isTrue();
    }
}
