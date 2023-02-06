package ru.yandex.market.logistics.cte.repo

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.enums.MatrixType

class QualityGroupRepositoryTest(
    @Autowired private val qualityGroupRepo: QualityGroupRepository) : IntegrationTest() {

    @DatabaseSetup("classpath:repository/quality_group/before.xml")
    @Test
    fun test() {
        val found = qualityGroupRepo.findAllAttributesByGroupAndMatrixType(1, MatrixType.FULFILLMENT)

        Assertions.assertEquals(2, found.size)
    }

    @DatabaseSetup("classpath:repository/quality_group/before.xml")
    @Test
    fun testGroup() {
        val found = qualityGroupRepo.findAllAttributesByGroup(1)

        Assertions.assertEquals(2, found.attributes.size)
    }
}
