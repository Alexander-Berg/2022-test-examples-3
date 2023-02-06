package ru.yandex.market.deepmind.common.services.availability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailability;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailabilityContext;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

public class ShopSkuMatrixAvailabilityServiceMock implements ShopSkuMatrixAvailabilityService {

    // serviceOfferKey -> warehouse_id -> availabilities
    private final Map<ServiceOfferKey, Map<Long, List<MatrixAvailability>>> map = new HashMap<>();
    private final Map<ServiceOfferKey, Long> shopSkuKeyToMskuId = new HashMap<>();

    public ShopSkuMatrixAvailabilityServiceMock addAvailability(ServiceOfferReplica offer, long warehouseId,
                                                                MatrixAvailability... availabilities) {
        return addAvailability(offer.getServiceOfferKey(), offer.getMskuId(), warehouseId, availabilities);
    }

    public ShopSkuMatrixAvailabilityServiceMock addAvailability(int supplierId, String shopSku, long mskuId,
                                                                long warehouseId,
                                                                MatrixAvailability... availabilities) {
        return addAvailability(new ServiceOfferKey(supplierId, shopSku), mskuId, warehouseId, availabilities);
    }

    public ShopSkuMatrixAvailabilityServiceMock addAvailability(ServiceOfferKey shopSkuKey, long mskuId,
                                                                long warehouseId,
                                                                MatrixAvailability... availabilities) {
        Map<Long, List<MatrixAvailability>> skuMap = map.computeIfAbsent(shopSkuKey, __ -> new HashMap<>());
        List<MatrixAvailability> warehouseAvailabilities = skuMap.computeIfAbsent(warehouseId, __ -> new ArrayList<>());
        warehouseAvailabilities.addAll(Arrays.asList(availabilities));
        shopSkuKeyToMskuId.put(shopSkuKey, mskuId);
        return this;
    }

    @Override
    public Map<ServiceOfferKey, ShopSkuAvailability> computeAvailability(Collection<ServiceOfferKey> shopSkuKeys,
                                                                    ShopSkuAvailabilityContext context) {
        List<ShopSkuAvailability> result = new ArrayList<>();

        for (ServiceOfferKey shopSkuKey : shopSkuKeys) {
            Long mskuId = shopSkuKeyToMskuId.getOrDefault(shopSkuKey, -1L);

            ShopSkuAvailability shopSkuAvailability =
                new ShopSkuAvailability(shopSkuKey.getSupplierId(), shopSkuKey.getShopSku(), mskuId);

            Map<Long, List<MatrixAvailability>> warehouseIdToAvailabilities = map.get(shopSkuKey);
            if (warehouseIdToAvailabilities == null) {
                continue;
            }

            for (Map.Entry<Long, List<MatrixAvailability>> e : warehouseIdToAvailabilities.entrySet()) {
                long warehouseId = e.getKey();
                List<MatrixAvailability> availabilities = e.getValue();
                if (context.getWarehouseIds().contains(warehouseId)) {
                    shopSkuAvailability.addAvailabilities(warehouseId, availabilities);
                }
            }
            result.add(shopSkuAvailability);
        }
        return result.stream()
            .collect(Collectors.toMap(
                ShopSkuAvailability::getServiceOfferKey,
                Function.identity()
            ));
    }

    public void clear() {
        map.clear();
        shopSkuKeyToMskuId.clear();
    }
}
