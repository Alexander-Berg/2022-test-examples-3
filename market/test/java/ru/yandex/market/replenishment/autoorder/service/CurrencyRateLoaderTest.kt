package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CurrencyRateLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class CurrencyRateLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: CurrencyRateLoader

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/axapta_erp/support/CurrencyExchangeRate"
        ],
        csv = "CurrencyRateLoaderTest.yql.csv",
        yqlMock = "CurrencyRateLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["CurrencyRateLoaderTest.before.csv"],
        after = ["CurrencyRateLoaderTest.after.csv"],
    )
    fun testLoad_isOk() {
        loader.load()
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/axapta_erp/support/CurrencyExchangeRate"
        ],
        csv = "CurrencyRateLoaderTest.yql.csv",
        yqlMock = "CurrencyRateLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["CurrencyRateLoaderTest.before.csv"],
        after = ["CurrencyRateLoaderTest.after.csv"],
    )
    fun testDoubleLoad_isOk() {
        loader.load()
        loader.load()
    }
}
