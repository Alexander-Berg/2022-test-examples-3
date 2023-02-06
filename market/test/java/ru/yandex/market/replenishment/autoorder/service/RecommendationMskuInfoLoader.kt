package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDateTime

private val NOW_DATE_TIME = LocalDateTime.of(2020, 5, 15, 12, 0)

class RecommendationMskuInfoLoaderTest : FunctionalTest() {

    @Autowired
    private lateinit var recommendationsMskuInfoLoader: RecommendationsMskuInfoLoader

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/msku_info",
        ],
        csv = "RecommendationMskuInfoLoaderTest.yql.csv",
        yqlMock = "RecommendationMskuInfoLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["RecommendationMskuInfoLoaderTest.before.csv"],
        after = ["RecommendationMskuInfoLoaderTest.after.csv"],
    )
    fun testLoader() {
        setTestTime(NOW_DATE_TIME)
        recommendationsMskuInfoLoader.load()
    }
}
