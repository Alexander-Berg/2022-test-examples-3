package ru.yandex.direct.grid.core.entity.strategy.repository

import ru.yandex.direct.grid.schema.yt.Tables
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

object YtRowStatsUtils {
    data class Row(
        val strategyId: Long,
        val date: LocalDate,
        val goalNum: Long,
        val priceCur: Long,
        val goalId: Long? = null
    ) {
        fun rowBuilder(): RowBuilder {
            val builder = RowBuilder()
                .withColValue("EffectiveStrategyId", strategyId)
                .withColValue(
                    Tables.DIRECTGRIDGOALSSTAT_BS.UPDATE_TIME.name,
                    date.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC)
                )
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM.name, goalNum)
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR.name, priceCur)
            if (goalId != null) {
                builder.withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOAL_ID.name, goalId)
            }
            return builder
        }
    }

    fun rowset(rows: List<Row>): UnversionedRowset =
        rows.fold(RowsetBuilder()) { builder, row ->
            builder.add(row.rowBuilder())
        }.build()
}
