package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RetroBonusLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class RetroBonusLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: RetroBonusLoader

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/axapta_erp/BI/TAMVendRebate/RebateAgreement",
                "//home/market/production/axapta_erp/BI/TAMVendRebate/RebateAgreementLine"
            ],
            csv = "RetroBonusLoaderTest.yql.csv",
            yqlMock = "RetroBonusLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["RetroBonusLoaderTest.before.csv"],
            after = ["RetroBonusLoaderTest.after.csv"],
    )
    fun testLoad_isOk() {
        loader.load()
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/axapta_erp/BI/TAMVendRebate/RebateAgreement",
                "//home/market/production/axapta_erp/BI/TAMVendRebate/RebateAgreementLine"
            ],
            csv = "RetroBonusLoaderTest.yql.csv",
            yqlMock = "RetroBonusLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["RetroBonusLoaderTest.before.csv"],
            after = ["RetroBonusLoaderTest.after.csv"],
    )
    fun testDoubleLoad_isOk() {
        loader.load()
        loader.load()
    }
}
