package ru.yandex.market.fulfillment.stockstorage.stockrestore.mrjob;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.fulfillment.stockstorage.stockrestore.AuditedYTableColumn;
import ru.yandex.market.fulfillment.stockstorage.stockrestore.auditevent.StockType;
import ru.yandex.market.fulfillment.stockstorage.stockrestore.auditevent.UnitId;
import ru.yandex.market.fulfillment.stockstorage.stockrestore.mrjob.StockRestoreJob.StockReducer;

import static ru.yandex.market.fulfillment.stockstorage.stockrestore.AuditedYTableColumn.EXPIRED;

public class StockRestoreTest {
    @Test
    @DisplayName("Событие SKU_CREATED зануляет еще не установленные другими событиями суммы")
    public void delayedSkuCreateZeroesAmountsTest() {
        StockReducer reducer = new StockReducer(LocalDate.of(2022, 3, 17), LocalDate.of(2022, 3, 17));

        UnitId unitId = new UnitId(1, 1, "1");

        List<YTreeMapNode> events = new ArrayList<>();
        events.add(buildEntry(unitId, "2022-03-17T22:00:00", null, "OTHER_EVENT", Map.of(EXPIRED, 2)));
        events.add(buildEntry(unitId, "2022-03-17T22:01:00", null, "SKU_CREATED", Map.of()));

        List<Yielded> expected = List.of(
                new Yielded(0, buildZeroEntry(unitId, "2022-03-17T22:01:00", "2022-03-17", null, EXPIRED, 2))
        );
        List<Yielded> actual = getReduceResult(reducer, events);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Записи создаются для всех дат в интервале (entry < from < to)")
    public void allEntriesCreatedNoUpdatesInIntervalTest() {
        StockReducer reducer = new StockReducer(LocalDate.of(2022, 3, 20), LocalDate.of(2022, 3, 25));

        UnitId unitId = new UnitId(1, 1, "1");

        List<YTreeMapNode> events = new ArrayList<>();
        events.add(buildZeroEntry(unitId, "2022-03-15T22:00:00", null, "OTHER_EVENT"));

        List<Yielded> expected = List.of(
                new Yielded(0, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-20", null)),
                new Yielded(1, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-21", null)),
                new Yielded(2, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-22", null)),
                new Yielded(3, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-23", null)),
                new Yielded(4, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-24", null)),
                new Yielded(5, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-25", null))
        );
        List<Yielded> actual = getReduceResult(reducer, events);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("""
            Записи создаются для всех дат в интервале (entry < from < entry < to), \
            состояние корректно обновляется""")
    public void allEntriesCreatedUpdateWithinIntervalTest() {
        StockReducer reducer = new StockReducer(LocalDate.of(2022, 3, 20), LocalDate.of(2022, 3, 25));

        UnitId unitId = new UnitId(1, 1, "1");

        List<YTreeMapNode> events = new ArrayList<>();
        events.add(buildZeroEntry(unitId, "2022-03-15T22:00:00", null, "OTHER_EVENT"));
        events.add(buildZeroEntry(unitId, "2022-03-23T22:00:00", null, "OTHER_EVENT", EXPIRED, 1));

        List<Yielded> expected = List.of(
                new Yielded(0, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-20", null)),
                new Yielded(1, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-21", null)),
                new Yielded(2, buildZeroEntry(unitId, "2022-03-15T22:00:00", "2022-03-22", null)),
                new Yielded(3, buildZeroEntry(unitId, "2022-03-23T22:00:00", "2022-03-23", null, EXPIRED, 1)),
                new Yielded(4, buildZeroEntry(unitId, "2022-03-23T22:00:00", "2022-03-24", null, EXPIRED, 1)),
                new Yielded(5, buildZeroEntry(unitId, "2022-03-23T22:00:00", "2022-03-25", null, EXPIRED, 1))
        );
        List<Yielded> actual = getReduceResult(reducer, events);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Записи создаются для всех дат при не заданном updated_at")
    public void entryWithoutUpdatedAtTest() {
        StockReducer reducer = new StockReducer(LocalDate.of(2022, 3, 20), LocalDate.of(2022, 3, 25));

        UnitId unitId = new UnitId(1, 1, "1");

        List<YTreeMapNode> events = new ArrayList<>();
        events.add(buildZeroEntry(unitId, null, null, "OTHER_EVENT"));

        List<Yielded> expected =
                List.of(
                        new Yielded(0, buildZeroEntry(unitId, null, "2022-03-20", null)),
                        new Yielded(1, buildZeroEntry(unitId, null, "2022-03-21", null)),
                        new Yielded(2, buildZeroEntry(unitId, null, "2022-03-22", null)),
                        new Yielded(3, buildZeroEntry(unitId, null, "2022-03-23", null)),
                        new Yielded(4, buildZeroEntry(unitId, null, "2022-03-24", null)),
                        new Yielded(5, buildZeroEntry(unitId, null, "2022-03-25", null))
                );
        List<Yielded> actual = getReduceResult(reducer, events);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Записи создаются для всех дат при не заданном updated_at с обновлением с заданным updated_at")
    public void entryWithoutUpdatedAtUpdateWithUpdateAtTest() {
        StockReducer reducer = new StockReducer(LocalDate.of(2022, 3, 20), LocalDate.of(2022, 3, 25));

        UnitId unitId = new UnitId(1, 1, "1");

        List<YTreeMapNode> events = new ArrayList<>();
        events.add(buildZeroEntry(unitId, null, null, "OTHER_EVENT"));
        events.add(buildZeroEntry(unitId, "2022-03-23T22:01:00", null, "OTHER_EVENT"));

        List<Yielded> expected =
                List.of(
                        new Yielded(0, buildZeroEntry(unitId, null, "2022-03-20", null)),
                        new Yielded(1, buildZeroEntry(unitId, null, "2022-03-21", null)),
                        new Yielded(2, buildZeroEntry(unitId, null, "2022-03-22", null)),
                        new Yielded(3, buildZeroEntry(unitId, "2022-03-23T22:01:00", "2022-03-23", null)),
                        new Yielded(4, buildZeroEntry(unitId, "2022-03-23T22:01:00", "2022-03-24", null)),
                        new Yielded(5, buildZeroEntry(unitId, "2022-03-23T22:01:00", "2022-03-25", null))
                );
        List<Yielded> actual = getReduceResult(reducer, events);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("""
            Записи создаются для всех дат в интервале (entry < from < entry < to) для нескольких UnitId, \
            состояние корректно обновляется""")
    public void allEntriesCreatedUpdateWithinIntervalSeveralUnitIdTest() {
        StockReducer reducer = new StockReducer(LocalDate.of(2022, 3, 20), LocalDate.of(2022, 3, 25));

        UnitId unitId1 = new UnitId(1, 1, "1");
        UnitId unitId2 = new UnitId(1, 1, "2");

        List<YTreeMapNode> events = new ArrayList<>();
        events.add(buildZeroEntry(unitId1, "2022-03-15T22:00:00", null, "OTHER_EVENT"));
        events.add(buildZeroEntry(unitId1, "2022-03-23T22:00:00", null, "OTHER_EVENT", EXPIRED, 1));
        events.add(buildZeroEntry(unitId2, "2022-03-15T22:00:00", null, "OTHER_EVENT"));
        events.add(buildZeroEntry(unitId2, "2022-03-23T22:00:00", null, "OTHER_EVENT", EXPIRED, 1));

        List<Yielded> expected = List.of(
                new Yielded(0, buildZeroEntry(unitId1, "2022-03-15T22:00:00", "2022-03-20", null)),
                new Yielded(1, buildZeroEntry(unitId1, "2022-03-15T22:00:00", "2022-03-21", null)),
                new Yielded(2, buildZeroEntry(unitId1, "2022-03-15T22:00:00", "2022-03-22", null)),
                new Yielded(3, buildZeroEntry(unitId1, "2022-03-23T22:00:00", "2022-03-23", null, EXPIRED, 1)),
                new Yielded(4, buildZeroEntry(unitId1, "2022-03-23T22:00:00", "2022-03-24", null, EXPIRED, 1)),
                new Yielded(5, buildZeroEntry(unitId1, "2022-03-23T22:00:00", "2022-03-25", null, EXPIRED, 1)),
                new Yielded(0, buildZeroEntry(unitId2, "2022-03-15T22:00:00", "2022-03-20", null)),
                new Yielded(1, buildZeroEntry(unitId2, "2022-03-15T22:00:00", "2022-03-21", null)),
                new Yielded(2, buildZeroEntry(unitId2, "2022-03-15T22:00:00", "2022-03-22", null)),
                new Yielded(3, buildZeroEntry(unitId2, "2022-03-23T22:00:00", "2022-03-23", null, EXPIRED, 1)),
                new Yielded(4, buildZeroEntry(unitId2, "2022-03-23T22:00:00", "2022-03-24", null, EXPIRED, 1)),
                new Yielded(5, buildZeroEntry(unitId2, "2022-03-23T22:00:00", "2022-03-25", null, EXPIRED, 1))
        );
        List<Yielded> actual = getReduceResult(reducer, events);

        Assertions.assertEquals(expected, actual);
    }

    private static List<Yielded> getReduceResult(StockReducer reducer, List<YTreeMapNode> events) {
        List<Yielded> output = new ArrayList<>();

        Yield<YTreeMapNode> yield = new Yield<YTreeMapNode>() {
            @Override
            public void close() throws IOException {
            }

            @Override
            public void yield(int index, YTreeMapNode value) {
                YTreeNode copy = YTree.deepCopy(value);
                output.add(new Yielded(index, copy));
            }
        };
        reducer.reduce(events.iterator(), yield, null, null);

        return output;
    }

    private static YTreeMapNode buildZeroEntry(UnitId unitId, String updatedAt, String date, String eventType) {
        return buildZeroEntry(unitId, updatedAt, date, eventType, Map.of());
    }

    private static YTreeMapNode buildZeroEntry(UnitId unitId, String updatedAt, String date, String eventType,
            AuditedYTableColumn column, Integer amount) {
        return buildZeroEntry(unitId, updatedAt, date, eventType, Map.of(column, amount));
    }

    private static YTreeMapNode buildZeroEntry(UnitId unitId, String updateAt, String date, String eventType,
            Map<AuditedYTableColumn, Integer> amounts) {
        return buildEntry(unitId, updateAt, date, eventType, zeroAmountMap(amounts));
    }

    private static YTreeMapNode buildEntry(UnitId unitId, String updatedAt, String date, String eventType,
            Map<AuditedYTableColumn, Integer> amounts) {
        YTreeMapNode entry = YTree.mapBuilder().buildMap();

        unitId.applyTo(entry);

        if (updatedAt != null) {
            AuditedYTableColumn.UPDATED_AT.applyTo(entry, LocalDateTime.parse(updatedAt));
        } else {
            AuditedYTableColumn.UPDATED_AT.applyTo(entry, null);
        }

        if (date != null) {
            entry.asMap().put("date", YTree.stringNode(date));
        }

        if (eventType != null) {
            AuditedYTableColumn.EVENT_TYPE.applyTo(entry, eventType);
        }

        for (Map.Entry<AuditedYTableColumn, Integer> amountEntry : amounts.entrySet()) {
            assignAmount(entry, amountEntry.getKey(), amountEntry.getValue());
        }

        for (StockType type : StockType.values()) {
            assignAmount(entry, type.getAmountColumns().getAmountCol(), null);
            assignAmount(entry, type.getAmountColumns().getFreezeCol(), null);
        }

        return entry;
    }

    private static void assignAmount(YTreeMapNode entry, AuditedYTableColumn col, Integer amount) {
        if (col == null) {
            return;
        }

        if (!entry.containsKey(col.columnName())) {
            col.applyTo(entry, amount);
        }
    }

    private static Map<AuditedYTableColumn, Integer> zeroAmountMap(Map<AuditedYTableColumn, Integer> except) {
        Map<AuditedYTableColumn, Integer> res = new HashMap<>();
        for (StockType type : StockType.values()) {
            res.put(type.getAmountColumns().getAmountCol(), 0);
            res.put(type.getAmountColumns().getFreezeCol(), 0);
        }
        res.putAll(except);
        return res;
    }

    private static class Yielded {
        private int index;
        private YTreeNode entry;

        Yielded(int index, YTreeNode entry) {
            super();
            this.index = index;
            this.entry = entry;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((entry == null) ? 0 : entry.hashCode());
            result = prime * result + index;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Yielded other = (Yielded) obj;
            if (entry == null) {
                if (other.entry != null) {
                    return false;
                }
            } else if (!entry.equals(other.entry)) {
                return false;
            }
            if (index != other.index) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return index + "/" + entry;
        }
    }
}
