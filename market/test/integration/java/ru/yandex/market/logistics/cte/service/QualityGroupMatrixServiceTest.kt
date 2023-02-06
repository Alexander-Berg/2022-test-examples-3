package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.enums.MatrixType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeValueType

internal class QualityGroupMatrixServiceTest(
    @Autowired private val qualityGroupMatrixService: QualityGroupMatrixService
) : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/quality-group-matrix/happy_path/before.xml")
    fun getLastByCategoryIdTest() {
        val lastByCategoryId = qualityGroupMatrixService.getLatestByCategoryIdAndMatrixType(6,
            MatrixType.RETURNS)

        requireNotNull(lastByCategoryId)

        assertions.assertThat(lastByCategoryId.entries.size).isEqualTo(1)

        assertions.assertThat(lastByCategoryId.entries.first { true }.value).isEqualTo(QualityAttributeValueType.ASC)
    }

    @Test
    @DatabaseSetup("classpath:service/quality-group-matrix/happy_path/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/quality-group-matrix/happy_path/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun getLastByCategoryIdWithNewCategoryId() {
        val lastByCategoryId = qualityGroupMatrixService.getLatestByCategoryIdAndMatrixType(7,
            MatrixType.RETURNS)

        requireNotNull(lastByCategoryId)

        assertions.assertThat(lastByCategoryId.entries.size).isEqualTo(1)

        assertions.assertThat(lastByCategoryId.entries.first { true }.value).isEqualTo(QualityAttributeValueType.ASC)
    }
}
