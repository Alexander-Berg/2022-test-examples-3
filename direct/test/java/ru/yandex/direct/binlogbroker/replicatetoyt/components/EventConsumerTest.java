package ru.yandex.direct.binlogbroker.replicatetoyt.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.binlogbroker.logbroker_utils.models.BinlogEventWithOffset;

@ParametersAreNonnullByDefault
public class EventConsumerTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static List<String> mkAndSplit(Object... operationsAndTables) {
        List<BinlogEventWithOffset> result = new ArrayList<>();
        List<BinlogEvent> binlogEvents = new ArrayList<>(operationsAndTables.length / 2);
        for (int i = 0; i < operationsAndTables.length; i += 2) {
            binlogEvents.add(new BinlogEvent()
                    .withOperation((Operation) operationsAndTables[i])
                    .withTable((String) operationsAndTables[i + 1]));
        }
        long offset = 1;
        int partition = 0;
        int seqNo = 1;
        for (BinlogEvent event : binlogEvents) {
            result.add(new BinlogEventWithOffset(event, offset, partition, seqNo));
            ++offset;
            ++seqNo;
        }
        return EventConsumer.splitEvents(result.iterator()).stream()
                .map(eventChunk -> String.format("%s-%d-%s",
                        eventChunk.eventType.name(),
                        eventChunk.offsetOfLastEvent,
                        eventChunk.events.stream()
                                .map(BinlogEvent::getTable)
                                .collect(Collectors.joining(","))))
                .collect(Collectors.toList());
    }

    /**
     * Разбиение на пачки должно соответствовать следующим условиям:
     * <ul>
     * <li>Порядок событий сохраняется</li>
     * <li>В одной пачке не может быть и DML, и DDL</li>
     * <li>Если в пачке DDL, то пачка состоит из одного элемента</li>
     * <li>Передаётся offset последнего события в пачке</li>
     * </ul>
     */
    @Test
    public void splitEvents() {
        softly.assertThat(mkAndSplit(Operation.INSERT, "one"))
                .describedAs("Just one DML")
                .containsExactly("DML-1-one");

        softly.assertThat(mkAndSplit(Operation.SCHEMA, "one"))
                .describedAs("Just one DDL")
                .containsExactly("DDL-1-one");

        softly.assertThat(mkAndSplit(Operation.INSERT, "one", Operation.UPDATE, "two", Operation.DELETE, "three"))
                .describedAs("Three DMLs")
                .containsExactly("DML-3-one,two,three");

        softly.assertThat(mkAndSplit(Operation.SCHEMA, "one", Operation.SCHEMA, "two", Operation.SCHEMA, "three"))
                .describedAs("Three DDLs")
                .containsExactly("DDL-1-one", "DDL-2-two", "DDL-3-three");

        softly.assertThat(
                mkAndSplit(
                        Operation.INSERT, "one",
                        Operation.UPDATE, "two",
                        Operation.SCHEMA, "three",
                        Operation.DELETE, "four",
                        Operation.INSERT, "five"))
                .describedAs("Two DMLs, one DDL, two DMLs")
                .containsExactly("DML-2-one,two", "DDL-3-three", "DML-5-four,five");

        softly.assertThat(
                mkAndSplit(
                        Operation.INSERT, "one",
                        Operation.UPDATE, "two",
                        Operation.SCHEMA, "three",
                        Operation.SCHEMA, "four",
                        Operation.DELETE, "five",
                        Operation.INSERT, "six"))
                .describedAs("Two DMLs, two DDLs, two DMLs")
                .containsExactly("DML-2-one,two", "DDL-3-three", "DDL-4-four", "DML-6-five,six");

        softly.assertThat(
                mkAndSplit(
                        Operation.INSERT, "one",
                        Operation.UPDATE, "two",
                        Operation.SCHEMA, "three"))
                .describedAs("Two DMLs, one DDL")
                .containsExactly("DML-2-one,two", "DDL-3-three");

        softly.assertThat(
                mkAndSplit(
                        Operation.SCHEMA, "one",
                        Operation.UPDATE, "two",
                        Operation.INSERT, "three"))
                .describedAs("One DDL, two DMLs")
                .containsExactly("DDL-1-one", "DML-3-two,three");
    }
}
