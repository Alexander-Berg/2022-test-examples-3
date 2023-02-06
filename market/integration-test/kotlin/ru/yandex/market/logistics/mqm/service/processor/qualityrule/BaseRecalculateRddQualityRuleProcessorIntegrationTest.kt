package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.additionaldata.RecalculationRddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.service.PlanFactService

class BaseRecalculateRddQualityRuleProcessorIntegrationTest: AbstractContextualTest() {

    @Autowired
    private lateinit var planFactService: PlanFactService

    @DisplayName("Тест обратной совместимости RecalculationRddPlanFactAdditionalData")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/recalculate_rdd/base/before/old_plan_fact.xml")
    fun recalculateAdditionalDataBackwardCompatibility() {
        val planFact = planFactService.getByIdOrThrow(1)
        val additionalData = planFact.getData(RecalculationRddPlanFactAdditionalData::class.java)!!
        assertSoftly {
            additionalData.delayInvocation shouldBe true
            additionalData.factInvocation shouldBe true
            additionalData.nextSegmentInInvocation shouldBe false
        }
    }
}
