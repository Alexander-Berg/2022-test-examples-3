package ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 20/10/2020
 */
public class SskuGoldenVerdictRepositoryMock
    implements SskuGoldenVerdictRepository {
    private final Map<ShopSkuKey, SskuVerdictResult> data = new HashMap<>();

    @Override
    public List<SskuVerdictResult> insertOrUpdateAll(Collection<SskuVerdictResult> values) {
        values.forEach(v -> data.put(v.getKey(), v));
        return new ArrayList<>(values);
    }

    @Override
    public List<SskuVerdictResult> findByIds(Collection<ShopSkuKey> keys) {
        return keys.stream().map(data::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<SskuVerdictResult> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void deleteAll() {
        data.clear();
    }

    @Override
    public void delete(List<ShopSkuKey> ids) {
        ids.forEach(data::remove);
    }
}
