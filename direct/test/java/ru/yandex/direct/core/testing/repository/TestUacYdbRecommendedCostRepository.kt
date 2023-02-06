package ru.yandex.direct.core.testing.repository

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Repository
import ru.yandex.direct.common.configuration.UacYdbConfiguration
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbRecommendedCost
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.RECOMMENDED_COST
import ru.yandex.direct.ydb.YdbPath
import ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder
import ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder
import ru.yandex.direct.ydb.client.YdbClient
import ru.yandex.direct.ydb.table.temptable.TempTableBuilder

@Lazy
@Repository
class TestUacYdbRecommendedCostRepository(
    @Qualifier(UacYdbConfiguration.UAC_YDB_CLIENT_BEAN) var ydbClient: YdbClient,
    @Qualifier(UacYdbConfiguration.UAC_YDB_PATH_BEAN) var path: YdbPath,
) {
    fun addRecommendedCost(recommendedCost: UacYdbRecommendedCost) {
        val insertValue = TempTableBuilder.buildTempTable {
            value(RECOMMENDED_COST.ID, recommendedCost.id.toIdLong())
            value(RECOMMENDED_COST.CATEGORY, recommendedCost.category)
            value(RECOMMENDED_COST.RECOMMENDED_COST, recommendedCost.recommendedCost)
            value(RECOMMENDED_COST.PLATFORM, recommendedCost.platform.id)
            value(RECOMMENDED_COST.TYPE, recommendedCost.type.id)
        }

        val queryAndParams = InsertBuilder.insertInto(RECOMMENDED_COST)
            .selectAll()
            .from(insertValue)
            .queryAndParams(path)

        ydbClient.executeQuery(queryAndParams, true)
    }

    fun clean() {
        val queryAndParams = DeleteBuilder
            .deleteFrom(RECOMMENDED_COST)
            .queryAndParams(path)
        ydbClient.executeQuery(queryAndParams, true)
    }
}
