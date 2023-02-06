package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuRslRepositoryMock extends GenericMapperRepositoryMock<SskuRsl, SskuRsl.Key>
    implements SskuRslRepository {

    public SskuRslRepositoryMock() {
        super(null, SskuRsl::getKey);
    }

    @Override
    protected SskuRsl.Key nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<ShopSkuKey, List<SskuRsl>> findByShopSkuKeys(Collection<ShopSkuKey> keys) {
        Set<ShopSkuKey> uniqueKeys = new HashSet<>(keys);
        return findAll().stream()
            .filter(i -> uniqueKeys.contains(i.getShopSkuKey()))
            .collect(Collectors.groupingBy(SskuRsl::getShopSkuKey));
    }
}
