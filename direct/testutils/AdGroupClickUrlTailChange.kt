package ru.yandex.direct.ess.router.testutils

import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow

data class GroupParamsTableChange(
    val pid: Long,
) : BaseTableChange()

private fun createAdGroupParamsTableRow(
    change: GroupParamsTableChange,
    op: Operation
): BinlogEvent.Row {
    val primaryKeys = mapOf(Tables.GROUP_PARAMS.PID.name to change.pid)
    val before = mutableMapOf<String, Any>()
    val after = mutableMapOf<String, Any>()
    fillChangedInRow(before, after, change.changedColumns, op)
    return BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
}

fun createGroupParamsEvent(
    changes: List<GroupParamsTableChange>,
    op: Operation
): BinlogEvent {
    return BinlogEvent().apply {
        table = Tables.GROUP_PARAMS.name
        operation = op
        rows = changes
            .map { createAdGroupParamsTableRow(it, operation) }
    }
}
