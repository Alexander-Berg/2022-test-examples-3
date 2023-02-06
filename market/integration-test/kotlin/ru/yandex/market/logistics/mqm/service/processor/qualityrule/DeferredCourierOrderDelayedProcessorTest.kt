package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.service.handler.QualityRuleHandler
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("Тест процессора для перевода заказа в тип OnDemand")
class DeferredCourierOrderDelayedProcessorTest : AbstractContextualTest() {

    private val fixedTime = Instant.parse("2021-01-07T12:00:00Z")

    @Autowired
    @Qualifier("planFactHandler")
    private lateinit var planFactHandler: QualityRuleHandler

    @Autowired
    lateinit var processor: DeferredCourierOrderDelayedProcessor

    @Autowired
    lateinit var lomClient: LomClient

    @BeforeEach
    fun setup() {
        clock.setFixed(fixedTime, ZoneOffset.UTC)
    }

    @Test
    fun canProcess() {
        QualityRuleProcessorType.values().forEach {
            val isEligible = it === QualityRuleProcessorType.LOM_CHANGE_REQUEST
            assertSoftly {
                processor.canProcess(QualityRule().apply { ruleProcessor = it }) shouldBe isEligible
            }
        }
    }

    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/deferred_courier_order.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/deferred_courier_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun process() {
        planFactHandler.handle(listOf(1), fixedTime)
        verify(lomClient).changeOrderToOnDemand(any())
    }

    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/deferred_courier_on_cancelled_order.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/deferred_courier_on_cancelled_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotProcessOnCancelledOrder() {
        planFactHandler.handle(listOf(1), fixedTime)
        verifyNoMoreInteractions(lomClient)
    }
}
