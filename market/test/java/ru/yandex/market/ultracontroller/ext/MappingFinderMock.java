package ru.yandex.market.ultracontroller.ext;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.InitializingBean;

import ru.yandex.market.ir.uc.SkuMappingKey;
import ru.yandex.market.ir.uc.SkuMappingValue;
import ru.yandex.market.ultracontroller.ext.datastorage.DataStorage;
import ru.yandex.market.ultracontroller.yt.MappingFinder;

public class MappingFinderMock implements MappingFinder<SkuMappingValue>, InitializingBean {
    private DataStorage dataStorage;
    private Map<SkuMappingKey, SkuMappingValue> skuMappingValueMap;

    @Override
    public void afterPropertiesSet() {
        skuMappingValueMap = dataStorage.getSkuMappingValueMap();
    }

    @Override
    public Optional<SkuMappingValue> findSku(long shopId, String skuShop) {
        SkuMappingKey key = SkuMappingKey.of(shopId, skuShop);
        return Optional.ofNullable(skuMappingValueMap.get(key));
    }

    public void setDataStorage(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }
}
