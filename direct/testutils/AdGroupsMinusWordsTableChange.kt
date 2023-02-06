package ru.yandex.direct.ess.router.testutils

import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import java.util.Map

data class AdGroupsMinusWordsTableChange(val pid: Long) : BaseTableChange()

private fun createdAdGroupsMinusWordsTableRow(tableChange: AdGroupsMinusWordsTableChange,
                                            operation: Operation): BinlogEvent.Row {
    val primaryKeys = Map.of<String, Any>(Tables.ADGROUPS_MINUS_WORDS.PID.name, tableChange.pid)
    val before = mutableMapOf<String, Any>()
    val after = kotlin.collections.mutableMapOf<String, Any>()
    if (operation != Operation.INSERT) {
        before[Tables.ADGROUPS_MINUS_WORDS.PID.name] = tableChange.pid
    }
    if (operation != Operation.DELETE) {
        after[Tables.ADGROUPS_MINUS_WORDS.PID.name] = tableChange.pid
    }
    TestUtils.fillChangedInRow(before, after, tableChange.changedColumns, operation)
    return BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
}

fun createdAdGroupsMinusWordsEvent(tableChanges: List<AdGroupsMinusWordsTableChange>,
                                   operation: Operation): BinlogEvent {
    val binlogEvent = BinlogEvent().withTable(Tables.ADGROUPS_MINUS_WORDS.name).withOperation(operation)
    val rows = tableChanges
        .map { tableChange -> createdAdGroupsMinusWordsTableRow(tableChange, operation) }
    binlogEvent.withRows(rows)
    return binlogEvent
}
