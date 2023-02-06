package ru.yandex.market.antifraud.orders.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.dao.TariffDao;
import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CacheIntegrationTest {

    @Autowired
    public TariffDao tariffDao;

    @Autowired
    @Qualifier("localCacheManager")
    public CacheManager localCacheManager;

    @Test
    public void checkTariffDaoLocalCache() {
        var defaultParams = tariffDao.getLatestGlobalParams().get();
        var newParams = tariffDao.saveGlobalParams(defaultParams);
        var cached = tariffDao.getLatestGlobalParams().get();
        assertThat(cached).isEqualTo(defaultParams);
        assertThat(cached).isNotEqualTo(newParams);
        localCacheManager.getCache("global-params-cache").clear();
        var latestParams = tariffDao.getLatestGlobalParams().get();
        assertThat(latestParams).isEqualTo(newParams);
    }

}
