package ru.yandex.market.logistics.cte.service.impl

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.dto.QualityAttributeRequestDTO
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType
import ru.yandex.market.logistics.cte.models.QualityAttributeKey
import ru.yandex.market.logistics.cte.models.exception.ConflictException
import ru.yandex.market.logistics.cte.service.QualityAttributeService

internal class QualityAttributeServiceTest(
    @Autowired val qualityAttributeService: QualityAttributeService
): IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/quality-attribute/quality_attributes_with_or_without_quality_group.xml")
    fun getQualityGroupAttributes() {

        val qualityGroupAttributes = qualityAttributeService.findQualityGroupAttributes(
            1, setOf(
                QualityAttributeKey("1.1", "name1"),
                QualityAttributeKey("1.2", "name2")
            )
        )

        assertions.assertThat(qualityGroupAttributes).hasSize(2)
        assertions.assertThat(qualityGroupAttributes[0].id).isEqualTo(1)
        assertions.assertThat(qualityGroupAttributes[1].id).isEqualTo(3)
    }

    @Test
    @DatabaseSetup("classpath:service/quality_attribute.xml")
    @ExpectedDatabase(value = "classpath:service/quality_attribute.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(Exception::class)
    fun addQualityAttributeWithDuplicateNameAndRefId() {
        val qualityAttributeRequestDTO = QualityAttributeRequestDTO(
            "PACKAGE_CONTAMINATION", "PACKAGE_CONTAMINATION", "1.3", QualityAttributeType.PACKAGE,
            "Загрязнения, следы влаги (Пятна, протечки)"
        )
        Assertions.assertThrows(
            ConflictException::class.java
        ) { qualityAttributeService.save(qualityAttributeRequestDTO) }
    }

}
