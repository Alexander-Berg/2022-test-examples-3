package ru.yandex.market.replenishment.autoorder.repository

import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderAssortmentLoadRepository

class TenderAssortmentLoadRepositoryTest : FunctionalTest() {

    @Autowired
    private lateinit var tenderAssortmentLoadRepository: TenderAssortmentLoadRepository

    @Test
    @DbUnitDataSet(before = ["TenderAssortmentLoadRepositoryTest_getLastByDemandIdAndSupplierId.before.csv"])
    fun testGetLastByDemandIdAndSupplierId() {
        val load = tenderAssortmentLoadRepository.getLastByDemandIdAndSupplierId(1L, 2L)
        Assertions.assertNotNull(load)
        Assertions.assertEquals("key 3", load!!.s3Key)
        Assertions.assertEquals(300, load.mboRequestId)
    }
}
