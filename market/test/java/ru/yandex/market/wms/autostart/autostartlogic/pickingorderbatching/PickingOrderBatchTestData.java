package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils;
import ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData;
import ru.yandex.market.wms.common.spring.dao.entity.AssigmentType;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrder;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrderBatch;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;

import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000001C4100001;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000001C4101001;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000002C4190002;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000003C4110003;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000004C4180004;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000005C4120005;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000006C4170006;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000007C4130007;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000008C4160008;
import static ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickSkuTestData.pickSkuROV0000000000000000009C4140009;

public interface PickingOrderBatchTestData {

    static PickingOrderBatch pickingOrderBatch0(String batchId, String batchKey) {
        return PickingOrderBatch.builder()
                .batchId(batchId)
                .batchKey(batchKey)
                .pickingOrders(
                        CollectionsUtils.listOf(
                                PickingOrder.builder()
                                        .pickingOrderKey("0")
                                        .type(AssigmentType.SORTABLE_CONVEYABLE)
                                        .workingArea("MEZONIN_2")
                                        .items(
                                                CollectionsUtils.listOf(
                                                        pickSkuROV0000000000000000005C4120005(1),
                                                        pickSkuROV0000000000000000007C4130007(1)
                                                )
                                        )
                                        .build()
                        )
                )
                .subBatches(
                        CollectionsUtils.listOf(
                                SubBatch.<OrderWithDetails>builder().sortingStation("S01")
                                        .orders(
                                                CollectionsUtils.listOf(
                                                        OrderWithDetailsTestData.owdB000003003(),
                                                        OrderWithDetailsTestData.owdB000004001()
                                                )
                                        )
                                        .build()
                        )
                )
                .build();
    }

    static PickingOrderBatch pickingOrderBatch1(String batchId, String batchKey) {
        return PickingOrderBatch.builder()
                .batchId(batchId)
                .batchKey(batchKey)
                .pickingOrders(
                        CollectionsUtils.listOf(
                                PickingOrder.builder()
                                        .pickingOrderKey("1")
                                        .type(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                                        .workingArea("MEZONIN_2")
                                        .items(
                                                CollectionsUtils.listOf(
                                                        pickSkuROV0000000000000000003C4110003(1),
                                                        pickSkuROV0000000000000000006C4170006(1),
                                                        pickSkuROV0000000000000000004C4180004(1)
                                                )
                                        )
                                        .build()
                        )
                )
                .subBatches(
                        CollectionsUtils.listOf(
                                SubBatch.<OrderWithDetails>builder().sortingStation("S02")
                                        .orders(
                                                CollectionsUtils.listOf(
                                                        OrderWithDetailsTestData.owdB000003004(),
                                                        OrderWithDetailsTestData.owdB000003002(),
                                                        OrderWithDetailsTestData.owdB000003001()
                                                )
                                        )
                                        .build()
                        )
                )
                .build();
    }

    static PickingOrderBatch pickingOrderBatch1Split(String batchId, String batchKey) {
        return PickingOrderBatch.builder()
                .batchId(batchId)
                .batchKey(batchKey)
                .pickingOrders(
                        CollectionsUtils.listOf(
                                PickingOrder.builder()
                                        .pickingOrderKey("1")
                                        .type(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                                        .workingArea("MEZONIN_2")
                                        .items(
                                                CollectionsUtils.listOf(
                                                        pickSkuROV0000000000000000003C4110003(1),
                                                        pickSkuROV0000000000000000004C4180004(1)
                                                )
                                        )
                                        .build(),
                                PickingOrder.builder()
                                        .pickingOrderKey("2")
                                        .type(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                                        .workingArea("MEZONIN_2")
                                        .items(
                                                listOf(
                                                        pickSkuROV0000000000000000006C4170006(1)
                                                )
                                        )
                                        .build()
                        )
                )
                .subBatches(
                        CollectionsUtils.listOf(
                                SubBatch.<OrderWithDetails>builder().sortingStation("S02")
                                        .orders(
                                                CollectionsUtils.listOf(
                                                        OrderWithDetailsTestData.owdB000003004(),
                                                        OrderWithDetailsTestData.owdB000003002(),
                                                        OrderWithDetailsTestData.owdB000003001()
                                                )
                                        )
                                        .build()
                        )
                )
                .build();
    }

    static PickingOrderBatch pickingOrderBatch2(String batchId, String batchKey) {
        return PickingOrderBatch.builder()
                .batchId(batchId)
                .batchKey(batchKey)
                .pickingOrders(
                        CollectionsUtils.listOf(
                                PickingOrder.builder()
                                        .pickingOrderKey("2")
                                        .type(AssigmentType.SORTABLE_CONVEYABLE)
                                        .workingArea("MEZONIN_2")
                                        .items(
                                                listOf(
                                                        pickSkuROV0000000000000000008C4160008(1)
                                                )
                                        )
                                        .build()
                        )
                )
                .subBatches(
                        CollectionsUtils.listOf(
                                SubBatch.<OrderWithDetails>builder().sortingStation("S03")
                                        .orders(
                                                CollectionsUtils.listOf(
                                                        OrderWithDetailsTestData.owdB000005002()
                                                )
                                        )
                                        .build()
                        )
                )
                .build();
    }

    static PickingOrderBatch pickingOrderBatch3(String batchId, String batchKey) {
        return PickingOrderBatch.builder()
                .batchId(batchId)
                .batchKey(batchKey)
                .pickingOrders(
                        CollectionsUtils.listOf(
                                PickingOrder.builder()
                                        .pickingOrderKey("3")
                                        .type(AssigmentType.SORTABLE_CONVEYABLE)
                                        .workingArea("FLOOR")
                                        .items(
                                                CollectionsUtils.listOf(
                                                        pickSkuROV0000000000000000001C4100001(1),
                                                        pickSkuROV0000000000000000001C4101001(2),
                                                        pickSkuROV0000000000000000002C4190002(1)
                                                )
                                        )
                                        .build(),
                                PickingOrder.builder()
                                        .pickingOrderKey("4")
                                        .type(AssigmentType.SORTABLE_CONVEYABLE)
                                        .workingArea("MEZONIN_2")
                                        .items(
                                                listOf(
                                                        pickSkuROV0000000000000000009C4140009(1)
                                                )
                                        )
                                        .build()
                        )
                )
                .subBatches(
                        CollectionsUtils.listOf(
                                SubBatch.<OrderWithDetails>builder().sortingStation("S04")
                                        .orders(
                                                CollectionsUtils.listOf(
                                                        OrderWithDetailsTestData.owdB000001003(),
                                                        OrderWithDetailsTestData.owdB000001004(),
                                                        OrderWithDetailsTestData.owdB000002002()
                                                )
                                        )
                                        .build()
                        )
                )
                .build();
    }
}
