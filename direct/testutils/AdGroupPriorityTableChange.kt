package ru.yandex.direct.ess.router.testutils

import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables

data class AdGroupPriorityTableChange(
    val pid: Long,
) : BaseTableChange()

private fun createAdGroupsMinusWordsTableRow(
    change: AdGroupPriorityTableChange,
    op: Operation
): BinlogEvent.Row {
    val primaryKeys = mapOf(Tables.ADGROUP_PRIORITY.PID.name to change.pid)
    val before = mutableMapOf<String, Any>()
    val after = mutableMapOf<String, Any>()
    if (op != Operation.INSERT) {
        before[Tables.ADGROUP_PRIORITY.PID.name] = change.pid
    }
    if (op != Operation.DELETE) {
        after[Tables.ADGROUP_PRIORITY.PID.name] = change.pid
    }
    TestUtils.fillChangedInRow(before, after, change.changedColumns, op)
    return BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
}

fun createAdGroupPriorityEvent(
    changes: List<AdGroupPriorityTableChange>,
    op: Operation
): BinlogEvent {
    return BinlogEvent().apply {
        table = Tables.ADGROUP_PRIORITY.name
        operation = op
        rows = changes
            .map { createAdGroupsMinusWordsTableRow(it, operation) }
    }
}
