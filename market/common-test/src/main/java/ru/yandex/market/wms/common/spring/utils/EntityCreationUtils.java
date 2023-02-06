package ru.yandex.market.wms.common.spring.utils;

import java.math.BigDecimal;

import ru.yandex.market.wms.common.model.enums.ShelfLifeIndicatorType;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.pojo.LotAggregatedFields;
import ru.yandex.market.wms.common.spring.pojo.SkuDimensions;

public final class EntityCreationUtils {
    public static final BigDecimal SCALED_ONE = new BigDecimal("1.00000");

    private EntityCreationUtils() {
        throw new AssertionError();
    }

    public static Sku createSku() {
        return createSku("ROV0000000000000001456", "465852");
    }

    public static Sku createSku(SkuDimensions dimensions) {
        return createSku("ROV0000000000000001456", "465852", dimensions);
    }

    public static Sku createSku(String sku, String storerKey) {
        SkuDimensions dimensions = createDimensions(10.16, 9.55, 1.15);
        return createSku(sku, storerKey, dimensions);
    }

    public static Sku createSku(String sku, String storerKey, SkuDimensions dimensions) {
        return Sku.builder()
                .sku(sku)
                .storerKey(storerKey)
                .shelfLifeIndicatorType(ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE)
                .toExpireDays(10)
                .putAwayClass("putAwayClass")
                .dimensions(dimensions)
                .build();
    }

    public static SkuDimensions createDimensions(double grossWeight, double netWeight, double cube) {
        return SkuDimensions.builder()
                .grossWeight(BigDecimal.valueOf(grossWeight))
                .netWeight(BigDecimal.valueOf(netWeight))
                .cube(BigDecimal.valueOf(cube))
                .build();
    }

    public static LotAggregatedFields createLotAggregatedFields(long quantity, long quantityOnHold, double grossWeight,
                                                                double netWeight, double cube) {
        return LotAggregatedFields.builder()
                .quantity(quantity)
                .quantityOnHold(quantityOnHold)
                .dimensions(createDimensions(grossWeight, netWeight, cube))
                .build();
    }
}
