package ru.yandex.direct.ess.router.testutils

import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables

data class AggrStatusesCampaignsTableChange(
    val cid: Long,
) : BaseTableChange()

private fun createdAggrStatusesCampaignsTableRow(
    tableChange: AggrStatusesCampaignsTableChange,
    operation: Operation
): BinlogEvent.Row {
    val primaryKeys = mapOf(Tables.AGGR_STATUSES_CAMPAIGNS.CID.name to tableChange.cid)
    val before = mutableMapOf<String, Any>()
    val after = mutableMapOf<String, Any>()
    if (operation != Operation.INSERT) {
        before[Tables.AGGR_STATUSES_CAMPAIGNS.CID.name] = tableChange.cid
    }
    if (operation != Operation.DELETE) {
        after[Tables.AGGR_STATUSES_CAMPAIGNS.CID.name] = tableChange.cid
    }
    TestUtils.fillChangedInRow(before, after, tableChange.changedColumns, operation)
    return BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
}

fun createdAggrStatusesCampaignsEvent(
    tableChanges: List<AggrStatusesCampaignsTableChange>,
    operation: Operation
): BinlogEvent {
    val binlogEvent = BinlogEvent().withTable(Tables.AGGR_STATUSES_CAMPAIGNS.name).withOperation(operation)
    val rows = tableChanges
        .map { tableChange -> createdAggrStatusesCampaignsTableRow(tableChange, operation) }
    binlogEvent.withRows(rows)
    return binlogEvent
}

