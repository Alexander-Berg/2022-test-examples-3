package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SupplierPaymentDelayLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class SupplierPaymentDelayLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: SupplierPaymentDelayLoader

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/axapta_erp/ShareData/Contracts/Contracts"
            ],
            csv = "SupplierPaymentDelayLoaderTest.yql.csv",
            yqlMock = "SupplierPaymentDelayLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["SupplierPaymentDelayLoaderTest.before.csv"],
            after = ["SupplierPaymentDelayLoaderTest.after.csv"],
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
            csv = "SupplierPaymentDelayLoaderTest.yql.csv",
            yqlMock = "SupplierPaymentDelayLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["SupplierPaymentDelayLoaderTest.before.csv"],
            after = ["SupplierPaymentDelayLoaderTest.after.csv"],
    )
    fun testDoubleLoad_isOk() {
        loader.load()
        loader.load()
    }
}
