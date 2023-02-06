package ru.yandex.direct.ess.router.testutils

import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import java.util.Map

data class MinusWordsTableChange(val minusWordId: Long) : BaseTableChange()

private fun createdMinusWordsTableRow(tableChange: MinusWordsTableChange,
                                      operation: Operation): BinlogEvent.Row {
    val primaryKeys = Map.of<String, Any>(Tables.MINUS_WORDS.MW_ID.name, tableChange.minusWordId)
    val before = mutableMapOf<String, Any>()
    val after = mutableMapOf<String, Any>()
    if (operation != Operation.INSERT) {
        before[Tables.MINUS_WORDS.MW_ID.name] = tableChange.minusWordId
    }
    if (operation != Operation.DELETE) {
        after[Tables.MINUS_WORDS.MW_ID.name] = tableChange.minusWordId
    }
    TestUtils.fillChangedInRow(before, after, tableChange.changedColumns, operation)
    return BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
}

fun createdMinusWordsEvent(tableChanges: List<MinusWordsTableChange>,
                           operation: Operation): BinlogEvent {
    val binlogEvent = BinlogEvent().withTable(Tables.MINUS_WORDS.name).withOperation(operation)
    val rows = tableChanges
        .map { tableChange -> createdMinusWordsTableRow(tableChange, operation) }
    binlogEvent.withRows(rows)
    return binlogEvent
}
