package ru.yandex.market.wms.radiator.service.stocks.redis;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.service.config.Dispatcher;

class RedisCacheExpirationItemsServiceTest extends BaseRedisCacheItemStocksServiceTest {

    @Autowired
    RedisCacheExpirationItemsService service;
    @Autowired
    Dispatcher dispatcher;

    @Override
    protected RedisCacheExpirationItemsService service() {
        return service;
    }
}
