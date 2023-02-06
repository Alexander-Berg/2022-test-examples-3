package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.AutostrategyShopState;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyShopState;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesMetaProcessorWhiteTest.autostrategy;

abstract class AbstractAutostrategiesShopStateProcessorTest extends AbstractTmsSpringConfiguration {

    private static final long MIN = TimeUnit.MINUTES.toMillis(1);

    private static final long UID = 1;

    private static final int SHOP_ID_1 = 2289;
    private static final int SHOP_ID_2 = 2290;
    private static final int SHOP_ID_3 = 2291;
    private static final int SHOP_ID_4 = 2292;

    @Autowired
    @Qualifier("autostrategiesMetaWhite")
    private AutostrategiesMetaProcessor metaProcessorWhite;

    @Autowired
    @Qualifier("autostrategiesStateWhite")
    private AutostrategiesStateProcessor stateProcessorWhite;

    @Autowired
    @Qualifier("autostrategiesShopStateProcessorWhite")
    private AutostrategiesShopStateProcessor processorWhite;

    @Autowired
    @Qualifier("autostrategiesMetaBlue")
    private AutostrategiesMetaProcessor metaProcessorBlue;

    @Autowired
    @Qualifier("autostrategiesMetaVendorBlue")
    private AutostrategiesMetaProcessor metaProcessorVendorBlue;

    @Autowired
    @Qualifier("autostrategiesStateBlue")
    private AutostrategiesStateProcessor stateProcessorBlue;

    @Autowired
    @Qualifier("autostrategiesStateVendorBlue")
    private AutostrategiesStateProcessor stateProcessorVendorBlue;

    @Autowired
    @Qualifier("autostrategiesShopStateProcessorBlue")
    private AutostrategiesShopStateProcessor processorBlue;

    @Autowired
    @Qualifier("autostrategiesShopStateProcessorVendorBlue")
    private AutostrategiesShopStateProcessor processorVendorBlue;

    private AutostrategiesMetaProcessor metaProcessor;
    private AutostrategiesStateProcessor stateProcessor;
    private AutostrategiesShopStateProcessor processor;
    private YtScenarioExecutor<AutostrategyShopState> autostrategiesShopState;

    private AutostrategyTarget target;

    void init(AutostrategyTarget target) {
        this.target = target;

        this.metaProcessor = target.get(metaProcessorWhite, metaProcessorBlue, metaProcessorVendorBlue);
        this.stateProcessor = target.get(stateProcessorWhite, stateProcessorBlue, stateProcessorVendorBlue);
        this.processor = target.get(processorWhite, processorBlue, processorVendorBlue);
        this.autostrategiesShopState = target.get(
                executors.autostrategiesShopStateWhite(),
                executors.autostrategiesShopStateBlue(),
                executors.autostrategiesShopStateVendorBlue());

        if (target == AutostrategyTarget.white) {
            testControls.initOnce(this.getClass(), () ->
                    testControls.executeInParallel(
                            () -> testControls.saveShop(shop(SHOP_ID_1, s -> s.setStatus(ShopStatus.ACTIVE))),
                            () -> testControls.saveShop(shop(SHOP_ID_2, s -> s.setStatus(ShopStatus.ACTIVE))),
                            () -> testControls.saveShop(shop(SHOP_ID_3, s -> s.setStatus(ShopStatus.INACTIVE))),
                            () -> testControls.saveShop(shop(SHOP_ID_4, s -> s.setStatus(ShopStatus.ACTIVE)))
                    ));
        }

        testControls.executeInParallel(
                () -> AutostrategiesMetaProcessorWhiteTest.cleanupTables(metaProcessor, stateProcessor, testControls),
                () -> autostrategiesShopState.clearTargetTable());
    }

    @Test
    void testNoRecords() {
        assertEquals(Set.of(), processor.collectShopsForProcessing());
    }

    @Test
    void testRecordWithoutShop() {
        var unknownShopId = 1_000_000 + SHOP_ID_1;
        metaProcessor.create(UID, unknownShopId, autostrategy("test1", AutostrategySettings.TypeEnum.DRR));

        TimingUtils.addTime(MIN);

        var expect = target.get(Set.of(), Set.of(unknownShopId));
        assertEquals(expect, processor.collectShopsForProcessing());
    }

    @Test
    void testSingleShopWithoutState_tooEarly() {
        metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        TimingUtils.addTime(-1);
        assertEquals(Set.of(), processor.collectShopsForProcessing());
    }

    @Test
    void testSingleShopWithoutState() {
        metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_1), processor.collectShopsForProcessing());
    }

    @Test
    void testMultipleShopsWithoutState() {
        metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));
        metaProcessor.create(UID, SHOP_ID_2, autostrategy("test2", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_1, SHOP_ID_2), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsWithDeletedAutostrategies() {
        var id1 = metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));
        var id2 = metaProcessor.create(UID, SHOP_ID_2, autostrategy("test2", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        metaProcessor.delete(UID, SHOP_ID_1, id1);
        metaProcessor.delete(UID, SHOP_ID_2, id2);

        TimingUtils.addTime(MIN);
        autostrategiesShopState.insert(List.of(
                autostrategyShopState(SHOP_ID_1, a -> {
                    a.setProcess_start(getInstant());
                    a.setProcess_complete(getInstant());
                    a.setUpdated_at(getInstant());
                })
        ));
        assertEquals(Set.of(SHOP_ID_2), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsWithProcessed1() {
        metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));
        metaProcessor.create(UID, SHOP_ID_2, autostrategy("test2", TypeEnum.DRR));


        TimingUtils.addTime(MIN);
        autostrategiesShopState.insert(List.of(
                autostrategyShopState(SHOP_ID_1, a -> {
                    a.setProcess_start(getInstant());
                    a.setProcess_complete(getInstant());
                    a.setUpdated_at(getInstant());
                })
        ));
        assertEquals(Set.of(SHOP_ID_2), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsWithProcessed1_again() {
        metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));
        metaProcessor.create(UID, SHOP_ID_2, autostrategy("test2", TypeEnum.DRR));

        TimingUtils.addTime(MIN + MIN);
        autostrategiesShopState.insert(List.of(
                autostrategyShopState(SHOP_ID_1, a -> {
                    a.setProcess_start(getInstant());
                    a.setProcess_complete(getInstant());
                    a.setUpdated_at(getInstant());
                })
        ));

        TimingUtils.addTime(MIN * 5);
        assertEquals(Set.of(SHOP_ID_2), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsWithProcessed1_thenUpdate() {
        var id = metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));
        metaProcessor.create(UID, SHOP_ID_2, autostrategy("test2", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        autostrategiesShopState.insert(List.of(
                autostrategyShopState(SHOP_ID_1, a -> {
                    a.setProcess_start(getInstant());
                    a.setProcess_complete(getInstant());
                    a.setUpdated_at(getInstant());
                })
        ));

        TimingUtils.addTime(MIN);
        metaProcessor.update(UID, SHOP_ID_1, id, autostrategy("test1.1", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_1, SHOP_ID_2), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsWithProcessed1_obsoleteForSure() {
        var id = metaProcessor.create(UID, SHOP_ID_1, autostrategy("test1", TypeEnum.DRR));
        metaProcessor.create(UID, SHOP_ID_2, autostrategy("test2", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        autostrategiesShopState.insert(List.of(
                autostrategyShopState(SHOP_ID_1, a -> {
                    a.setProcess_start(getInstant());
                    a.setUpdated_at(getInstant());
                })
        ));

        TimingUtils.addTime(MIN);
        metaProcessor.update(UID, SHOP_ID_1, id, autostrategy("test1.1", TypeEnum.DRR));
        assertEquals(Set.of(SHOP_ID_2), processor.collectShopsForProcessing());

        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_2), processor.collectShopsForProcessing());

        TimingUtils.addTime(MIN + 1);
        assertEquals(Set.of(SHOP_ID_1, SHOP_ID_2), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsInactive() {
        metaProcessor.create(UID, SHOP_ID_3, autostrategy("test1", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_3), processor.collectShopsForProcessing());
    }

    @Test
    void testShopsNotPl2() {
        metaProcessor.create(UID, SHOP_ID_4, autostrategy("test1", TypeEnum.DRR));

        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_4), processor.collectShopsForProcessing());
    }

    @Test
    void testUpdateDuringProcessing() {
        var settings = autostrategy("test1", TypeEnum.DRR);
        var id = metaProcessor.create(UID, SHOP_ID_1, settings);
        TimingUtils.addTime(MIN);
        assertEquals(Set.of(SHOP_ID_1), processor.collectShopsForProcessing());

        var state = autostrategyShopState(SHOP_ID_1, a -> {
            a.setProcess_start(getInstant());
            a.setUpdated_at(getInstant());
        });
        autostrategiesShopState.insert(List.of(state));
        assertEquals(Set.of(), processor.collectShopsForProcessing());

        long sec15 = TimeUnit.SECONDS.toMillis(15);

        // Обновили - ожидаем запуск нового цикла
        TimingUtils.addTime(sec15);
        metaProcessor.update(UID, SHOP_ID_1, id, settings);

        state.setUpdated_at(getInstant());
        autostrategiesShopState.insert(List.of(state));
        assertEquals(Set.of(), processor.collectShopsForProcessing());

        TimingUtils.addTime(sec15);
        state.setProcess_complete(getInstant());
        autostrategiesShopState.insert(List.of(state));
        assertEquals(Set.of(), processor.collectShopsForProcessing());

        TimingUtils.addTime(sec15);
        TimingUtils.addTime(sec15);
        TimingUtils.addTime(sec15);
        assertEquals(Set.of(SHOP_ID_1), processor.collectShopsForProcessing());
    }

}
