package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto
import java.time.Instant

class RecalculatedRddNotificationQualityRuleProcessorIntegrationTest: StartrekProcessorTest(){
    @Autowired
    private lateinit var lomClient: LomClient

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2022-01-28T19:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Интеграционный тест положительного сценария")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/recalculate_rdd/final/before/set_up.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/recalculate_rdd/final/after/result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun success() {
        mockLomClient()
        handlePlanFacts()
        verify(lomClient).preDeliveryRddRecalculation(any())
    }

    private fun mockLomClient() {
        val dto = ChangeOrderRequestDto.builder().id(321).build()
        whenever(lomClient.preDeliveryRddRecalculation(any())).thenReturn(dto)
    }
}
