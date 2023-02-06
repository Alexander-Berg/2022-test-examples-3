package ru.yandex.market.wms.constraints.integration

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.isA
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.dto.StartrekIssueDto
import ru.yandex.market.wms.core.base.dto.SkuCharacteristic
import ru.yandex.market.wms.core.base.dto.SkuCharacteristicType
import ru.yandex.market.wms.core.base.dto.SkuSpecification
import ru.yandex.market.wms.core.base.response.SkuCharacteristicsResponse

class MDMServiceIntegrationTest : ConstraintsIntegrationTest() {

    @Autowired
    private lateinit var underTest: MDMService

    @Autowired
    @MockBean
    private lateinit var jmsTemplate: JmsTemplate

    @AfterEach
    fun reset() {
        reset(jmsTemplate)
    }

    @Test
    @DatabaseSetup("/integration/mdm/common-before.xml")
    @DatabaseSetups(
        DatabaseSetup("/integration/mdm/common-before.xml"),
        DatabaseSetup("/integration/mdm/base-issues/before.xml"),
    )
    @ExpectedDatabase(
        value = "/integration/mdm/base-issues/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `send base issues and update status`() {
        val skus = sampleSkus()
        val response = sampleResponse(skus)

        whenever(coreClient.getSkuCharacteristics(isA()))
            .thenReturn(response)

        val processedIssueCount = underTest.createTicketForCargoTypes()

        assertThat(processedIssueCount).isEqualTo(2)

        argumentCaptor<Any> {
            verify(jmsTemplate).convertAndSend(isA<String>(), capture(), isA())

            assertThat(this.firstValue).isExactlyInstanceOf(StartrekIssueDto::class.java)
            assertThat((this.firstValue as StartrekIssueDto).isStorageCategoryIssue).isFalse()
        }
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/integration/mdm/common-before.xml"),
        DatabaseSetup("/integration/mdm/storage-category-issues/before.xml"),
    )
    @ExpectedDatabase(
        value = "/integration/mdm/storage-category-issues/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `send base and storage category issues`() {
        val skus = sampleSkus()
        val response = sampleResponse(skus)

        whenever(coreClient.getSkuCharacteristics(isA()))
            .thenReturn(response)

        val processedIssueCount = underTest.createTicketForCargoTypes()

        assertThat(processedIssueCount).isEqualTo(3)

        argumentCaptor<Any> {
            verify(jmsTemplate, times(2)).convertAndSend(isA<String>(), capture(), isA())

            val firstDto = (this.firstValue as StartrekIssueDto)
            val secondDto = (this.secondValue as StartrekIssueDto)
            assertThat(firstDto.isStorageCategoryIssue)
                .isNotEqualTo(secondDto.isStorageCategoryIssue)
        }
    }
}

private fun sampleSkus() = listOf(
    SkuId("465852", "ROV000897823"),
    SkuId("123456", "ROV000100011"),
)

private fun sampleResponse(skus: List<SkuId>) = SkuCharacteristicsResponse(
    listOf(
        SkuSpecification(
            skus.component1().sku, skus.component1().storerKey, null, null, listOf(
                SkuCharacteristic(SkuCharacteristicType.CARGO_TYPE, "150"),
                SkuCharacteristic(SkuCharacteristicType.LENGTH, "50"),
                SkuCharacteristic(SkuCharacteristicType.WIDTH, "10.500"),
                SkuCharacteristic(SkuCharacteristicType.HEIGHT, "3"),
                SkuCharacteristic(SkuCharacteristicType.WEIGHT, "34.060"),
                SkuCharacteristic(SkuCharacteristicType.VOLUME, "0.0"), // будет исключено
            )
        ),
        SkuSpecification(
            skus.component2().sku, skus.component2().storerKey, "MSKU00238", "Товар 1", listOf(
                SkuCharacteristic(SkuCharacteristicType.CARGO_TYPE, "390"),
                SkuCharacteristic(SkuCharacteristicType.LENGTH, "5"),
                SkuCharacteristic(SkuCharacteristicType.HEIGHT, "2"),
            )
        ),
    )
)
