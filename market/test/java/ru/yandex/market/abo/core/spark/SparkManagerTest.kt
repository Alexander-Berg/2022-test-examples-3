package ru.yandex.market.abo.core.spark

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class SparkManagerTest @Autowired constructor(
    val sparkManager: SparkManager,
) : EmptyTest() {

    /**
     * зависит от файлов GetCompanyExtendedReport.xml, CheckCompanyStatus.xml
     */
    @Test
    fun testCheckStatusAndUpdate() {
        var data = sparkManager.loadFromSparkAndUpdate(SparkServiceTest.TEST_OGRN_OOO)
        Assertions.assertTrue(data!!.isActing)
        Assertions.assertEquals(24, data.statusCode)
        sparkManager.checkStatusAndUpdate(data)

        data = sparkManager.getSparkShopDataFromDbBySparkId(data.sparkId)
        Assertions.assertFalse(data.isActing)
        Assertions.assertEquals(38, data.statusCode)
    }
}
