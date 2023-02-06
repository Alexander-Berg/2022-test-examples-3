package ru.yandex.market.mboc.common.erp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.market.mboc.common.erp.model.ErpCCCodeMarkupChange;

public class ErpCCCodeMarkupExporterRepositoryMock implements ErpCCCodeMarkupExporterRepository {
    private final List<ErpCCCodeMarkupChange> data = new ArrayList<>();
    private final Map<String, ErpCCCodeMarkupChange> latestChangesByShopSku = new HashMap<>();

    @Override
    public int insertCCCodeMarkupChanges(List<ErpCCCodeMarkupChange> erpCCCodeMarkupChanges) {
        erpCCCodeMarkupChanges.forEach(this::insert);
        return erpCCCodeMarkupChanges.size();
    }

    private void insert(ErpCCCodeMarkupChange change) {
        data.add(change);
        latestChangesByShopSku.put(change.getShopSku(), change);
    }

    @Override
    public Map<String, ErpCCCodeMarkupChange> findLatestChanges(Collection<String> shopSkus) {
        Map<String, ErpCCCodeMarkupChange> result = new LinkedHashMap<>();
        for (String shopSku: shopSkus) {
            ErpCCCodeMarkupChange latestChange = latestChangesByShopSku.get(shopSku);
            if (latestChange != null) {
                result.put(shopSku, latestChange);
            }
        }
        return result;
    }

    @Override
    public List<ErpCCCodeMarkupChange> findAll() {
        return List.copyOf(data);
    }
}
