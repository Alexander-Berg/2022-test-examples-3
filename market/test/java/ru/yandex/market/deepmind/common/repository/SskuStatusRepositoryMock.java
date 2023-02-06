package ru.yandex.market.deepmind.common.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusFilter;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;

public class SskuStatusRepositoryMock implements SskuStatusRepository {

    private final Map<ServiceOfferKey, SskuStatus> statusMap = new HashMap<>();

    @Override
    public List<SskuStatus> findAll() {
        return new ArrayList<>(statusMap.values());
    }

    @Override
    public List<SskuStatus> find(SskuStatusFilter filter) {
        return findAll();
    }

    @Override
    public int findCount(SskuStatusFilter filter) {
        return statusMap.size();
    }

    @Override
    public Map<OfferAvailability, Integer> countByStatus() {
        var map =  statusMap.values().stream().collect(Collectors.groupingBy(
            SskuStatus::getAvailability,
            HashMap::new, Collectors.counting()));
        return map.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().intValue()));
    }

    @Override
    public List<ServiceOfferKey> findKeys(SskuStatusFilter filter) {
        return new ArrayList<>(statusMap.keySet());
    }

    @Override
    public Optional<SskuStatus> findByKey(int supplierId, String shopSku) {
        return Optional.ofNullable(statusMap.get(new ServiceOfferKey(supplierId, shopSku)));
    }

    @Override
    public void save(Collection<SskuStatus> offerStatuses) {
        offerStatuses.forEach(it -> statusMap.put(new ServiceOfferKey(it.getSupplierId(), it.getShopSku()), it));
    }

    @Override
    public void deleteByKey(Collection<ServiceOfferKey> shopSkuKeys) {
        shopSkuKeys.forEach(statusMap::remove);
    }
}
