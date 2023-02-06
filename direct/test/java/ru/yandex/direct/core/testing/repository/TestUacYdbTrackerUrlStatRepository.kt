package ru.yandex.direct.core.testing.repository

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Repository
import ru.yandex.direct.common.configuration.UacYdbConfiguration
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbTrackerUrlStatRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CounterType
import ru.yandex.direct.core.entity.uac.repository.ydb.model.TrackerAppEvent
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.TRACKER_URL_STAT
import ru.yandex.direct.ydb.YdbPath
import ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder
import ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder
import ru.yandex.direct.ydb.client.YdbClient
import ru.yandex.direct.ydb.table.temptable.TempTableBuilder

data class UacYdbTrackerUrlStat(
    val id: String = UacYdbUtils.generateUniqueRandomId(),
    val updateTime: Long,
    val trackerUrl: String,
    val counterType: CounterType,
    val hitCount: Long,
    val conversionsByEvent: Map<TrackerAppEvent, Long>
)

@Lazy
@Repository
class TestUacYdbTrackerUrlStatRepository(
    @Qualifier(UacYdbConfiguration.UAC_YDB_CLIENT_BEAN) var ydbClient: YdbClient,
    @Qualifier(UacYdbConfiguration.UAC_YDB_PATH_BEAN) var path: YdbPath,
) {
    fun insertStat(uacYdbTrackerUrlStat: UacYdbTrackerUrlStat) {
        val insertValues = TempTableBuilder.buildTempTable {
            value(TRACKER_URL_STAT.ID, uacYdbTrackerUrlStat.id.toIdLong())
            value(TRACKER_URL_STAT.UPDATE_TIME, uacYdbTrackerUrlStat.updateTime)
            value(TRACKER_URL_STAT.TRACKER_URL, uacYdbTrackerUrlStat.trackerUrl)
            value(TRACKER_URL_STAT.COUNTER_TYPE, uacYdbTrackerUrlStat.counterType.id.toLong())
            value(TRACKER_URL_STAT.HIT_COUNT, uacYdbTrackerUrlStat.hitCount)
            UacYdbTrackerUrlStatRepository.COLUMN_BY_TRACKER_APP_EVENT.forEach {
                value(it.value, uacYdbTrackerUrlStat.conversionsByEvent[it.key] ?: 0L)
            }
        }

        val queryAndParams = InsertBuilder.insertInto(TRACKER_URL_STAT)
            .selectAll()
            .from(insertValues)
            .queryAndParams(path)

        ydbClient.executeQuery(queryAndParams, true)
    }

    fun clean() {
        val queryAndParams = DeleteBuilder.deleteFrom(TRACKER_URL_STAT).queryAndParams(path)
        ydbClient.executeQuery(queryAndParams, true)
    }
}
