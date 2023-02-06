package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.math.BigDecimal;
import java.util.Map;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils;
import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

public interface SkuTestData {

    static Map<SkuId, Dimensions> skuDimensions() {
        return CollectionsUtils.mapOf(
                new Pair<>(new SkuId("100", "ROV0000000000000000003"),
                        new Dimensions.DimensionsBuilder()
                                .weight(BigDecimal.valueOf(10d))
                                .cube(BigDecimal.valueOf(10d))
                                .build()),

                new Pair<>(new SkuId("100", "ROV0000000000000000006"),
                        new Dimensions.DimensionsBuilder()
                                .weight(BigDecimal.valueOf(20d))
                                .cube(BigDecimal.valueOf(20d))
                                .build()),

                new Pair<>(new SkuId("100", "ROV0000000000000000004"),
                        new Dimensions.DimensionsBuilder()
                                .weight(BigDecimal.valueOf(10d))
                                .cube(BigDecimal.valueOf(10d))
                                .build())
        );
    }
}
