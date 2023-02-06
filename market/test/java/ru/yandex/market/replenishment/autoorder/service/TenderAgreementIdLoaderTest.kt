package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.TenderAgreementIdLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class TenderAgreementIdLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: TenderAgreementIdLoader

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/axapta_erp/ShareData/Contracts/Contracts"
            ],
            csv = "TenderAgreementIdLoaderTest.yql.csv",
            yqlMock = "TenderAgreementIdLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["TenderAgreementIdLoaderTest.before.csv"],
            after = ["TenderAgreementIdLoaderTest.after.csv"],
    )
    fun testLoad_isOk() {
        loader.load()
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/axapta_erp/ShareData/Contracts/Contracts"
            ],
            csv = "TenderAgreementIdLoaderTest.yql.csv",
            yqlMock = "TenderAgreementIdLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["TenderAgreementIdLoaderTest.before.csv"],
            after = ["TenderAgreementIdLoaderTest.after.csv"],
    )
    fun testDoubleLoad_isOk() {
        loader.load()
        loader.load()
    }
}
