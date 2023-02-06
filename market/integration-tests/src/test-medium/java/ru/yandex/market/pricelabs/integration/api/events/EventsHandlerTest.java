package ru.yandex.market.pricelabs.integration.api.events;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.api.converter.AutostrategyEventConverter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.misc.AutostrategiesEventsUtils;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.events.AutostrategyEventInTable;
import ru.yandex.market.pricelabs.model.types.events.AutostrategyEventType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPO;
import static ru.yandex.market.pricelabs.integration.api.AbstractApiTests.checkResponse;
import static ru.yandex.market.pricelabs.misc.Utils.getDefaultZoneId;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.autostrategy;

@Slf4j
public class EventsHandlerTest extends AbstractAutostrategiesEventsTests {

    @Test
    public void testGetShopWithNoAutostrategyEvents() {
        AutostrategyEventInTable savedEvents =
                createEventsForDisabledAutostrategiesInShop(TimingUtils.getInstant());
        var expectedEvents = List.of(AutostrategyEventConverter.convertSingle(savedEvents));
        var result = publicApi.autostrategiesAllAutostrategiesDisabledEventsGet(SHOP_WHITE1);
        assertEquals(expectedEvents, result.getBody());
    }

    @Test
    public void testGetShopWithNoAutostrategyEventsWhenShopHasSeverelEventsMustReturnLast() {
        //first day
        createEventsForDisabledAutostrategiesInShop(TimingUtils.getInstant());
        TimingUtils.addTime(1000 * 60 * 60 * 24 * 2);
        createEventsForDisabledAutostrategiesInShop(TimingUtils.getInstant());
        TimingUtils.addTime(1000 * 60 * 60 * 24 * 2);
        //Полученное событие должно быть после этой даты
        OffsetDateTime date = OffsetDateTime.ofInstant(TimingUtils.getInstant(), getDefaultZoneId());
        //second day
        AutostrategyEventInTable savedEvent =
                createEventsForDisabledAutostrategiesInShop(TimingUtils.getInstant());
        TimingUtils.addTime(1000 * 60 * 60 * 24);
        var expectedEvents = AutostrategyEventConverter.convertSingle(savedEvent);
        var result = publicApi.autostrategiesAllAutostrategiesDisabledEventsGet(SHOP_WHITE1).getBody();
        assertEquals(1, result.size());
        assertEquals(expectedEvents, result.get(0));
        assertEquals(1, result.get(0).getEvents().size());
        assert (result.get(0).getEvents().get(0).getTimestamp().isAfter(date));
    }


    @Test
    public void testGetEvents() {

        var target = WHITE_TARGET;
        var startTime = TimingUtils.getInstant();
        //first day
        var s1a1 = autostrategy("s11", CPO);
        var createdFirst = checkResponse(publicApi.autostrategyBatchPost(SHOP_WHITE1, List.of(
                new AutostrategySaveWithId().autostrategy(s1a1)), target, null, null));
        TimingUtils.addTime(1000 * 60 * 60 * 24);

        //second day
        var s2a1 = autostrategy("s21", CPO);
        var createdSecond = checkResponse(publicApi.autostrategyBatchPost(SHOP_WHITE1, List.of(
                new AutostrategySaveWithId().autostrategy(s2a1)), target, null, null));
        TimingUtils.addTime(1000 * 60);
        checkResponse(publicApi.autostrategyBatchChangeStatePost(SHOP_WHITE1, List.of(createdFirst.get(0).getId()),
                false,
                target));
        TimingUtils.addTime(1000 * 60);
        checkResponse(publicApi.autostrategyBatchChangeStatePost(SHOP_WHITE1, List.of(createdSecond.get(0).getId()),
                false,
                target));
        TimingUtils.addTime(1000 * 60 * 60 * 24);

        //Third day
        checkResponse(publicApi.autostrategyBatchChangeStatePost(SHOP_WHITE1, List.of(createdFirst.get(0).getId()),
                true,
                target));
        TimingUtils.addTime(1000 * 60 * 60 * 24);
        var endTime = TimingUtils.getInstant();
        eventsGeneratorWhite.generateAndSaveEvents();

        String dateFrom = LocalDate.ofInstant(startTime, getDefaultZoneId()).toString();
        String dateTo = LocalDate.ofInstant(endTime, getDefaultZoneId()).toString();
        var result = publicApi.autostrategiesEventsGet(SHOP_WHITE1, dateFrom, dateTo, null);

        OffsetDateTime offsetDateFrom =
                OffsetDateTime.ofInstant(startTime, getDefaultZoneId()).truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime offsetDateTo =
                OffsetDateTime.ofInstant(endTime, getDefaultZoneId()).truncatedTo(ChronoUnit.DAYS);
        var expectedEvents = AutostrategiesEventsUtils.getHistory(offsetDateFrom, createdFirst.get(0),
                createdSecond.get(0), SHOP_WHITE1);

        assertEquals(expectedEvents, result.getBody());
    }

    private AutostrategyEventInTable createEventsForDisabledAutostrategiesInShop(Instant time) {
        AutostrategyEventInTable event = new AutostrategyEventInTable();
        event.setEvent_type(AutostrategyEventType.ALL_DISABLED);
        event.setShop_id(AbstractAutostrategiesEventsTests.SHOP_WHITE1);
        event.setAutostrategy_id(-1);
        event.setTimestamp(time);
        var table = coreTables.getAutostrategiesEventTable();
        var processorCfg = ytCfg.getProcessorCfg(table);
        processorCfg.getClient().execInTransaction(
                tx -> tx.insertRows(table.getTable(), table.getBinder(), List.of(event))
        );
        return event;
    }
}
