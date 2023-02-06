package ru.yandex.market.pricelabs.integration.api.events;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.misc.AutostrategiesEventsUtils;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.events.AutostrategyEventInTable;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.events.AutostrategyEventType;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.events.EventsGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPO;
import static ru.yandex.market.pricelabs.integration.api.AbstractApiTests.checkResponse;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.autostrategy;

@Slf4j
public class EventsGenerationTests extends AbstractAutostrategiesEventsTests {

    static final String BLUE_TARGET = AutostrategyTarget.blue.name();

    @Qualifier("autostrategeiesEventsGeneratorBlue")
    @Autowired
    private EventsGenerator eventsGeneratorBlue;

    @Test
    public void testGenerateAutostrategiesEvents() {
        var startTime = TimingUtils.getInstant();
        //first day
        var s1a1 = autostrategy("s11", CPO);
        var createdFirst = checkResponse(publicApi.autostrategyBatchPost(SHOP_WHITE1, List.of(
                new AutostrategySaveWithId().autostrategy(s1a1)), WHITE_TARGET, null, null));
        Instant whiteExpectedEndTime = TimingUtils.getInstant();
        TimingUtils.addTime(1000 * 60);
        eventsGeneratorWhite.generateAndSaveEvents();
        Instant whiteEndTime = getSystemEvents(startTime, AutostrategyEventType.LAST_READ_EVENT_WHITE);
        assertEquals(whiteExpectedEndTime, whiteEndTime);
        var firstDayResult = getGeneratedEvents(startTime);
        var expectedFirstEvent =
                AutostrategiesEventsUtils.getAddAutostrategyEventsTable(createdFirst.get(0), SHOP_WHITE1);
        AutostrategiesEventsUtils.compareAutostrategiesEvents(firstDayResult, expectedFirstEvent);

        //second day
        startTime = TimingUtils.getInstant();
        var s2a1 = autostrategy("s21", CPO);
        var createdSecond = checkResponse(publicApi.autostrategyBatchPost(SHOP_BLUE2, List.of(
                new AutostrategySaveWithId().autostrategy(s2a1)), BLUE_TARGET, null, null));
        TimingUtils.addTime(1000 * 60);
        checkResponse(publicApi.autostrategyBatchChangeStatePost(SHOP_WHITE1, List.of(createdFirst.get(0).getId()),
                false,
                WHITE_TARGET));
        whiteExpectedEndTime = TimingUtils.getInstant();
        TimingUtils.addTime(1000 * 60);
        checkResponse(publicApi.autostrategyBatchChangeStatePost(SHOP_BLUE2, List.of(createdSecond.get(0).getId()),
                false,
                BLUE_TARGET));
        Instant blueExpectedEndTime = TimingUtils.getInstant();

        eventsGeneratorWhite.generateAndSaveEvents();
        whiteEndTime = getSystemEvents(startTime, AutostrategyEventType.LAST_READ_EVENT_WHITE);
        assertEquals(whiteExpectedEndTime, whiteEndTime);

        eventsGeneratorBlue.generateAndSaveEvents();
        Instant blueEndTime = getSystemEvents(startTime, AutostrategyEventType.LAST_READ_EVENT_BLUE);
        assertEquals(blueExpectedEndTime, blueEndTime);

        var secondDayResult = getGeneratedEvents(startTime);
        var expectedSecondEvent =
                new ArrayList<>(AutostrategiesEventsUtils.getAddAutostrategyEventsTable(createdSecond.get(0),
                        SHOP_BLUE2));
        expectedSecondEvent.addAll(AutostrategiesEventsUtils.getRemoveAutostrategyEventsTable(createdFirst.get(0),
                SHOP_WHITE1));
        expectedSecondEvent.addAll(AutostrategiesEventsUtils.getRemoveAutostrategyEventsTable(createdSecond.get(0),
                SHOP_BLUE2));
        AutostrategiesEventsUtils.compareAutostrategiesEvents(secondDayResult, expectedSecondEvent);
    }

    private List<AutostrategyEventInTable> getGeneratedEvents(Instant timeFrom) {
        Instant timeTo = TimingUtils.getInstant();
        var table = coreTables.getAutostrategiesEventTable();
        String query = "* from [" + table.getTable() + "] where event_type >= 0 and timestamp between "
                + timeFrom.toEpochMilli() + " and " + timeTo.toEpochMilli();
        var rows =
                ytClient.selectRows(query, table.getBinder()).stream()
                        .sorted(Comparator.comparing(AutostrategyEventInTable::getTimestamp))
                        .collect(Collectors.toList());
        rows.forEach(x -> {
            if (x.getAutostrategy_name().isEmpty()) {
                x.setAutostrategy_name(null);
            }
        });
        return rows;
    }

    private Instant getSystemEvents(Instant timeFrom, AutostrategyEventType eventType) {
        Instant timeTo = TimingUtils.getInstant();
        var table = coreTables.getAutostrategiesEventTable();
        String query = "* from [" + table.getTable() + "] where event_type =" + eventType.value() + " and timestamp " +
                "between " + timeFrom.toEpochMilli() + " and " + timeTo.toEpochMilli() + "order by timestamp desc " +
                "limit 1";
        var rows = ytClient.selectRows(query, table.getBinder());
        return rows.get(0).getTimestamp();
    }
}
