package ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author sany-der
 * @date 28/10/2020
 */
public class SskuPartnerVerdictRepositoryMock implements SskuPartnerVerdictRepository {
    private final Map<ShopSkuKey, SskuPartnerVerdictResult> data = new HashMap<>();

    @Override
    public List<SskuPartnerVerdictResult> insertOrUpdateAll(Collection<SskuPartnerVerdictResult> values) {
        values.forEach(v -> data.put(v.getKey(), v));
        return new ArrayList<>(values);
    }

    @Override
    public List<SskuPartnerVerdictResult> findByIds(Collection<ShopSkuKey> keys) {
        return keys.stream().map(data::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<SskuPartnerVerdictResult> findAll() {
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
