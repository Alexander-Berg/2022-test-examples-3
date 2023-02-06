package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.ExtendedDemandInfoLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class ExtendedDemandInfoLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: ExtendedDemandInfoLoader

    @Test
    @DbUnitDataSet(
        before = ["ExtendedDemandInfoLoaderTest_extendedInfoUpdate.before.csv"],
        after = ["ExtendedDemandInfoLoaderTest_extendedInfoUpdate.after.csv"],
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/axapta_purchases/latest",
            "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/latest",
            "//home/market/production/mstat/dictionaries/fulfillment_request_status_history/latest",
            "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/latest",
        ],
        csv = "ExtendedDemandInfoLoaderTest.yql.csv",
        yqlMock = "ExtendedDemandInfoLoaderTest.yql.mock",
    )
    fun extendedInfoUpdate() {
        loader.load()
    }
}
