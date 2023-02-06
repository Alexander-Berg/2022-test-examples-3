package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class PhrasesTableChange extends BaseTableChange {
    public long pid;
    public long cid;

    public static BinlogEvent createPhrasesEvent(List<PhrasesTableChange> phrasesTableChanges, Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(PHRASES.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = phrasesTableChanges.stream()
                .map(phrasesTableChange -> createPhrasesTableRow(phrasesTableChange, operation)
                ).collect(Collectors.toList());

        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createPhrasesTableRow(PhrasesTableChange phrasesTableChange, Operation operation) {
        Map<String, Object> primaryKeys = Map.of(PHRASES.PID.getName(), phrasesTableChange.pid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(PHRASES.CID.getName(), phrasesTableChange.cid);
        }

        if (!operation.equals(DELETE)) {
            after.put(PHRASES.CID.getName(), phrasesTableChange.cid);
        }
        fillChangedInRow(before, after, phrasesTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public PhrasesTableChange withPid(long pid) {
        this.pid = pid;
        return this;
    }

    public PhrasesTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }
}
