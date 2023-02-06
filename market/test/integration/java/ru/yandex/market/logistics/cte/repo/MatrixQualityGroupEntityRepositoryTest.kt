package ru.yandex.market.logistics.cte.repo

import javax.transaction.Transactional
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.enums.MatrixType

class MatrixQualityGroupEntityRepositoryTest(
    @Autowired private val matrixRepo: MatrixQualityGroupEntityRepository) : IntegrationTest() {

    @Transactional
    @DatabaseSetup("classpath:repository/matrix_quality_group/before.xml")
    @Test
    fun test() {
        val found = matrixRepo.findFirstByGroupIdAndMatrixTypeOrderByUpdatedAtDesc(1, MatrixType.FULFILLMENT)

        Assertions.assertEquals(2, found!!.group!!.attributes.size)
    }
}
