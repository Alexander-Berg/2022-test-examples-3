package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils;
import ru.yandex.market.wms.autostart.util.dispenser.AccumulatedLimitChecker;
import ru.yandex.market.wms.autostart.util.dispenser.MultiAccumulatedLimitChecker;
import ru.yandex.market.wms.autostart.util.dispenser.SubSequencesDispenser;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class VanillaPickingOrderBatchingLogicTest {

    @Test
    void split__greedily() {
        assertSplit(
                MultiAccumulatedLimitChecker::new, PickSkuTestData2.somePickSkus2(), false, CollectionsUtils.listOf(
                CollectionsUtils.listOf(
                        PickSkuTestData2.pickSkuROV1(7),
                        PickSkuTestData2.pickSkuROV2(3)
                ),
                CollectionsUtils.listOf(
                        PickSkuTestData2.pickSkuROV2(6),
                        PickSkuTestData2.pickSkuROV3(4)
                ),
                CollectionsUtils.listOf(
                        PickSkuTestData2.pickSkuROV3(1)
                )
        ), 10);
    }

    @Test
    void split__uniformly() {
        assertSplit(MultiAccumulatedLimitChecker::new, PickSkuTestData2.somePickSkus2(), true, CollectionsUtils.listOf(
                CollectionsUtils.listOf(
                        PickSkuTestData2.pickSkuROV1(7)
                ),
                CollectionsUtils.listOf(
                        PickSkuTestData2.pickSkuROV2(7)
                ),
                CollectionsUtils.listOf(
                        PickSkuTestData2.pickSkuROV2(2),
                        PickSkuTestData2.pickSkuROV3(5)
                )
        ), 10);
    }

    @Test
    void split__greedily__limit_volume() {

        assertSplit(
                () -> PickingOrdersBatchingService.checker(SkuTestData.skuDimensions(), null, 20d),
                PickSkuTestData.somePickSkus(),
                false,
                CollectionsUtils.listOf(
                        CollectionsUtils.listOf(
                                PickSkuTestData.pickSkuROV0000000000000000003C4110003(1),  // volume 10
                                PickSkuTestData.pickSkuROV0000000000000000004C4180004(1)   // volume 10
                        ),
                        CollectionsUtils.listOf(
                                PickSkuTestData.pickSkuROV0000000000000000006C4170006(1)   // volume 20
                        )
                ), 30);
    }


    private static void assertSplit(
            Supplier<AccumulatedLimitChecker<PickSku>> checkerSupplier,
            List<PickSku> pickSkus,
            boolean uniformly,
            List<List<PickSku>> expected,
            int threshold) {

        List<List<PickSku>> actual = VanillaPickingOrderBatchingLogic.split(uniformly,
                pickSkus,
                threshold,
                pickSkusList -> new SubSequencesDispenser<>(pickSkusList, checkerSupplier));

        assertThat(actual, is(expected));
    }
}
