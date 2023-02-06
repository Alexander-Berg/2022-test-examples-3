package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuExistenceRepositoryMock implements SskuExistenceRepository {
    private final Map<ShopSkuKey, Boolean> data = new HashMap<>();

    @Override
    public void markExistence(Collection<ShopSkuKey> keys, boolean existence) {
        for (ShopSkuKey key : keys) {
            data.put(key, existence);
        }
    }

    @Override
    public Set<ShopSkuKey> retainExisting(Collection<ShopSkuKey> keys) {
        return keys.stream().filter(k -> data.getOrDefault(k, false)).collect(Collectors.toSet());
    }

    @Override
    public void clearRepository() {
        data.clear();
    }
}
