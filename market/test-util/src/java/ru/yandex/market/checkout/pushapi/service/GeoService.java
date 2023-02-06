package ru.yandex.market.checkout.pushapi.service;

import ru.yandex.market.checkout.pushapi.client.entity.Region;

public interface GeoService {
    
    Region getRegion(long id);
    
}
