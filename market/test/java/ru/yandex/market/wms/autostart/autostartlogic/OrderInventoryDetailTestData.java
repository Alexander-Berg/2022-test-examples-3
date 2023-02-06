package ru.yandex.market.wms.autostart.autostartlogic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuLocationTestData;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuProperties;

public interface OrderInventoryDetailTestData {

    static List<OrderInventoryDetail> sampleInventory() {
        return Arrays.asList(
                invROV0000000000000000001C4100001(1),
                invROV0000000000000000001C4101001(4),
                invROV0000000000000000002C4190002(1),
                invROV0000000000000000003C4110003(1),
                invROV0000000000000000005C4120005(1),
                invROV0000000000000000007C4130007(1),
                invROV0000000000000000009C4140009(1),
                invROV0000000000000000008C4160008(1),
                invROV0000000000000000006C4170006(1),
                invROV0000000000000000004C4180004(1),
                invROV0000000000000000010C5200001(1),
                invROV0000000000000000010C5300001(1)
        );
    }

    static OrderInventoryDetail invROV0000000000000000001C4100001(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000001"))
                .location(PickSkuLocationTestData.c4100001("0000000001"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000001C4101001(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000001"))
                .location(PickSkuLocationTestData.c4101001("0000000001"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000001C4110003(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000001"))
                .location(PickSkuLocationTestData.c4110003("0000000001"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000002C4190002(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000002"))
                .location(PickSkuLocationTestData.c4190002("0000000002"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000003C4110003(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000003"))
                .location(PickSkuLocationTestData.c4110003("0000000003"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000004C4180004(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000004"))
                .location(PickSkuLocationTestData.c4180004("0000000004"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000005C4120005(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000005"))
                .location(PickSkuLocationTestData.c4120005("0000000005"))
                .qty(qty)
                .build();
    }


    static OrderInventoryDetail invROV0000000000000000006C4170006(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000006"))
                .location(PickSkuLocationTestData.c4170006("0000000006"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000007C4130007(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000007"))
                .location(PickSkuLocationTestData.c4130007("0000000007"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000008C4160008(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000008"))
                .location(PickSkuLocationTestData.c4160008("0000000008"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000009C4140009(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000009"))
                .location(PickSkuLocationTestData.c4140009("0000000009"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000010C5200001(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000010"))
                .location(PickSkuLocationTestData.c5200001("0000000010"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000010C5300001(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000011"))
                .location(PickSkuLocationTestData.c5300001("0000000011"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail invROV0000000000000000010C5300002(int qty) {
        return pickSkuTemplate()
                .skuId(new SkuId("100", "ROV0000000000000000011"))
                .location(PickSkuLocationTestData.c5300002("0000000011"))
                .qty(qty)
                .build();
    }

    static OrderInventoryDetail.OrderInventoryDetailBuilder pickSkuTemplate() {
        return OrderInventoryDetail.builder()
                .skuProperties(sampleSkuProperties());
    }


    static SkuProperties sampleSkuProperties() {
        return SkuProperties.builder()
                .packKey("PACK")
                .cartonGroup("BC1")
                .rotation("1")
                .skuRotation("1")
                .rotateby("Lot")
                .shelfLife(BigDecimal.ZERO)
                .build();
    }
}
