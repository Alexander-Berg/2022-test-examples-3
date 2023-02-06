package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 11/11/2020
 */
public class SilverSskuRepositoryMock implements SilverSskuRepository {

    private final Map<SilverSskuKey, SilverCommonSsku> data = new HashMap<>();

    @Override
    public Map<SilverSskuKey, SilverCommonSsku> findSskusBySilverKeys(Collection<SilverSskuKey> keys,
                                                                      boolean withLock) {
        return keys.stream().map(data::get).filter(Objects::nonNull).collect(
            Collectors.toMap(SilverCommonSsku::getBusinessKey, Function.identity())
        );
    }

    @Override
    public Map<ShopSkuKey, List<SilverCommonSsku>> findSskus(Collection<ShopSkuKey> keys) {
        return data.values().stream()
            .filter(v -> keys.contains(v.getBusinessKey().getShopSkuKey()))
            .collect(Collectors.groupingBy(s -> s.getBusinessKey().getShopSkuKey()));
    }

    @Override
    public Map<SilverSskuKey, SilverCommonSsku> insertOrUpdateSskus(Collection<SilverCommonSsku> sskus) {
        sskus.forEach(ssku -> data.put(ssku.getBusinessKey(), ssku));
        return sskus.stream().collect(Collectors.toMap(SilverCommonSsku::getBusinessKey, Function.identity()));
    }

    @Override
    public Map<ShopSkuKey, List<SskuSilverParamValue>> findParametrizedSskus(Collection<ShopSkuKey> keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<ShopSkuKey, List<SskuSilverParamValue>> findParametrizedSskus(Collection<ShopSkuKey> keys,
                                                                             MasterDataSourceType source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SskuSilverParamValue> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSskus(Collection<SilverCommonSsku> sskus) {
        sskus.stream()
            .map(SilverCommonSsku::getMultikey)
            .flatMap(Set::stream)
            .forEach(data::remove);
    }

    @Override
    public void deleteSskus(Collection<ShopSkuKey> sskuKeys, MasterDataSourceType sourceType) {
        Set<ShopSkuKey> sskuKeysSet = new HashSet<>(sskuKeys);
        List<SilverSskuKey> keysToRemove = data.keySet().stream()
            .filter(key -> sskuKeysSet.contains(key.getShopSkuKey()))
            .filter(key -> key.getSourceType() == sourceType)
            .collect(Collectors.toList());
        keysToRemove.forEach(data::remove);
    }

    @Override
    public Map<ShopSkuKey, List<SilverCommonSsku>> findSskusForSourceType(Collection<ShopSkuKey> keys,
                                                                          MasterDataSourceType source) {
        return data.values().stream()
            .filter(v -> keys.contains(v.getBusinessKey().getShopSkuKey())
                && v.getBusinessKey().getSourceType().equals(source))
            .collect(Collectors.groupingBy(s -> s.getBusinessKey().getShopSkuKey()));
    }

    public Collection<SilverCommonSsku> allSskus() {
        return data.values();
    }
}
