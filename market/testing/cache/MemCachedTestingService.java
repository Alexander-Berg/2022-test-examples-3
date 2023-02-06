package ru.yandex.market.core.testing.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ru.yandex.common.cache.memcached.MemCachedService;
import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.cacheable.BulkMemCacheable;
import ru.yandex.common.cache.memcached.cacheable.MemCacheable;
import ru.yandex.common.cache.memcached.cacheable.ServiceMethodBulkMemCacheable;
import ru.yandex.common.cache.memcached.cacheable.ServiceMethodMemCacheable;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.DbTestingService;
import ru.yandex.market.core.testing.FullTestingState;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;

/**
 * @author mkasumov
 */
public class MemCachedTestingService extends DbTestingService implements MemCachedService {

    private final MemCacheable<FullTestingState, Long> fullTestingStateCacheable =
            new ServiceMethodMemCacheable<FullTestingState, Long>(this, "getFullTestingState") {

                @Override
                public FullTestingState queryNonCached(Long datasourceId) {
                    return MemCachedTestingService.super.getFullTestingState(datasourceId);
                }
            };
    private final MemCacheable<TestingState, CacheKey> testingStateCacheable =
            new ServiceMethodMemCacheable<TestingState, CacheKey>(this, "getTestingStatus") {

                @Override
                public TestingState queryNonCached(CacheKey query) {
                    return MemCachedTestingService.super.getTestingStatus(query.getDatasourceId(),
                            query.getShopProgram());
                }
            };
    private final BulkMemCacheable<TestingState, Long> testingStatusBulkCacheable =
            new ServiceMethodBulkMemCacheable<TestingState, Long>(this, "getTestingStatuses") {

                @Override
                public TestingState queryNonCached(Long datasourceId) {
                    Set<Long> datasourceIds = Collections.singleton(datasourceId);
                    return MemCachedTestingService.super.getTestingStatuses(datasourceIds).get(datasourceId);
                }

                @Override
                public Map<Long, TestingState> queryNonCachedBulk(Collection<Long> datasourceIds) {
                    return MemCachedTestingService.super.getTestingStatuses(datasourceIds);
                }
            };
    private MemCachingService memCachingService;
    private MemCachedServiceConfig config;


    public MemCachedTestingService() {
    }

    @Override
    public MemCachingService getMemCachingService() {
        return memCachingService;
    }

    public void setMemCachingService(MemCachingService memCachingService) {
        this.memCachingService = memCachingService;
    }

    @Override
    public MemCachedServiceConfig getConfig() {
        return config;
    }

    public void setConfig(MemCachedServiceConfig config) {
        this.config = config;
    }

    @Override
    public TestingState getTestingStatus(long datasourceId, ShopProgram shopProgram) {
        return memCachingService.query(testingStateCacheable, new CacheKey(datasourceId, shopProgram));
    }

    @Override
    public Map<Long, TestingState> getTestingStatuses(Collection<Long> datasourceIds) {
        return memCachingService.queryBulk(testingStatusBulkCacheable, datasourceIds);
    }

    @Override
    public FullTestingState getFullTestingState(long datasourceId) {
        return memCachingService.query(fullTestingStateCacheable, datasourceId);
    }

    private void cleanTestingStatus(CacheKey cacheKey) {
        memCachingService.clean(testingStateCacheable, cacheKey);
        memCachingService.clean(testingStatusBulkCacheable, cacheKey.getDatasourceId());
        memCachingService.clean(fullTestingStateCacheable, cacheKey.getDatasourceId());
    }

    @Override
    public void updateState(ShopActionContext ctx, TestingState state) {
        super.updateState(ctx, state);
        cleanTestingStatus(new CacheKey(state.getDatasourceId(), state.getTestingType().getShopProgram()));
    }

    @Override
    public void insertState(ShopActionContext ctx, TestingState state) {
        super.insertState(ctx, state);
        cleanTestingStatus(new CacheKey(state.getDatasourceId(), state.getTestingType().getShopProgram()));
    }

    @Override
    public void removeState(ShopActionContext ctx, TestingState state) {
        super.removeState(ctx, state);
        cleanTestingStatus(new CacheKey(state.getDatasourceId(), state.getTestingType().getShopProgram()));
    }

    private static class CacheKey {
        private long datasourceId;
        private ShopProgram shopProgram;

        private CacheKey(long datasourceId, ShopProgram shopProgram) {
            this.datasourceId = datasourceId;
            this.shopProgram = Objects.requireNonNull(shopProgram);
        }

        public long getDatasourceId() {
            return datasourceId;
        }

        public ShopProgram getShopProgram() {
            return shopProgram;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return datasourceId == cacheKey.datasourceId &&
                    shopProgram == cacheKey.shopProgram;
        }

        @Override
        public int hashCode() {
            return Objects.hash(datasourceId, shopProgram);
        }

        // Memcache uses toString method to address cached values...
        @Override
        public String toString() {
            return "CacheKey" + datasourceId + "_" + shopProgram;
        }
    }
}
