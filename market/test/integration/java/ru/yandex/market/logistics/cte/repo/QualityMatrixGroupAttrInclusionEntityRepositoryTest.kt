package ru.yandex.market.logistics.cte.repo

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.dto.QualityMatrixDTO
import ru.yandex.market.logistics.cte.client.enums.ApiField
import ru.yandex.market.logistics.cte.client.enums.EnumerationOrder
import ru.yandex.market.logistics.cte.client.enums.MatrixType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeValueType
import java.time.ZonedDateTime

class QualityMatrixGroupAttrInclusionEntityRepositoryTest(
    @Autowired private val qualityMatrixGroupRepository: QualityMatrixGroupsJdbcRepository
): IntegrationTest() {

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/qmatrix_group.xml"),
        DatabaseSetup("classpath:repository/quality_attribute_inclusion.xml"),
    )
    fun findAllMatrixRsql() {
        val list = qualityMatrixGroupRepository.findAll(ApiField.MATRIX_ID, EnumerationOrder.ASC, "", 5, 0, false)
        val count = qualityMatrixGroupRepository.getQualityMatrixDTOCount("", false)

        assertions.assertThat(expectedMatrix()).isEqualTo(list)
    }

    fun expectedMatrix(): List<QualityMatrixDTO> {
        return listOf(
            QualityMatrixDTO(
                1,
                0,
                "default",
                6,
                "WRONG_OR_DAMAGED_LABELS",
                "WRONG_OR_DAMAGED_LABELS",
                QualityAttributeType.ITEM,
                "Повреждена этикетка (Отсутствие или нарушение заводской этикетки, печатных изданий)",
                QualityAttributeValueType.ENABLED,
                ZonedDateTime.parse("2021-12-08T10:55:45+03:00[Europe/Moscow]"),
                MatrixType.RETURNS
            ),
            QualityMatrixDTO(
                1,
                0,
                "default",
                4,
                "PACKAGE_HOLES",
                "PACKAGE_HOLES",
                QualityAttributeType.PACKAGE,
                "Отверстия (Исключающие повреждение товара, утерю комплектующих)",
                QualityAttributeValueType.ENABLED,
                ZonedDateTime.parse("2021-12-08T10:55:45+03:00[Europe/Moscow]"),
                MatrixType.RETURNS
            )
        )
    }
}
