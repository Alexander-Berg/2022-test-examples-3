package ru.yandex.market.pricelabs.misc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategiesEvents;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyEvent;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyEventAdd;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyEventChanges;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyEventStatusChange;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.model.events.AutostrategyEventInTable;
import ru.yandex.market.pricelabs.model.events.AutostrategyPayload;
import ru.yandex.market.pricelabs.model.types.events.AutostrategyEventType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AutostrategiesEventsUtils {
    private AutostrategiesEventsUtils() {
        //
    }

    public static List<AutostrategiesEvents> getHistory(OffsetDateTime dateFrom,
                                                        AutostrategyLoad auto1,
                                                        AutostrategyLoad auto2,
                                                        int shopId) {
        List<AutostrategyEvent> firstDayEvents = new ArrayList<>(List.copyOf(getAddAutostrategyEvents(auto1, shopId)));
        AutostrategiesEvents firstDayEvent = new AutostrategiesEvents().date(dateFrom).events(firstDayEvents);

        var day2 = dateFrom.plusDays(1);
        List<AutostrategyEvent> secondCreateEvent = getAddAutostrategyEvents(auto2, shopId);
        List<AutostrategyEvent> firstRemoveAutostrategyEvents = getRemoveAutostrategyEvents(auto1, shopId);
        List<AutostrategyEvent> secondRemoveAutostrategyEvents = getRemoveAutostrategyEvents(auto2, shopId);
        List<AutostrategyEvent> secondDayEvents =
                new ArrayList<>(secondCreateEvent.size()
                        + firstRemoveAutostrategyEvents.size()
                        + secondRemoveAutostrategyEvents.size());
        secondDayEvents.addAll(secondCreateEvent);
        secondDayEvents.addAll(firstRemoveAutostrategyEvents);
        secondDayEvents.addAll(secondRemoveAutostrategyEvents);
        AutostrategiesEvents second = new AutostrategiesEvents().date(day2)
                .events(secondDayEvents);

        var day3 = day2.plusDays(1);
        List<AutostrategyEvent> thirdDatEvents = new ArrayList<>(List.copyOf(getEnableAutostrategyEvents(auto1,
                shopId)));
        AutostrategiesEvents thirdDayEvent = new AutostrategiesEvents().date(day3).events(thirdDatEvents);

        return List.of(firstDayEvent, second, thirdDayEvent);
    }

    public static List<AutostrategyEvent> getRemoveAutostrategyEvents(AutostrategyLoad autostrategyLoad, int shopId) {
        var changes = new AutostrategyEventChanges()
                .statusChanged(new AutostrategyEventStatusChange()
                        .oldStatus(true).newStatus(false));
        return List.of(
                getAutostrategyEvent(autostrategyLoad, AutostrategyEvent.TypeEnum.STATUS_UPDATE, changes, shopId),
                getAutostrategyEvent(autostrategyLoad, AutostrategyEvent.TypeEnum.REMOVE, null, shopId)
        );
    }

    public static List<AutostrategyEvent> getEnableAutostrategyEvents(AutostrategyLoad autostrategyLoad, int shopId) {
        var changes = new AutostrategyEventChanges()
                .statusChanged(new AutostrategyEventStatusChange()
                        .oldStatus(false).newStatus(true));
        return List.of(
                getAutostrategyEvent(autostrategyLoad, AutostrategyEvent.TypeEnum.STATUS_UPDATE, changes, shopId)
        );
    }

    public static List<AutostrategyEvent> getAddAutostrategyEvents(AutostrategyLoad autostrategyLoad, int shopId) {
        var changes = new AutostrategyEventChanges()
                .added(new AutostrategyEventAdd()
                        .offers(autostrategyLoad.getOfferCount()));
        return List.of(
                getAutostrategyEvent(autostrategyLoad, AutostrategyEvent.TypeEnum.ADD, changes, shopId)
        );
    }

    private static AutostrategyEvent getAutostrategyEvent(AutostrategyLoad autostrategyLoad,
                                                          AutostrategyEvent.TypeEnum add,
                                                          @Nullable AutostrategyEventChanges changes,
                                                          int shopId) {
        return new AutostrategyEvent().autostrategyId(autostrategyLoad.getId()).name(autostrategyLoad.getName())
                .timestamp(TimingUtils.getInstant().atOffset(ZoneOffset.UTC))
                .type(add)
                .changes(changes)
                .shopId(shopId);
    }


    public static List<AutostrategyEventInTable> getRemoveAutostrategyEventsTable(AutostrategyLoad autostrategyLoad,
                                                                                  int shopId) {
        var payload = new AutostrategyPayload();
        payload.setNewStatus(false);
        payload.setOldStatus(true);
        var event1 = getEventInTable(autostrategyLoad, AutostrategyEventType.STATUS_UPDATE, payload, shopId);

        var event2 = getEventInTable(autostrategyLoad, AutostrategyEventType.REMOVE, null, shopId);
        return List.of(event1, event2);
    }

    public static List<AutostrategyEventInTable> getAddAutostrategyEventsTable(AutostrategyLoad autostrategyLoad,
                                                                               int shopId) {
        var payload = new AutostrategyPayload();
        payload.setNewValue(autostrategyLoad.getOfferCount());
        var event = getEventInTable(autostrategyLoad, AutostrategyEventType.ADD, payload, shopId);
        return List.of(event);
    }

    private static AutostrategyEventInTable getEventInTable(AutostrategyLoad current, AutostrategyEventType type,
                                                            @Nullable AutostrategyPayload payload, int shopId) {
        var result = new AutostrategyEventInTable();
        result.setEvent_type(type);
        result.setTimestamp(current.getTimestamp().toInstant());
        result.setAutostrategy_id(current.getId());
        result.setAutostrategy_name(current.getName());
        result.setPayload(payload);
        result.setShop_id(shopId);
        return result;
    }

    public static void compareAutostrategiesEvents(AutostrategyEventInTable real,
                                                   AutostrategyEventInTable expected) {
        assertNotNull(real);
        assertNotNull(expected);
        assertEquals(expected.getShop_id(), real.getShop_id());
        assertEquals(expected.getAutostrategy_id(), real.getAutostrategy_id());
        assertEquals(expected.getEvent_type(), real.getEvent_type());
        assertEquals(expected.getPayload(), real.getPayload());
        assertEquals(expected.getAutostrategy_name(), real.getAutostrategy_name());
    }

    public static void compareAutostrategiesEvents(List<AutostrategyEventInTable> real,
                                                   List<AutostrategyEventInTable> expected) {
        assertNotNull(real);
        assertNotNull(expected);
        assertEquals(expected.size(), real.size());
        for (int i = 0; i < real.size(); i++) {
            compareAutostrategiesEvents(real.get(i), expected.get(i));
        }
    }
}
