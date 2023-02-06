package ru.yandex.market.crm.platform.mr;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author apershukov
 */
class EventsReducerTest {

    private static class YieldImpl implements Yield<YTreeMapNode> {

        private final List<YTreeMapNode> result = new ArrayList<>();

        @Override
        public void yield(int index, YTreeMapNode value) {
            result.add(value);
        }

        @Override
        public void close() {
        }

        List<YTreeMapNode> getResult() {
            return result;
        }
    }

    private static YTreeMapNode fact(long timestamp, String payload) {
        return YTree.mapBuilder()
                .key("uuid").value("uuid")
                .key("fact_id").value("fact_id")
                .key("timestamp").value(timestamp)
                .key("payload").value(payload)
                .buildMap();
    }

    private static YTreeMapNode fact(String payload) {
        return fact(TIMESTAMP, payload);
    }

    private static YTreeMapNode event(String eventType,
                                      long eventTimestamp,
                                      YTreeMapNode fact) {
        var builder = YTree.mapBuilder()
                .key("event_type_").value(eventType)
                .key("$timestamp").value(eventTimestamp);

        for (var e : fact.asMap().entrySet()) {
            builder.key(e.getKey()).value(e.getValue());
        }

        return builder.buildMap();
    }

    private static final long TIMESTAMP = 1000;

    private static final String INSERT_TYPE = "INSERT";
    private static final String REMOVE_TYPE = "REMOVE";

    /**
     * В случае если новых событий, связанных с фактом нет, строка с ним возвращается без изменений
     */
    @Test
    void testReturnFactAsIsIfThereWereNoEvents() {
        var fact = fact("payload");
        var result = reduce(fact);

        assertThat(result, hasSize(1));
        assertEquals(fact, result.get(0));
    }

    /**
     * В случае если приходит событие вставки факта, которого раньше не было на выходе
     * должна быть строка этого факта в виде в котором она была вставлена
     */
    @Test
    void testProcessSingleInsertEvent() {
        var fact = fact("payload");
        var event = event("INSERT", 2000, fact);
        var result = reduce(event);

        assertThat(result, hasSize(1));
        assertEquals(fact, result.get(0));
    }

    /**
     * Если попадается сразу несколько событий вставки, выбирается последнее
     */
    @Test
    void testTakeLastValues() {
        var fact1 = fact("payload-1");

        var fact2 = fact("payload-3");
        var event1 = event(INSERT_TYPE, 3000, fact2);

        var fact3 = fact("payload-2");
        var event2 = event(INSERT_TYPE, 2000, fact3);

        var result = reduce(fact1, event1, event2);

        assertThat(result, hasSize(1));
        assertEquals(fact2, result.get(0));
    }

    /**
     * В случае если последним событием было удаление факта строки с ним на выходе не будет
     */
    @Test
    void testProcessDeleteEvent() {
        var fact1 = fact("payload-1");

        var fact2 = fact("payload-3");
        var event1 = event(REMOVE_TYPE, 3000, fact2);

        var fact3 = fact("payload-2");
        var event2 = event(INSERT_TYPE, 2000, fact3);

        var result = reduce(fact1, event1, event2);
        assertThat(result, empty());
    }

    /**
     * Обработка разных фактов не смешивается
     */
    @Test
    void testDoNotMixDifferentFacts() {
        var fact1 = fact(TIMESTAMP, "payload-1");
        var fact2 = fact(2000, "payload-2");

        var result = reduce(fact1, fact2);
        assertThat(result, hasSize(2));
        assertEquals(fact1, result.get(0));
        assertEquals(fact2, result.get(1));
    }

    private List<YTreeMapNode> reduce(YTreeMapNode... rows) {
        var reducer = new EventsReducer(List.of("uuid", "fact_id", "timestamp"));

        var yield = new YieldImpl();
        reducer.reduce(Iterators.forArray(rows), yield, null, null);
        return yield.getResult();
    }
}
