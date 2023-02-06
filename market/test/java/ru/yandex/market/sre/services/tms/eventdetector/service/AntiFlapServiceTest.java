package ru.yandex.market.sre.services.tms.eventdetector.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.Event;
import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.FlapState;
import ru.yandex.market.sre.services.tms.eventdetector.enums.EventType;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.FlapSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.Period;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.RawEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AntiFlapServiceTest {
    List<RawEvent> events;
    long lastEventFinishedAt;

    RawEvent build(EventType type, long from, long to) {
        return RawEvent.Builder.anEvent().type(type).from(from).to(to).build();
    }

    void init(long start) {
        lastEventFinishedAt = start;
        events = new ArrayList<>();
    }

    void next(EventType type, long duration) {
        long from = lastEventFinishedAt;
        lastEventFinishedAt += duration;
        events.add(build(type, from, lastEventFinishedAt));
    }

    long minutes(int minutes) {
        return 60L * minutes;
    }

    @Test
    public void filter() {
        long startTime = new Date().getTime();
        init(startTime);
        next(EventType.OK, minutes(3));
        next(EventType.CRITICAL, minutes(1));
        next(EventType.OK, minutes(3));
        next(EventType.NO_DATA, minutes(3)); //не учитывается во времени даунтайма, но учитывается во флапе
        next(EventType.CRITICAL, minutes(4));
        next(EventType.OK, minutes(31));
        FlapState state = new FlapState();
        List<Event> filtered = new AntiFlapService(null, null)
                .filter(events, state, new FlapSettings());
        assertEquals(1, filtered.size());
        assertNull(state.getEvent());
        Event event = filtered.get(0);
        assertEquals(minutes(1 + 3 + 3 + 4), event.duration().longValue());
        assertEquals(minutes(1 + 3 + 4), event.downtime().longValue());
        assertEquals(startTime + minutes(3), event.getFrom().longValue());
        assertEquals(startTime + minutes(3 + 1 + 3 + 3 + 4), event.getTo().longValue());
    }

    @Test
    public void filter_NoProblemNoEvent() {
        long startTime = new Date().getTime();
        init(startTime);
        next(EventType.OK, minutes(300));
        FlapState state = new FlapState();
        List<Event> filtered = new AntiFlapService(null, null).filter(events, state, new FlapSettings());
        assertEquals(0, filtered.size());
        assertNull(state.getEvent());
    }

    @Test
    public void filter_NoProblem_ThenClose() {
        long startTime = new Date().getTime();
        long EVENT_DURATION = 100, EVENT2_DURATION = 50;
        init(startTime);
        next(EventType.OK, minutes(3));
        next(EventType.NO_DATA, EVENT2_DURATION);
        next(EventType.OK, minutes(300));
        FlapState state = new FlapState();
        Event lastEvent = new Event();
        lastEvent.setPeriod(new Period(startTime - EVENT_DURATION, null));
        lastEvent.setType(EventType.CRITICAL);
        lastEvent.addDowntime(EVENT_DURATION);

        state.setCriticalTimeStart(lastEvent.getFrom());
        state.setHighestType(EventType.CRITICAL);
        state.setType(EventType.CRITICAL);
        state.setEvent(lastEvent);
        List<Event> filtered = new AntiFlapService(null, null).filter(events, state, new FlapSettings());
        assertEquals(1, filtered.size());
        assertNull(state.getEvent());
        Event event = filtered.get(0);
        assertEquals(EVENT_DURATION + EVENT2_DURATION + minutes(3), event.duration().longValue());
        assertEquals(EVENT_DURATION + EVENT2_DURATION, event.downtime().longValue());
        assertEquals(startTime - EVENT_DURATION, event.getFrom().longValue());
        assertEquals(event.getFrom() + event.duration(), event.getTo().longValue());
    }

    @Test
    public void filter_AfterStabileCheck() {
        long startTime = new Date().getTime();
        init(startTime);
        next(EventType.OK, minutes(3));
        next(EventType.NO_DATA, minutes(5));
        next(EventType.OK, minutes(1));
        next(EventType.CRITICAL, minutes(6));
        next(EventType.OK, minutes(300));
        next(EventType.CRITICAL, minutes(7));
        next(EventType.OK, minutes(2));
        next(EventType.NO_DATA, minutes(8));
        next(EventType.OK, minutes(300));

        FlapState state = new FlapState();
        List<Event> filtered = new AntiFlapService(null, null).filter(events, state, new FlapSettings());
        assertEquals(2, filtered.size());
        assertNull(state.getEvent());

        Event event = filtered.get(0);
        assertEquals(startTime + minutes(3), event.getFrom().longValue());
        assertEquals(event.getFrom() + event.duration(), event.getTo().longValue());
        assertEquals(minutes(5 + 1 + 6), event.duration().longValue());
        assertEquals(minutes(5 + 6), event.downtime().longValue());

        event = filtered.get(1);
        assertEquals(startTime + minutes(3 + 5 + 1 + 6 + 300), event.getFrom().longValue());
        assertEquals(event.getFrom() + event.duration(), event.getTo().longValue());
        assertEquals(minutes(7 + 2 + 8), event.duration().longValue());
        assertEquals(minutes(7 + 8), event.downtime().longValue());

    }

    @Test
    public void filter_onepoint_test() {
        init(1586660100);
        next(EventType.OK, 1586662685 - 1586660100);
        next(EventType.CRITICAL, 1586662795 - 1586662685);
        events.add(RawEvent.Builder.anEvent().type(EventType.OK).from(1586662795L).duration(905L).build());
        events.add(RawEvent.Builder.anEvent().type(EventType.OK).from(1586663700L).duration(3600L).build());
        events.add(RawEvent.Builder.anEvent().type(EventType.OK).from(1586663700L).duration(3600L).build());

        FlapState state = new FlapState();
        List<Event> filtered = new AntiFlapService(null, null)
                .filter(events, state, new FlapSettings());
        assertEquals(1, filtered.size());
        assertNull(state.getEvent());

        Event event = filtered.get(0);
        assertEquals(1586662685, event.getFrom().longValue());
        assertEquals(event.getFrom() + event.duration(), event.getTo().longValue());
        assertEquals(1586662795, event.getTo().longValue());
        assertEquals(1586662795 - 1586662685, event.duration().longValue());
        assertEquals(1586662795 - 1586662685, event.downtime().longValue());

    }


    @Test
    public void filter_err_test() {
        //1800	1586856180	CRITICAL	1586856222	1758	1586856222	OK
        FlapState state = new FlapState();
        state.setIndicatorId("1800");
        state.setCriticalDuration(1800);
        state.setCriticalTimeStart(1586856180L);
        state.setHighestType(EventType.CRITICAL);
        state.setRepairTimeStart(1586856222L);
        state.setStableDuration(1758);
        state.setStableTimeStart(1586856222L);
        state.setType(EventType.OK);

        init(1586856222L + minutes(10));
        next(EventType.OK, minutes(20));

        List<Event> filtered = new AntiFlapService(null, null)
                .filter(events, state, new FlapSettings());
        assertEquals(1, filtered.size());
        assertNull(state.getEvent());

        Event event = filtered.get(0);
        assertEquals(1586856180, event.getFrom().longValue());
        assertEquals(event.getFrom() + event.duration(), event.getTo().longValue());
        //assertEquals(1586662795, event.getTo().longValue());
        //assertEquals(1586662795 - 1586662685, event.duration().longValue());
        //assertEquals(1586662795 - 1586662685, event.downtime().longValue());
        //401	ru.yandex.market.sre.services.tms.eventdetector.dao.entity.Event	42	42	BERU_2	1586856180
        // unset	CRITICAL

    }
}
