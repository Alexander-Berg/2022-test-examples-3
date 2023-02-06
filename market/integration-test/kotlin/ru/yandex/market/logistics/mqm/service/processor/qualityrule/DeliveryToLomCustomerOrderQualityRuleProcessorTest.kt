package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.utils.getAsScalarUpdate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.Transition
import java.time.LocalDateTime

class DeliveryToLomCustomerOrderQualityRuleProcessorTest: StartrekProcessorTest() {
    @BeforeEach
    fun setUp() {
        clock.setFixed(START_DATE, DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivery_to_lom_customerorder/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivery_to_lom_customerorder/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssueTest() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
            )

        handlePlanFacts()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] Заказ 1001 вовремя не отправлен в логистику"
            issueValues.getOrThrow("description") shouldBe
                    """
                    Идентификатор заказа: 1001
                    https://abo.market.yandex-team.ru/order/1001
                    https://ow.market.yandex-team.ru/order/1001
                    https://tsum.yandex-team.ru/trace/r1
                    Дедлайн: 20-05-2021 06:00
                    """.trimIndent()
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 1
            issueValues.getOrThrow("components") shouldBe listOf(123L)
        }
    }

    @DisplayName("Создание тикетов в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivery_to_lom_customerorder/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivery_to_lom_customerorder/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSeveralIssueTest() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
            )

        handlePlanFacts()
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivery_to_lom_customerorder/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivery_to_lom_customerorder/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllAggregatedIssueTest() {
        handlePlanFacts()

        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq("MONITORINGSNDBX-1"),
            ArgumentMatchers.any(String::class.java),
            captor.capture()
        )
        val issueUpdate = captor.value
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        assertSoftly {
            commentString shouldBe "Тикет автоматически закрыт."
            values.getAsScalarUpdate("resolution") shouldBe "can'tReproduce"
        }
    }

    @DisplayName("Переоткрытие тикета")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivery_to_lom_customerorder/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = Mockito.mock(
            Transition::class.java
        )
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handlePlanFacts()

        verify(transitions).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    companion object {
        private val START_DATE = LocalDateTime
            .of(2021, 5, 20, 7, 0, 0)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .toInstant()
    }
}
