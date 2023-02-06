package ru.yandex.market.pricelabs.tms.processing.events;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.misc.AutostrategiesEventsUtils;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.events.AutostrategyEventInTable;
import ru.yandex.market.pricelabs.model.events.AutostrategyPayload;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.events.AutostrategyEventType;
import ru.yandex.market.pricelabs.processing.CoreTables;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.events.EventsGenerator;
import ru.yandex.market.pricelabs.yt.YtConfiguration;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPO;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.autostrategy;

//TODO разделить на два
@Slf4j
public abstract class AbstractShopWithNoAutostrategiesEventsTests extends AbstractTmsSpringConfiguration {


    static final int SHOP_1 = 11041;
    static final int SHOP_2 = 12041;
    static final int SHOP_3 = 13041;

    private static final long UID = 100;

    @Autowired
    private YtConfiguration ytCfg;

    @Autowired
    private CoreTables coreTables;

    @Autowired
    private YtClientProxy ytClient;

    private EventsGenerator turnedOffEventsGenerator;
    private EventsGenerator deletedEventsGenerator;
    private AutostrategiesMetaProcessor metaProcessor;

    protected void init(EventsGenerator turnedOffAutostrategiesEventsGenerator,
                        EventsGenerator deletedAutostrategiesEventsGereator,
                        AutostrategiesMetaProcessor metaProcessor, AutostrategyTarget target) {
        //Cleanup all tables
        cleanUpTables(coreTables);
        //

        this.turnedOffEventsGenerator = turnedOffAutostrategiesEventsGenerator;
        this.deletedEventsGenerator = deletedAutostrategiesEventsGereator;
        this.metaProcessor = metaProcessor;

        var saveOffers = getSaveOffersRunnable(
                target.get(false, true),
                target.get(executors.offers(), executors.offersBlue()));

        testControls.executeInParallel(
                saveOffers);
    }

    @AfterEach
    void afterEach() {
        executors.offers().clearTargetTable();
        executors.offersBlue().clearTargetTable();
    }

    @Test
    public void testCreateEventsForShopsWithNoActiveAutostrategiesMultipleRuns() {
        createAndDeleteAutostrategy(metaProcessor, SHOP_1, true);
        createAutostrategies(metaProcessor, SHOP_1, false);
        createAutostrategies(metaProcessor, SHOP_2, true);
        createAndDeleteAutostrategy(metaProcessor, SHOP_3, true);
        createAutostrategies(metaProcessor, SHOP_3, false);
        //Первый день
        var result = generateAndGetEvents(turnedOffEventsGenerator);
        AutostrategyEventInTable expectedEvent = getAutostrategyEventInTable(SHOP_1);
        assertEquals(1, result.size());
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(0), expectedEvent);

        //Второй день. Ожидается что при таком коротком временном промежутке вернётся пустой список
        TimingUtils.addTime(1000 * 60 * 60 * 23);
        var result2 = generateAndGetEvents(turnedOffEventsGenerator);
        assertEquals(0, result2.size());

        //Имитируется поведение, при котором автостратегию включили, а потом выключили через несколько дней.
        TimingUtils.addTime(1000 * 60 * 60 * 25 * 3);
        var result3 = generateAndGetEvents(turnedOffEventsGenerator);
        assertEquals(1, result3.size());
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result3.get(0), expectedEvent);
    }

    @Test
    public void testWhenSeveralShopsHasDissabledAutostrategies() {
        createAndDeleteAutostrategy(metaProcessor, SHOP_1, true);
        createAutostrategies(metaProcessor, SHOP_1, false);
        createAndDeleteAutostrategy(metaProcessor, SHOP_2, true);
        createAutostrategies(metaProcessor, SHOP_2, false);
        var result = generateAndGetEvents(turnedOffEventsGenerator);
        assertEquals(2, result.size());
        result.sort(Comparator.comparingInt(a -> (int) a.getShop_id()));

        AutostrategyEventInTable expectedEvent1 = getAutostrategyEventInTable(SHOP_1);
        AutostrategyEventInTable expectedEvent2 = getAutostrategyEventInTable(SHOP_2);

        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(0), expectedEvent1);
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(1), expectedEvent2);
    }

    @Test
    public void testWhenSeveralShopsHasDissabledButHadNoActiveAutostrategyAutostrategies() {
        createAndDeleteAutostrategy(metaProcessor, SHOP_1, true);
        createAutostrategies(metaProcessor, SHOP_1, false);
        createAutostrategies(metaProcessor, SHOP_2, false);
        var result = generateAndGetEvents(turnedOffEventsGenerator);
        assertEquals(1, result.size());

        AutostrategyEventInTable expectedEvent = getAutostrategyEventInTable(SHOP_1);
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(0), expectedEvent);
    }

    @Test
    public void testWhenShopsWithDisabledAndDeletedAutostrategies() {
        Instant dateFrom = TimingUtils.getInstant();
        createAndDeleteAutostrategy(metaProcessor, SHOP_1, true);
        createAndDeleteAutostrategy(metaProcessor, SHOP_2, true);
        createAutostrategies(metaProcessor, SHOP_2, false);
        generateAndGetEvents(turnedOffEventsGenerator);
        deletedEventsGenerator.generateAndSaveEvents();
        var result = getGeneratedEvents(dateFrom);
        assertEquals(2, result.size());
        result.sort(Comparator.comparingInt(a -> (int) a.getShop_id()));

        AutostrategyEventInTable expectedEvent1 = getAutostrategyEventInTable(SHOP_1);
        AutostrategyEventInTable expectedEvent2 = getAutostrategyEventInTable(SHOP_2);

        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(0), expectedEvent1);
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(1), expectedEvent2);
    }

    @Test
    public void whenShopHasNoAutostrategyButHadActiveMustCreateEventTest() {
        createAndDeleteAutostrategy(metaProcessor, SHOP_1, false);
        createAndDeleteAutostrategy(metaProcessor, SHOP_2, true);
        createAutostrategies(metaProcessor, SHOP_3, false);
        var result = generateAndGetEvents(deletedEventsGenerator);
        AutostrategyEventInTable expectedEvent = getAutostrategyEventInTable(SHOP_2);
        assertEquals(1, result.size());
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(0), expectedEvent);
    }

    @Test
    public void testWhenShopHasNoAutostrategyMultipleRuns() {
        createAndDeleteAutostrategy(metaProcessor, SHOP_2, true);
        var result = generateAndGetEvents(deletedEventsGenerator);
        AutostrategyEventInTable expectedEvent = getAutostrategyEventInTable(SHOP_2);
        assertEquals(1, result.size());
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result.get(0), expectedEvent);

        //Второй день. Ожидается что при таком коротком временном промежутке вернётся пустой список
        TimingUtils.addTime(1000 * 60 * 60 * 23);
        var result2 = generateAndGetEvents(deletedEventsGenerator);
        assertEquals(0, result2.size());

        //Имитируется поведение, при котором автостратегию включили, а потом выключили через несколько дней.
        TimingUtils.addTime(1000 * 60 * 60 * 25 * 3);
        var result3 = generateAndGetEvents(deletedEventsGenerator);
        assertEquals(1, result3.size());
        AutostrategiesEventsUtils.compareAutostrategiesEvents(result3.get(0), expectedEvent);
    }

    private void createAutostrategies(AutostrategiesMetaProcessor metaProcessor, Integer shop,
                                      boolean secondEnabled) {
        var s1a1 = autostrategy("s1" + shop.toString(), CPO);
        s1a1.enabled(false);
        var s1a2 = autostrategy("s2" + shop, CPO);
        s1a2.enabled(secondEnabled);
        metaProcessor.create(UID, shop, s1a1);
        metaProcessor.create(UID, shop, s1a2);
    }

    private void createAndDeleteAutostrategy(AutostrategiesMetaProcessor metaProcessor, Integer shop, boolean enabled) {
        var autostrategy = autostrategy("sd1" + shop.toString(), CPO);
        autostrategy.enabled(enabled);
        var id = metaProcessor.create(UID, shop, autostrategy);
        metaProcessor.delete(UID, shop, id);
    }


    private void cleanUpTables(CoreTables coreTables) {
        testControls.executeInParallel(
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getBlueAutostrategiesTable())),

                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesHistoryTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getBlueAutostrategiesHistoryTable())),

                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesStateTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getBlueAutostrategiesStateTable())),

                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesStateHistoryTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getBlueAutostrategiesStateHistoryTable())),

                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getFiltersTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getFiltersHistoryTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesEventTable())),
                () -> YtScenarioExecutor.
                        clearTable(ytCfg.getProcessorCfg(coreTables.getShopWithNoAutostrategiesInstanceTable()))
        );
    }

    Runnable getSaveOffersRunnable(boolean sskuOffer,
                                   YtScenarioExecutor<Offer> executor) {
        return () -> {
            var offers = List.of(offer("0", o -> {
                        o.setShop_id(SHOP_1);
                        o.setFeed_id(2);
                        o.setStatus(Status.ACTIVE);
                        o.setName("name1");
                        o.setPrice(999);
                        o.setModel_id(1001);
                        o.setApp_autostrategy_id(3);
                        o.setSsku_offer(sskuOffer);
                        o.setCategory_id(2141);
                    }),
                    offer("1", o -> {
                        o.setShop_id(SHOP_2);
                        o.setFeed_id(2);
                        o.setStatus(Status.ACTIVE);
                        o.setName("name2");
                        o.setPrice(998);
                        o.setModel_id(1002);
                        o.setApp_autostrategy_id(3);
                        o.setSsku_offer(sskuOffer);
                        o.setCategory_id(2142);
                    }));
            executor.insert(offers);
        };
    }

    private List<AutostrategyEventInTable> generateAndGetEvents(EventsGenerator eventsGenerator) {
        eventsGenerator.generateAndSaveEvents();
        //
        return getGeneratedEvents(TimingUtils.getInstant());
    }

    private List<AutostrategyEventInTable> getGeneratedEvents(Instant timeFrom) {
        var table = coreTables.getAutostrategiesEventTable();
        String query = "* from [" + table.getTable() + "] where event_type=7 and timestamp>=" + timeFrom.toEpochMilli();
        var rows = ytClient.selectRows(query, table.getBinder());
        rows.forEach(x -> {
            if (x.getAutostrategy_name().isEmpty()) {
                x.setAutostrategy_name(null);
            }
        });
        return rows;
    }

    private AutostrategyEventInTable getAutostrategyEventInTable(int shop) {
        var event = new AutostrategyEventInTable();
        event.setShop_id(shop);
        event.setEvent_type(AutostrategyEventType.ALL_DISABLED);
        event.setAutostrategy_id(-1);
        event.setAutostrategy_name(null);
        event.setPayload(new AutostrategyPayload());
        return event;
    }
}
