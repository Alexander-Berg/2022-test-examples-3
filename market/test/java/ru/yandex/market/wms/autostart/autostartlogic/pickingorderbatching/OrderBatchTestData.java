package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils;
import ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData;
import ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData;
import ru.yandex.market.wms.common.spring.dao.entity.Batch;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;

public interface OrderBatchTestData {

    static Batch<OrderWithDetails> orderBatch0(String id) {
        return Batch.<OrderWithDetails>builder().id(id).subBatches(
                CollectionsUtils.listOf(
                        SubBatch.<OrderWithDetails>builder()
                                .sortingStation(SortingStationTestData.DOOR_S_01)
                                .orders(CollectionsUtils.listOf(
                                        OrderWithDetailsTestData.owdB000003003(),
                                        OrderWithDetailsTestData.owdB000004001()
                                ))
                                .build()
                )
        ).build();
    }

    static Batch<OrderWithDetails> orderBatch1(String id) {
        return Batch.<OrderWithDetails>builder().id(id).subBatches(
                CollectionsUtils.listOf(
                        SubBatch.<OrderWithDetails>builder()
                                .sortingStation(SortingStationTestData.DOOR_S_02)
                                .orders(CollectionsUtils.listOf(
                                        OrderWithDetailsTestData.owdB000003004(),
                                        OrderWithDetailsTestData.owdB000003002(),
                                        OrderWithDetailsTestData.owdB000003001()
                                ))
                                .build()
                )
        ).build();
    }

    static Batch<OrderWithDetails> orderBatch2(String id) {
        return Batch.<OrderWithDetails>builder().id(id).subBatches(
                CollectionsUtils.listOf(
                        SubBatch.<OrderWithDetails>builder()
                                .sortingStation(SortingStationTestData.DOOR_S_03)
                                .orders(CollectionsUtils.listOf(
                                        OrderWithDetailsTestData.owdB000005002()
                                ))
                                .build()
                )
        ).build();
    }

    static Batch<OrderWithDetails> orderBatch3(String id) {
        return Batch.<OrderWithDetails>builder().id(id).subBatches(
                CollectionsUtils.listOf(
                        SubBatch.<OrderWithDetails>builder()
                                .sortingStation(SortingStationTestData.DOOR_S_04)
                                .orders(CollectionsUtils.listOf(
                                        OrderWithDetailsTestData.owdB000001003(),
                                        OrderWithDetailsTestData.owdB000001004(),
                                        OrderWithDetailsTestData.owdB000002002()
                                ))
                                .build()
                )
        ).build();
    }
}
