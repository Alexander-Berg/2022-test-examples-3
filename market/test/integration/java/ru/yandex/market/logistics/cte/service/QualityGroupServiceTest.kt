package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO
import ru.yandex.market.logistics.cte.client.enums.MatrixType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeValueType
import ru.yandex.market.logistics.cte.entity.category.CategoryInfo
import ru.yandex.market.logistics.cte.entity.parser.QualityAttributeValueDTO

internal class QualityGroupServiceTest(
    @Autowired private val qualityGroupService: QualityGroupService
) : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/quality-matrix/quality_group_category_update/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/quality-matrix/quality_group_category_update/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun saveCheckUpdateOfCategoriesWithoutGroupId() {
        qualityGroupService.getQualityGroupIdOrDefault(5, "GROUP 5")

        val attributes = listOf(
            QualityAttributeValueDTO(
                QualityAttributeDTO(1, "", "", "", QualityAttributeType.ITEM, ""),
                QualityAttributeValueType.ENABLED
            )
        )

        val categories = listOf(CategoryInfo(id = 1), CategoryInfo(id = 2), CategoryInfo(id = 3))

        qualityGroupService.save(5, MatrixType.RETURNS, categories, attributes)
    }

    @Test
    @DatabaseSetup(
        "classpath:service/quality-matrix/duplicated_categories_attributes/quality_matrix_missing_categories_and_attributes.xml")
    fun getDefaultQualityGroupId() {
        val qualityGroupId = qualityGroupService.getQualityGroupIdOrDefault(DEFAULT_QUALITY_GROUP_ID, "")

        assertions.assertThat(qualityGroupId).isEqualTo(0)
    }

    /*
        Проверка мерджа атрибутов качества для уже существующей матрицы группы качеств
        Есть матрицы группы качеств с атрибутами и их оценками
        [qualityAttribute1 -> UTIL, qualityAttribute2 -> UTIL]
        Загружается матрица, где второй атрибут имеет новое значение и добавлется третий атрибут.
        Итоговая матрица содержит
        [qualityAttribute1 -> UTIL, qualityAttribute2 -> SERVICE_CENTER, qualityAttribute3 -> ENABLED]
     */
    @Test
    @DatabaseSetup("classpath:service/quality-matrix/check_merge/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/quality-matrix/check_merge/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun saveQualityAttributesValuesAndMergeWithExistedAttributeValues() {
        qualityGroupService.getQualityGroupIdOrDefault(20, "GROUP 20")

        val attributes = listOf(
            QualityAttributeValueDTO(
                QualityAttributeDTO(2, "", "", "", QualityAttributeType.ITEM, ""),
                QualityAttributeValueType.ASC
            ),
            QualityAttributeValueDTO(
                QualityAttributeDTO(3, "", "", "", QualityAttributeType.ITEM, ""),
                QualityAttributeValueType.ENABLED
            )
        )

        val categories = listOf(CategoryInfo(id = 1))

        qualityGroupService.save(20, MatrixType.RETURNS, categories, attributes)
    }
}
