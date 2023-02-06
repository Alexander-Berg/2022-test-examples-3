package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static org.assertj.core.api.Assertions.assertThat;

public class BinlogLogbrokerWriterTest {

    @Test
    public void convertJsonAndCheckDateTimeFormat() {
        var binlogEvent = new BinlogEvent()
                .withUtcTimestamp(LocalDateTime.now())
                .withOperation(Operation.INSERT)
                .withRows(List.of(
                        new BinlogEvent.Row()
                                .withAfter(Map.of(
                                        "id", 1L,
                                        "datetime", LocalDateTime.of(1000, 1, 2, 3, 4, 5)
                                ))
                                .withBefore(Map.of())
                                .withPrimaryKey(Map.of(
                                        "id", 1L))
                                .withRowIndex(0)
                        )
                );

        var result = new String(BinlogLogbrokerWriter.convertToJson(binlogEvent));
        assertThat(result).contains("\"datetime\":\"1000-01-02 03:04:05\"");
    }
}
