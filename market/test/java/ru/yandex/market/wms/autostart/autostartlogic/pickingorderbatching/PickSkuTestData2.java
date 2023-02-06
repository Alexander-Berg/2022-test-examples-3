package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.util.Arrays;
import java.util.List;

import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.PickSkuLocation;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

public interface PickSkuTestData2 {

    static List<PickSku> somePickSkus() {
        return Arrays.asList(
                // logicalLocation unsorted on purpose to check sorting
                pickSkuROV2(7),
                pickSkuROV1(7),
                pickSkuROV3(5)
        );
    }

    static List<PickSku> somePickSkus2() {
        return Arrays.asList(
                // logicalLocation unsorted on purpose to check sorting
                pickSkuROV2(9),
                pickSkuROV1(7),
                pickSkuROV3(5)
        );
    }

    static PickSku pickSkuROV1(int qty) {
        return PickSku.builder()
                .skuId(new SkuId("100", "ROV1"))
                .location(PickSkuLocation.builder().logicalLocation("1").build())
                .qty(qty)
                .build();
    }

    static PickSku pickSkuROV2(int qty) {
        return PickSku.builder()
                .skuId(new SkuId("100", "ROV2"))
                .location(PickSkuLocation.builder().logicalLocation("2").build())
                .qty(/*2*/qty)
                .build();
    }

    static PickSku pickSkuROV3(int qty) {
        return PickSku.builder()
                .skuId(new SkuId("100", "ROV3"))
                .location(PickSkuLocation.builder().logicalLocation("3").build())
                .qty(qty)
                .build();
    }
}
