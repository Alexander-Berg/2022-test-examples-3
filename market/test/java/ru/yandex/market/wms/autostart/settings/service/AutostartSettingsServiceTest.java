package ru.yandex.market.wms.autostart.settings.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.configuration.CacheConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Disabled
public class AutostartSettingsServiceTest extends AutostartIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AutostartSettingsService autostartSettingsService;

    private Integer getCachedWarehouseCutoffShift() {
        Cache cache = cacheManager.getCache(CacheConfig.WAREHOUSE_CUTOFF_SHIFT_CONFIG_CACHE);
        return cache.get(new SimpleKey(), Integer.class);
    }

    @Test
    void getWarehouseCutoffShiftCacheTest() {
        autostartSettingsService.getWarehouseCutoffShift();
        assertThat(
                autostartSettingsService.getWarehouseCutoffShift(),
                is(equalTo(getCachedWarehouseCutoffShift()))
        );
    }
}
