package ru.yandex.market.crm.core.domain.trigger;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EventTableTest {

    static class TestEventTable extends EventTable<LocalDateTime> {

        LocalDateTime from = LocalDateTime.now();

        TestEventTable() {
            super(Map.of(
                    "start", Function.identity()
            ));
        }

        @Override
        public LocalDateTime fromTime() {
            return this.from;
        }
    }

    /**
     * В таблицу не должны загружаться сущности, время событий для которых вышло
     */
    @Test
    public void testUpdate() {
        TestEventTable table = new TestEventTable();
        table.update(Arrays.asList(
                null,
                table.from.minusDays(1),
                table.from.minusNanos(1),
                table.from,
                table.from.plusNanos(1),
                table.from.plusNanos(2)
        ));
        EventResult<LocalDateTime> nextEvent = table.getNextEvent();

        assertEquals(2, nextEvent.getItems().size());
        assertEquals(Set.of(table.from.plusNanos(1), table.from.plusNanos(2)), new HashSet<>(nextEvent.getItems()));
    }

    /**
     * Маркировка сущностей при обновлении должна сохранятся
     */
    @Test
    public void testUpdateKeepMarks() {
        TestEventTable table = new TestEventTable();
        LocalDateTime e1 = table.from.plusNanos(1);
        LocalDateTime e2 = table.from.plusNanos(2);
        List<LocalDateTime> entities = List.of(e1, e2, table.from.plusDays(2));

        table.update(entities);

        EventResult<LocalDateTime> nextEvent = table.getNextEvent();
        assertSetEquals(List.of(e1, e2), nextEvent.getItems());

        table.notifyOnEvent(nextEvent);

        table.update(entities);
        Set<LocalDateTime> marked = table.eventTable.stream()
                .filter(st -> st.isMarked("start"))
                .map(EventTable.EventState::getEntity)
                .collect(Collectors.toSet());
        assertEquals(Set.of(e1, e2), marked);
    }

    /**
     * Без обновления и маркировки, таблица должна возвращать событие по одному и
     * тому же списку сущностей
     */
    @Test
    public void testIdempotency() {
        TestEventTable table = new TestEventTable();
        LocalDateTime e1 = table.from.plusNanos(1);
        LocalDateTime e2 = table.from.plusNanos(2);
        List<LocalDateTime> entities = List.of(e1, e2, table.from.plusDays(2));

        table.update(entities);

        EventResult<LocalDateTime> evt = table.getNextEvent();
        assertSetEquals(evt.getItems(), table.getNextEvent().getItems());
        assertSetEquals(evt.getItems(), table.getNextEvent().getItems());
    }

    /**
     * Промаркированные сущности не должны возвращаться в следующем событии
     */
    @Test
    public void testAbsentMarked() {
        TestEventTable table = new TestEventTable();
        LocalDateTime e1 = table.from.plusNanos(1);
        LocalDateTime e2 = table.from.plusNanos(2);
        LocalDateTime e3 = table.from.plusDays(2);

        table.update(List.of(e1, e2, e3));

        EventResult<LocalDateTime> nextEvent = table.getNextEvent();
        assertSetEquals(List.of(e1, e2), nextEvent.getItems());

        table.notifyOnEvent(nextEvent);

        nextEvent = table.getNextEvent();
        assertSetEquals(Set.of(e3), nextEvent.getItems());
    }

    private static  <T> void assertSetEquals(Collection<T> one, Collection<T> other) {
        assertEquals(new HashSet<>(one), new HashSet<>(other));
    }
}
