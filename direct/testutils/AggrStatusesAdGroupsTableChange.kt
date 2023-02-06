package ru.yandex.direct.ess.router.testutils

import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables

data class AggrStatusesAdGroupsTableChange(
    val pid: Long,
) : BaseTableChange()

private fun createdAggrStatusesAdGroupsTableRow(
    tableChange: AggrStatusesAdGroupsTableChange,
    operation: Operation
): BinlogEvent.Row {
    val primaryKeys = mapOf(Tables.AGGR_STATUSES_ADGROUPS.PID.name to tableChange.pid)
    val before = mutableMapOf<String, Any>()
    val after = mutableMapOf<String, Any>()
    if (operation != Operation.INSERT) {
        before[Tables.AGGR_STATUSES_ADGROUPS.PID.name] = tableChange.pid
    }
    if (operation != Operation.DELETE) {
        after[Tables.AGGR_STATUSES_ADGROUPS.PID.name] = tableChange.pid
    }
    TestUtils.fillChangedInRow(before, after, tableChange.changedColumns, operation)
    return BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
}

fun createdAggrStatusesAdGroupsEvent(
    tableChanges: List<AggrStatusesAdGroupsTableChange>,
    operation: Operation
): BinlogEvent {
    val binlogEvent = BinlogEvent().withTable(Tables.AGGR_STATUSES_ADGROUPS.name).withOperation(operation)
    val rows = tableChanges
        .map { tableChange -> createdAggrStatusesAdGroupsTableRow(tableChange, operation) }
    binlogEvent.withRows(rows)
    return binlogEvent
}
