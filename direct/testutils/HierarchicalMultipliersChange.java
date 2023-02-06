package ru.yandex.direct.ess.router.testutils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.HIERARCHICAL_MULTIPLIERS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class HierarchicalMultipliersChange extends BaseTableChange {
    public long hierarchicalMultiplierId;
    public long cid;
    public Long pid;
    public HierarchicalMultipliersType type;

    public static BinlogEvent createMultiplierEvent(List<HierarchicalMultipliersChange> changes, Operation operation) {
        BinlogEvent binlogEvent =
                new BinlogEvent().withTable(HIERARCHICAL_MULTIPLIERS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = changes.stream()
                .map(change -> createHierarchicalMultiplierTableRow(change, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createHierarchicalMultiplierTableRow(
            HierarchicalMultipliersChange change, Operation operation) {
        Map<String, Object> primaryKeys = Map.of(
                HIERARCHICAL_MULTIPLIERS.HIERARCHICAL_MULTIPLIER_ID.getName(),
                BigInteger.valueOf(change.hierarchicalMultiplierId));

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(HIERARCHICAL_MULTIPLIERS.CID.getName(), change.cid);
            before.put(HIERARCHICAL_MULTIPLIERS.TYPE.getName(), change.type.getLiteral());

            if (change.pid != null) {
                before.put(HIERARCHICAL_MULTIPLIERS.PID.getName(), change.pid);
            }
        }
        if (!operation.equals(DELETE)) {
            after.put(HIERARCHICAL_MULTIPLIERS.CID.getName(), change.cid);
            after.put(HIERARCHICAL_MULTIPLIERS.TYPE.getName(), change.type.getLiteral());

            if (change.pid != null) {
                after.put(HIERARCHICAL_MULTIPLIERS.PID.getName(), change.pid);
            }
        }
        fillChangedInRow(before, after, change.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public HierarchicalMultipliersChange withHierarchicalMultiplierId(long hierarchicalMultiplierId) {
        this.hierarchicalMultiplierId = hierarchicalMultiplierId;
        return this;
    }

    public HierarchicalMultipliersChange withCid(long cid) {
        this.cid = cid;
        return this;
    }

    public HierarchicalMultipliersChange withPid(Long pid) {
        this.pid = pid;
        return this;
    }

    public HierarchicalMultipliersChange withType(HierarchicalMultipliersType type) {
        this.type = type;
        return this;
    }
}
