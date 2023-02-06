package ru.yandex.market.deepmind.common.assertions.custom;

import java.util.Optional;
import java.util.stream.StreamSupport;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.FactoryBasedNavigableIterableAssert;
import org.assertj.core.api.ObjectEnumerableAssert;

import ru.yandex.market.deepmind.common.availability.ShopSkuWKey;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailability;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;

/**
 * @author s-ermakov
 */
public class IterableShopSkuAvailabilityAssertions
    extends FactoryBasedNavigableIterableAssert<IterableShopSkuAvailabilityAssertions,
    Iterable<? extends ShopSkuAvailability>, ShopSkuAvailability, ShopSkuAvailabilityAssertions>
    implements ObjectEnumerableAssert<IterableShopSkuAvailabilityAssertions, ShopSkuAvailability> {

    public IterableShopSkuAvailabilityAssertions(Iterable<? extends ShopSkuAvailability> iterable) {
        super(iterable, IterableShopSkuAvailabilityAssertions.class, ShopSkuAvailabilityAssertions::new);
    }

    public IterableShopSkuAvailabilityAssertions containsExactlyInAnyOrderShopSkuKeys(ServiceOfferKey... shopSkuKeys) {
        Assertions.assertThat(actual)
            .extracting(ShopSkuAvailability::getServiceOfferKey)
            .containsExactlyInAnyOrder(shopSkuKeys);
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions containsShopSkuKey(ServiceOfferKey shopSkuKey) {
        isNotEmpty();
        Optional<? extends ShopSkuAvailability> first = StreamSupport.stream(actual.spliterator(), false)
            .filter(s -> s.getServiceOfferKey().equals(shopSkuKey)).findFirst();
        if (first.isEmpty()) {
            failWithMessage("No element with %s in list: %s", shopSkuKey, actual);
        }
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions containsShopSkuKey(int supplierId, String shopSku) {
        return containsShopSkuKey(new ServiceOfferKey(supplierId, shopSku));
    }

    public IterableShopSkuAvailabilityAssertions doesNotContainShopSkuKey(ServiceOfferKey shopSkuKey) {
        Optional<? extends ShopSkuAvailability> first = StreamSupport.stream(actual.spliterator(), false)
            .filter(s -> s.getServiceOfferKey().equals(shopSkuKey)).findFirst();
        if (first.isPresent()) {
            failWithMessage("Element with %s exists in list: %s", shopSkuKey, actual);
        }
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions doesNotContainShopSkuKey(int supplierId, String shopSkuKey) {
        return doesNotContainShopSkuKey(new ServiceOfferKey(supplierId, shopSkuKey));
    }

    public ShopSkuAvailabilityAssertions findByShopSkuKey(ServiceOfferKey shopSkuKey) {
        containsShopSkuKey(shopSkuKey);
        ShopSkuAvailability availability = StreamSupport.stream(actual.spliterator(), false)
            .filter(s -> s.getServiceOfferKey().equals(shopSkuKey)).findFirst().get();
        return toAssert(availability, navigationDescription("check element " + shopSkuKey));
    }

    public ShopSkuAvailabilityAssertions findByShopSkuKey(int supplierId, String shopSku) {
        return findByShopSkuKey(new ServiceOfferKey(supplierId, shopSku));
    }

    public IterableShopSkuAvailabilityAssertions containsExactlyInAnyOrder(
        int supplierId, String shopSku, long warehouseId, MatrixAvailability... availabilities
    ) {
        containsExactlyInAnyOrder(new ShopSkuWKey(supplierId, shopSku, warehouseId),
            availabilities);
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions containsExactlyInAnyOrder(
        ServiceOfferKey key, long warehouseId, MatrixAvailability... availabilities
    ) {
        containsExactlyInAnyOrder(new ShopSkuWKey(key.getSupplierId(), key.getShopSku(), warehouseId),
            availabilities);
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions containsExactlyInAnyOrder(
        ShopSkuWKey key, MatrixAvailability... availabilities
    ) {
        findByShopSkuKey(key.getSupplierId(), key.getShopSku())
            .findByWarehouseId(key.getWarehouseId())
            .containsExactlyInAnyOrder(availabilities);
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions doesNotContainForWarehouseId(
        int supplierId, String shopSku, long warehouseId) {
        findByShopSkuKey(supplierId, shopSku).doesNotContainForWarehouseIds(warehouseId);
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions doesNotContainForWarehouseId(ServiceOfferKey key, long warehouseId) {
        doesNotContainForWarehouseId(key.getSupplierId(), key.getShopSku(), warehouseId);
        return myself;
    }

    public IterableShopSkuAvailabilityAssertions doesNotContainForWarehouseId(ShopSkuWKey key) {
        doesNotContainForWarehouseId(key.getSupplierId(), key.getShopSku(), key.getWarehouseId());
        return myself;
    }
}
