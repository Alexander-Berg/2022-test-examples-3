package ru.yandex.market.checkout.pushapi.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestOperations;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.pushapi.service.shop.CheckouterShopMetaDataGetter;

/**
 * @author sergeykoles
 * Created on: 26.12.2019
 */
public class StubbedCheckouterShopMetaDataRwService extends CheckouterShopMetaDataGetter {

    @Autowired
    private CheckouterMockConfigurer checkouterMockConfigurer;

    public StubbedCheckouterShopMetaDataRwService(int cacheExpireTimeSeconds, int cacheMaxSize) {
        super(cacheExpireTimeSeconds, cacheMaxSize);
    }

    @Override
    public ShopMetaData updateMeta(long shopId, ShopMetaData data) {
        checkouterMockConfigurer.setShopMetaData(shopId, data);
        cache.refresh(shopId);
        return data;
    }

}
