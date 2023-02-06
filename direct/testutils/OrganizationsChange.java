package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.ORGANIZATIONS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class OrganizationsChange extends BaseTableChange {
    public Long permalinkId;

    public static BinlogEvent createOrganizationsEvent(List<OrganizationsChange> organizationsChanges,
                                                       Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(ORGANIZATIONS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = organizationsChanges.stream()
                .map(organizationsChange -> createOrganizationsTableRow(organizationsChange, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createOrganizationsTableRow(OrganizationsChange organizationsChange,
                                                               Operation operation) {
        Map<String, Object> primaryKeys = Map.of(ORGANIZATIONS.PERMALINK_ID.getName(), organizationsChange.permalinkId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, organizationsChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public OrganizationsChange withPermalinkId(Long permalinkId) {
        this.permalinkId = permalinkId;
        return this;
    }
}
