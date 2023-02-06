package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterVendor;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyMap;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPO;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsDRR;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsPOS;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsVPOS;
import ru.yandex.market.pricelabs.misc.PricelabsRuntimeException;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyHistory;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.FilterHistory;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.AutostrategyType;
import ru.yandex.market.pricelabs.model.types.FilterType;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategyBlock;
import ru.yandex.market.pricelabs.services.database.SequenceService;
import ru.yandex.market.pricelabs.services.database.SequenceServiceImpl;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.TestControls;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.yt.binding.ProcessorCfg;
import ru.yandex.market.yt.exception.RetryOperationException;
import ru.yandex.misc.random.Random2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter.TypeEnum.SIMPLE;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter.TypeEnum.VENDOR;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPA;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPO;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.DRR;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.POS;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.VPOS;

public abstract class AbstractAutostrategiesMetaProcessorTest extends AbstractTmsSpringConfiguration {
    static final int UID = 100;
    static final int SHOP1 = 1;
    static final int SHOP2 = 2;

    @Autowired
    @Qualifier("autostrategiesMetaWhite")
    private AutostrategiesMetaProcessor metaWhite;

    @Autowired
    @Qualifier("autostrategiesMetaBlue")
    private AutostrategiesMetaProcessor metaBlue;

    @Autowired
    @Qualifier("autostrategiesMetaVendorBlue")
    private AutostrategiesMetaProcessor metaVendorBlue;

    @Autowired
    @Qualifier("autostrategiesStateWhite")
    private AutostrategiesStateProcessor stateWhite;

    @Autowired
    @Qualifier("autostrategiesStateBlue")
    private AutostrategiesStateProcessor stateBlue;

    @Autowired
    @Qualifier("autostrategiesStateVendorBlue")
    private AutostrategiesStateProcessor stateVendorBlue;

    private AutostrategyTarget target;

    protected AutostrategiesMetaProcessor metaProcessor;

    @Autowired
    private SequenceService sequenceService;

    void init(@NonNull AutostrategyTarget target) {
        this.target = target;

        this.metaProcessor = target.get(metaWhite, metaBlue, metaVendorBlue);
        var stateProcessor = target.get(stateWhite, stateBlue, stateVendorBlue);

        cleanupTables(metaProcessor, stateProcessor, testControls);
        ((SequenceServiceImpl) sequenceService).resetSequences();
    }

    @Test
    void create() {
        int id1 = metaProcessor.create(UID, SHOP1, autostrategy("test-1", CPO));
        int id2 = metaProcessor.create(UID, SHOP1, autostrategy("test-2", DRR));
        int id3 = metaProcessor.create(UID, SHOP1, autostrategy("test-3", POS));
        int id4 = metaProcessor.create(UID, SHOP1, autostrategy("test-4", VPOS));
        int id5 = metaProcessor.create(UID, SHOP1, autostrategy("test-5", VENDOR, VPOS));
        int id6 = metaProcessor.create(UID, SHOP1, autostrategy("test-6", CPA));

        var b1 = metaProcessor.load(SHOP1, id1);
        assertEquals(block(b1, "test-1", CPO), b1);

        var b2 = metaProcessor.load(SHOP1, id2);
        assertEquals(block(b2, "test-2", DRR), b2);

        var b3 = metaProcessor.load(SHOP1, id3);
        assertEquals(block(b3, "test-3", POS), b3);

        var b4 = metaProcessor.load(SHOP1, id4);
        assertEquals(block(b4, "test-4", VPOS), b4);

        var b5 = metaProcessor.load(SHOP1, id5);
        assertEquals(block(b5, "test-5", VENDOR, VPOS), b5);

        var b6 = metaProcessor.load(SHOP1, id6);
        assertEquals(block(b6, "test-6", CPA), b6);
    }

    @Test
    void createLoadDifferentShop() {
        int id1 = metaProcessor.create(UID, SHOP1, autostrategy("test-1", CPO));
        assertThrows(RetryOperationException.class, () -> metaProcessor.load(2, id1));
    }

    @Test
    void createInParallel() {
        testControls.executeInParallel(this::create, this::create, this::create, this::create, this::create);
    }

    @Test
    void update() {
        var auto = autostrategy("test-1", CPO);
        int id = metaProcessor.create(UID, SHOP1, auto);

        var b1 = metaProcessor.load(SHOP1, id);
        var expect = block(b1, "test-1", CPO);
        assertEquals(expect, b1);

        var expectAuto = expect.getAuto();

        auto.setName("test-2");
        auto.getSettings().getCpo().setCpo(333L);
        metaProcessor.update(UID, SHOP1, id, auto);

        // Проверим, что настройки изменились
        var b11 = metaProcessor.load(SHOP1, id);
        expectAuto.setLast_change_id(b11.getAuto().getLast_change_id());
        expectAuto.setName("test-2");
        expectAuto.getCpoSettings().setCpo(333);
        expect.getFilter().setName("test-2");
        assertEquals(expect, metaProcessor.load(SHOP1, id));
    }

    @Test
    void updateInParallel() {
        testControls.executeInParallel(this::update, this::update, this::update, this::update, this::update);
    }

    @Test
    void updateMultiple() {
        var auto = autostrategy("test-1", CPO);
        int id = metaProcessor.create(UID, SHOP1, auto);

        var b1 = metaProcessor.load(SHOP1, id);
        var expect = block(b1, "test-1", CPO);
        assertEquals(expect, b1);
        var expectAuto = expect.getAuto();
        var expectFilter = expect.getFilter();

        auto.setName("test-2");
        metaProcessor.update(UID, SHOP1, id, auto);

        expectAuto.setLast_change_id(expectAuto.getLast_change_id() + 1);
        expectAuto.setName("test-2");
        expect.getFilter().setName("test-2");
        assertEquals(expect, metaProcessor.load(SHOP1, id));

        auto.getFilter().getSimple().setPriceFrom(420L);
        metaProcessor.update(UID, SHOP1, id, auto);

        expectAuto.setLast_change_id(expectAuto.getLast_change_id() + 1);
        expect.getFilter().setPrice_from(420L);
        assertEquals(expect, metaProcessor.load(SHOP1, id));

        auto.setEnabled(false);
        metaProcessor.update(UID, SHOP1, id, auto);

        expectAuto.setLast_change_id(expectAuto.getLast_change_id() + 1);
        expectAuto.setEnabled(false);
        assertEquals(expect, metaProcessor.load(SHOP1, id));


        auto.getFilter().getSimple()
                .vendors(List.of("v1", "v2", "v1", "v3", "v3"))
                .categories(List.of(4L, 4L, 5L, 5L, 0L))
                .excludeCategories(List.of(9L, 0L, 9L, 1L));
        metaProcessor.update(UID, SHOP1, id, auto);

        expectAuto.setLast_change_id(expectAuto.getLast_change_id() + 1);
        expectFilter.setVendors(Utils.stableSet("v1", "v2", "v3"));
        expectFilter.setCategories_by_id(Utils.stableSet(4L, 5L, 0L));
        expectFilter.setExclude_categories_by_id(Utils.stableSet(9L, 0L, 1L));
        assertEquals(expect, metaProcessor.load(SHOP1, id));
    }

    @Test
    void updateDifferentShop() {
        var auto = autostrategy("test-1", CPO);
        int id = metaProcessor.create(UID, SHOP1, auto);

        TimingUtils.addTime(1);
        assertThrows(RetryOperationException.class, () -> metaProcessor.update(UID, SHOP2, id, auto));
    }

    @Test
    void delete() {
        var auto = autostrategy("test-1", CPO);
        int id = metaProcessor.create(UID, SHOP1, auto);

        metaProcessor.delete(UID, SHOP1, id);
    }


    @Test
    void updateList() {
        var ids = metaProcessor.updateList(UID, SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(autostrategy("test-1", CPO)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-2", DRR)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-3", POS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-4", VPOS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-5", VENDOR, VPOS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-6", CPA))
        ));

        var id1 = ids.get(0);
        var id2 = ids.get(1);
        var id3 = ids.get(2);
        var id4 = ids.get(3);
        var id5 = ids.get(4);
        var id6 = ids.get(5);

        var b1 = metaProcessor.load(SHOP1, id1);
        assertEquals(block(b1, "test-1", CPO), b1);

        var b2 = metaProcessor.load(SHOP1, id2);
        assertEquals(block(b2, "test-2", DRR), b2);

        var b3 = metaProcessor.load(SHOP1, id3);
        assertEquals(block(b3, "test-3", POS), b3);

        var b4 = metaProcessor.load(SHOP1, id4);
        assertEquals(block(b4, "test-4", VPOS), b4);

        var b5 = metaProcessor.load(SHOP1, id5);
        assertEquals(block(b5, "test-5", VENDOR, VPOS), b5);

        var b6 = metaProcessor.load(SHOP1, id6);
        assertEquals(block(b6, "test-6", CPA), b6);
    }

    @Test
    void deleteList() {
        var ids = metaProcessor.updateList(UID, SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(autostrategy("test-1", CPO)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-2", DRR)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-3", POS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-4", VPOS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-5", VENDOR, VPOS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-6", CPA))
        ));

        var id1 = ids.get(0);
        var id2 = ids.get(1);
        var id3 = ids.get(2);
        var id4 = ids.get(3);
        var id5 = ids.get(4);
        var id6 = ids.get(5);

        metaProcessor.deleteList(UID, SHOP1, List.of(id2, id3));

        var b1 = metaProcessor.load(SHOP1, id1);
        assertEquals(block(b1, "test-1", CPO), b1);

        assertThrows(RetryOperationException.class, () -> metaProcessor.load(SHOP1, id2));
        assertThrows(RetryOperationException.class, () -> metaProcessor.load(SHOP1, id3));

        var b4 = metaProcessor.load(SHOP1, id4);
        assertEquals(block(b4, "test-4", VPOS), b4);

        var b5 = metaProcessor.load(SHOP1, id5);
        assertEquals(block(b5, "test-5", VENDOR, VPOS), b5);

        var b6 = metaProcessor.load(SHOP1, id6);
        assertEquals(block(b6, "test-6", CPA), b6);

        // Никаких ошибок не будет при повторном удалении несуществующих записей
        metaProcessor.deleteList(UID, SHOP1, List.of(id2, id3));
    }

    @Test
    void updateAndCreateList() {
        var ids = metaProcessor.updateList(UID, SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(autostrategy("test-1", CPO)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-4", VPOS)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-5", VENDOR, VPOS))));
        assertEquals(3, ids.size());

        var id1 = ids.get(0);
        var id4 = ids.get(1);
        var id5 = ids.get(2);

        var b1 = metaProcessor.load(SHOP1, id1);
        assertEquals(block(b1, "test-1", CPO), b1);

        var b4 = metaProcessor.load(SHOP1, id4);
        assertEquals(block(b4, "test-4", VPOS), b4);

        var b5 = metaProcessor.load(SHOP1, id5);
        assertEquals(block(b5, "test-5", VENDOR, VPOS), b5);

        var ids2 = metaProcessor.updateList(UID, SHOP1, List.of(
                new AutostrategySaveWithId().id(id1).autostrategy(autostrategy("test-1x", CPO)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-2", DRR)),
                new AutostrategySaveWithId().id(999665).autostrategy(autostrategy("test-2-unknown", DRR)),
                new AutostrategySaveWithId().autostrategy(autostrategy("test-3", POS)),
                new AutostrategySaveWithId().id(999666).autostrategy(autostrategy("test-3-unknown", DRR)),
                new AutostrategySaveWithId().id(id4).autostrategy(autostrategy("test-4x", VPOS)),
                new AutostrategySaveWithId().id(id5).autostrategy(autostrategy("test-5x", VENDOR, VPOS))));
        assertEquals(7, ids2.size());

        var id21 = ids2.get(0);
        assertEquals(id21, id1);

        var id22 = ids2.get(1);
        assertNull(ids2.get(2));
        var id23 = ids2.get(3);
        assertNull(ids2.get(4));
        var id24 = ids2.get(5);
        assertEquals(id24, id4);
        var id25 = ids2.get(6);
        assertEquals(id25, id5);

        var b21 = metaProcessor.load(SHOP1, id21);
        assertEquals(block(b21, "test-1x", CPO), b21);

        var b22 = metaProcessor.load(SHOP1, id22);
        assertEquals(block(b22, "test-2", DRR), b22);

        var b23 = metaProcessor.load(SHOP1, id23);
        assertEquals(block(b23, "test-3", POS), b23);

        var b24 = metaProcessor.load(SHOP1, id24);
        assertEquals(block(b24, "test-4x", VPOS), b24);

        var b25 = metaProcessor.load(SHOP1, id25);
        assertEquals(block(b25, "test-5x", VENDOR, VPOS), b25);
    }

    @Test
    void deleteInParallel() {
        testControls.executeInParallel(this::delete, this::delete, this::delete, this::delete, this::delete);
    }

    @Test
    void mixedInParallel() {
        testControls.executeInParallel(
                this::create, this::update, this::delete,
                this::create, this::update, this::delete,
                this::create, this::update, this::delete,
                this::create, this::update, this::delete,
                this::create, this::update, this::delete);
    }

    // Точно не должно быть повторов
    @Timeout(5)
    @Test
    void deleteUnknown() {
        assertThrows(PricelabsRuntimeException.class, () -> metaProcessor.delete(UID, SHOP1, 1));
    }

    @Test
    void deleteDifferentShop() {
        var auto = autostrategy("test-1", CPO);
        int id = metaProcessor.create(UID, SHOP1, auto);

        assertThrows(PricelabsRuntimeException.class, () -> metaProcessor.delete(UID, SHOP2, id));
    }

    @Test
    void reorder() {

        int id1 = metaProcessor.create(UID, SHOP1, autostrategy("test-1", CPO));
        int id2 = metaProcessor.create(UID, SHOP1, autostrategy("test-2", DRR));
        int id3 = metaProcessor.create(UID, SHOP1, autostrategy("test-3", POS));
        int id4 = metaProcessor.create(UID, SHOP1, autostrategy("test-4", VPOS));
        int id5 = metaProcessor.create(UID, SHOP1, autostrategy("test-5", VENDOR, VPOS));
        int id6 = metaProcessor.create(UID, SHOP1, autostrategy("test-6", CPA));

        var r = Random2.R;
        var p1 = r.nextInt(1000);
        var p2 = r.nextInt(1000);
        var p3 = r.nextInt(1000);
        var p4 = r.nextInt(1000);
        var p5 = r.nextInt(1000);
        var p6 = r.nextInt(1000);
        metaProcessor.reorder(UID, SHOP1, reorder(Map.of(id1, p1, id2, p2, id3, p3, id4, p4, id5, p5, id6, p6)));

        assertEquals(p1, metaProcessor.load(SHOP1, id1).getAuto().getPriority());
        assertEquals(p2, metaProcessor.load(SHOP1, id2).getAuto().getPriority());
        assertEquals(p3, metaProcessor.load(SHOP1, id3).getAuto().getPriority());
        assertEquals(p4, metaProcessor.load(SHOP1, id4).getAuto().getPriority());
        assertEquals(p5, metaProcessor.load(SHOP1, id5).getAuto().getPriority());
        assertEquals(p6, metaProcessor.load(SHOP1, id6).getAuto().getPriority());
    }

    @Test
    void reorderInParallel() {
        testControls.executeInParallel(this::reorder, this::reorder, this::reorder, this::reorder, this::reorder);
    }

    @Test
    void changeState() {
        int id1 = metaProcessor.create(UID, SHOP1, autostrategy("test-1", CPO));
        int id2 = metaProcessor.create(UID, SHOP1, autostrategy("test-2", DRR));
        int id3 = metaProcessor.create(UID, SHOP1, autostrategy("test-3", POS));
        int id4 = metaProcessor.create(UID, SHOP1, autostrategy("test-4", CPA));

        metaProcessor.changeState(UID, SHOP1, List.of(id2, id3), false);
        var b1 = metaProcessor.load(SHOP1, id1);
        assertEquals(block(b1, "test-1", CPO), b1);

        var b2 = metaProcessor.load(SHOP1, id2);
        assertEquals(changeState(block(b2, "test-2", DRR), false), b2);

        var b3 = metaProcessor.load(SHOP1, id3);
        assertEquals(changeState(block(b3, "test-3", POS), false), b3);

        var b4 = metaProcessor.load(SHOP1, id4);
        assertEquals(block(b4, "test-4", CPA), b4);

        //
        metaProcessor.changeState(UID, SHOP1, List.of(id1, id2, id4), true);
        var b21 = metaProcessor.load(SHOP1, id1);
        assertEquals(block(b21, "test-1", CPO), b21);

        var b22 = metaProcessor.load(SHOP1, id2);
        assertEquals(block(b22, "test-2", DRR), b22);

        var b24 = metaProcessor.load(SHOP1, id4);
        assertEquals(block(b24, "test-4", CPA), b24);

        var b23 = metaProcessor.load(SHOP1, id3);
        assertEquals(changeState(block(b23, "test-3", POS), false), b23);

        metaProcessor.changeState(UID, SHOP1, List.of(id1, 100500), false);
        var b31 = metaProcessor.load(SHOP1, id1);
        assertEquals(changeState(block(b31, "test-1", CPO), false), b31);

        var b32 = metaProcessor.load(SHOP1, id2);
        assertEquals(block(b32, "test-2", DRR), b32);

        var b33 = metaProcessor.load(SHOP1, id3);
        assertEquals(changeState(block(b33, "test-3", POS), false), b33);
    }

    public static List<AutostrategyMap> reorder(Map<Integer, Integer> keyOrder) {
        return keyOrder.entrySet().stream()
                .map(e -> new AutostrategyMap().id(e.getKey()).priority(e.getValue()))
                .collect(Collectors.toList());
    }

    private AutostrategyBlock block(AutostrategyBlock block, String name, AutostrategySettings.TypeEnum type) {
        return block(block, name, SIMPLE, type);
    }

    protected AutostrategyBlock block(AutostrategyBlock block, String name,
                                      AutostrategyFilter.TypeEnum filterType,
                                      AutostrategySettings.TypeEnum strategyType) {
        var fromAuto = block.getAuto();

        var auto = new AutostrategyHistory();
        auto.setShop_id(SHOP1);
        auto.setAutostrategy_id(fromAuto.getAutostrategy_id());
        auto.setName(name);
        auto.setPriority(fromAuto.getAutostrategy_id());
        auto.setLast_change_id(fromAuto.getLast_change_id());
        auto.setUpdated_at(getInstant());
        auto.setUser_id(UID);
        auto.setEnabled(true);
        auto.setFilter_id(fromAuto.getFilter_id());
        switch (strategyType) {
            case CPO:
                auto.setType(AutostrategyType.CPO);
                auto.getCpoSettings().setCpo(100);
                break;
            case DRR:
                auto.setType(AutostrategyType.DRR);
                auto.getDrrSettings().setTake_rate(300);
                break;
            case POS:
                auto.setType(AutostrategyType.POS);
                auto.getPosSettings().setMax_bid(200);
                auto.getPosSettings().setPosition(1);
                break;
            case VPOS:
                auto.setType(AutostrategyType.VPOS);
                auto.getVposSettings().setMax_bid(400);
                auto.getVposSettings().setPosition(1);
                break;
            case CPA:
                auto.setType(AutostrategyType.CPA);
                auto.getCpaSettings().setDrr_bid(500L);
                break;
            default:
                throw new IllegalStateException("Unsupported strategy type: " + strategyType);
        }

        var filter = new FilterHistory();
        filter.setShop_id(SHOP1);
        filter.setFilter_id(auto.getFilter_id());
        filter.setCreated_at(getInstant());
        filter.setUpdated_at(getInstant());
        filter.setImported_at(Instant.ofEpochMilli(0));

        switch (filterType) {
            case SIMPLE:
                auto.setFilter_type(FilterType.SIMPLE);
                filter.setCategories_by_id(Utils.stableSet(1L, 2L));
                filter.setExclude_categories_by_id(Utils.stableSet(3L, 4L));
                filter.setVendors(Utils.stableSet("v1", "v2"));
                filter.setPrice_from(100);
                filter.setPrice_to(200);
                filter.setModels(Set.of());
                filter.setShops(Set.of());
                filter.setBusinesses(Set.of());
                break;
            case VENDOR:
                auto.setFilter_type(FilterType.VENDOR);
                filter.setModels(Utils.stableSet(5, 6));
                filter.setShops(Utils.stableSet(7, 8));
                filter.setBusinesses(Set.of());
                filter.setCategories_by_id(Set.of());
                filter.setExclude_categories_by_id(Set.of());
                filter.setVendors(Set.of());
                break;
            default:
                throw new IllegalStateException("Unsupported filter type: " + filterType);
        }
        filter.setName(name);
        filter.setFilter_class(target.getFilterClass());

        filter.setDescription("");
        filter.setQuery("");
        filter.setCategory("");
        filter.setId_list(Set.of());
        filter.setHash("");
        filter.setParams(Map.of());
        filter.setAutostrategies_by_id(Set.of());
        filter.setShard("");
        filter.setMarket_vendors(Set.of());

        // Пересохраним наш фильтр
        return new AutostrategyBlock(auto, YTreeDeepCopier.deepCopyOf(filter));
    }

    private AutostrategyBlock changeState(AutostrategyBlock block, boolean enabled) {
        block.getAuto().setEnabled(enabled);
        return block;
    }

    public static AutostrategySave autostrategy(String name, AutostrategySettings.TypeEnum type) {
        return autostrategy(name, SIMPLE, type);
    }

    public static AutostrategySave autostrategy(String name, AutostrategyFilter.TypeEnum filterType,
                                                AutostrategySettings.TypeEnum settingsType) {
        return new AutostrategySave()
                .name(name)
                .enabled(true)
                .recommendationType(AutostrategySave.RecommendationTypeEnum.DEFAULT)
                .filter(new AutostrategyFilter()
                        .type(filterType)
                        .simple(filterType == SIMPLE ? new AutostrategyFilterSimple()
                                .categories(List.of(1L, 2L))
                                .excludeCategories(List.of(3L, 4L))
                                .vendors(List.of("v1", "v2"))
                                .priceFrom(100L)
                                .priceTo(200L)
                                .offerIds(List.of()) : null)
                        .vendor(filterType == VENDOR ? new AutostrategyFilterVendor()
                                .models(List.of(5, 6))
                                .shops(List.of(7, 8)).businesses(List.of()) : null))

                .settings(new AutostrategySettings()
                        .type(settingsType)
                        .cpo(settingsType == CPO ?
                                new AutostrategySettingsCPO().cpo(100L) : null)
                        .drr(settingsType == DRR ?
                                new AutostrategySettingsDRR().takeRate(300L) : null)
                        .pos(settingsType == POS ?
                                new AutostrategySettingsPOS().position(1).maxBid(200L) : null)
                        .vpos(settingsType == VPOS ?
                                new AutostrategySettingsVPOS().position(1).maxBid(400L) : null)
                        .cpa(settingsType == CPA ?
                                new AutostrategySettingsCPA().drrBid(500L) : null));
    }

    public static void cleanupTables(AutostrategiesMetaProcessor metaProcessor,
                                     AutostrategiesStateProcessor stateProcessor,
                                     TestControls testControls) {
        cleanupTables(metaProcessor.getCfg(), metaProcessor.getHistoryCfg(),
                stateProcessor.getCfg(), stateProcessor.getHistoryCfg(),
                metaProcessor.getFilterCfg(), metaProcessor.getFilterHistoryCfg(),
                testControls);
    }

    public static void cleanupTables(ProcessorCfg<Autostrategy> cfg,
                                     ProcessorCfg<AutostrategyHistory> historyCfg,
                                     ProcessorCfg<AutostrategyState> stateCfg,
                                     ProcessorCfg<AutostrategyStateHistory> stateHistoryCfg,
                                     ProcessorCfg<Filter> filterCfg,
                                     ProcessorCfg<FilterHistory> filterHistoryCfg,
                                     TestControls testControls) {
        testControls.executeInParallel(
                () -> YtScenarioExecutor.clearTable(cfg),
                () -> YtScenarioExecutor.clearTable(stateCfg),
                () -> YtScenarioExecutor.clearTable(stateHistoryCfg),
                () -> YtScenarioExecutor.clearTable(historyCfg),
                () -> YtScenarioExecutor.clearTable(filterCfg),
                () -> YtScenarioExecutor.clearTable(filterHistoryCfg));
    }


}
