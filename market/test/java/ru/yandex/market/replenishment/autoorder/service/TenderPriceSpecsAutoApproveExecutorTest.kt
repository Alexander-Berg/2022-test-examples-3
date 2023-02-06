package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.TenderPriceSpecsAutoApproveLoader

open class TenderPriceSpecsAutoApproveExecutorTest: FunctionalTest() {

    @Autowired
    lateinit var tenderPriceSpecsAutoApproveLoader: TenderPriceSpecsAutoApproveLoader

    @Test
    @DbUnitDataSet(
            before = ["TenderPriceSpecsAutoApproveExecutorTest.before.csv"],
            after = ["TenderPriceSpecsAutoApproveExecutorTest.after.csv"],
    )
    open fun testTenderPriceSpecsAutoApproveLoader() {
        tenderPriceSpecsAutoApproveLoader.load()
    }
}
