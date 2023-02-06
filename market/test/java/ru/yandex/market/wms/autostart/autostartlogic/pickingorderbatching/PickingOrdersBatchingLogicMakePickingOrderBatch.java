package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData;
import ru.yandex.market.wms.autostart.model.dto.AOSZoneSettingsDto;
import ru.yandex.market.wms.autostart.util.dispenser.AccumulatedLimitChecker;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrderBatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class PickingOrdersBatchingLogicMakePickingOrderBatch extends AutostartIntegrationTest {

    static AtomicInteger batchKey = new AtomicInteger();
    static AtomicInteger pickingOrderKey = new AtomicInteger();

    @BeforeEach
    void beforeEach() {
        batchKey.set(1);
        pickingOrderKey.set(1);
    }


    @Test
    void makePickingOrderBatchCheckVolume() {
        makePickingOrderBatch(zone -> PickingOrdersBatchingService.checker(SkuTestData.skuDimensions(), 20d, null));
    }

    @Test
    void makePickingOrderBatchCheckWeight() {
        makePickingOrderBatch(zone -> PickingOrdersBatchingService.checker(SkuTestData.skuDimensions(), null, 20d));
    }


    void makePickingOrderBatch(Function<String, AccumulatedLimitChecker<PickSku>> accumulatedLimitCheckerSupplier) {
        PickingOrderBatch result = logic(accumulatedLimitCheckerSupplier).makePickingOrderBatch(
                OrderBatchTestData.orderBatch1("1"),
                new PickingOrdersPlanner(OrderInventoryDetailTestData.sampleInventory()));
        assertThat(result, is(equalTo(PickingOrderBatchTestData.pickingOrderBatch1Split("1", "1"))));
    }

    PickingOrdersBatchingLogic logic(Function<String,
            AccumulatedLimitChecker<PickSku>> accumulatedLimitCheckerSupplier) {
        return new PickingOrdersBatchingLogic(
                () -> String.valueOf(batchKey.getAndIncrement()),
                () -> String.valueOf(pickingOrderKey.getAndIncrement()),
                accumulatedLimitCheckerSupplier,
                zone -> AOSZoneSettingsDto.builder().itemsIntoPickingOrder(30).build(),
                false,
                0
        );
    }
}
