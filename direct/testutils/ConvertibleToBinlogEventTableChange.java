package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

@ParametersAreNonnullByDefault
abstract class ConvertibleToBinlogEventTableChange extends BaseTableChange {

    protected static BinlogEvent createEvent(
            List<? extends ConvertibleToBinlogEventTableChange> bannerImagesTableChanges,
            Operation operation,
            String tableName) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(tableName).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerImagesTableChanges.stream()
                .map(bannerImagesTableChange -> bannerImagesTableChange.createRow(operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    protected BinlogEvent.Row createRow(Operation operation) {
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        if (operation.hasBefore()) {
            fillChangeToMap(before);
        }
        if (operation.hasAfter()) {
            fillChangeToMap(after);
        }
        TestUtils.fillChangedInRow(before, after, getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(getPrimaryKeys()).withBefore(before).withAfter(after);
    }

    protected abstract void fillChangeToMap(Map<String, Object> dest);

    protected abstract Map<String, Object> getPrimaryKeys();

}
