package ru.yandex.market.deepmind.common.assertions.custom;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailability;

public class ShopSkuAvailabilityAssertions
    extends AbstractObjectAssert<ShopSkuAvailabilityAssertions, ShopSkuAvailability> {

    public ShopSkuAvailabilityAssertions(ShopSkuAvailability actual) {
        super(actual, ShopSkuAvailabilityAssertions.class);
    }

    public ListAssert<MatrixAvailability> findByWarehouseId(long... warehouseIds) {
        List<MatrixAvailability> res = Arrays.stream(warehouseIds).boxed()
            .flatMap(wid -> actual.getAllMatrixAvailabilities(wid).stream())
            .collect(Collectors.toList());
        return Assertions.assertThat(res);
    }

    public ShopSkuAvailabilityAssertions containsExactlyInAnyOrder(
        long warehouseId, MatrixAvailability... availabilities
    ) {
        findByWarehouseId(warehouseId).containsExactlyInAnyOrder(availabilities);
        return myself;
    }

    public ShopSkuAvailabilityAssertions doesNotContainForWarehouseIds(long... warehouseIds) {
        findByWarehouseId(warehouseIds).isNullOrEmpty();
        return myself;
    }
}
