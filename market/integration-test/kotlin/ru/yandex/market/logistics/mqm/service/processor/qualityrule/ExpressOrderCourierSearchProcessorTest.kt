package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition

class ExpressOrderCourierSearchProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T20:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов, если курьер не найден")
    @DatabaseSetup("/service/processor/qualityrule/before/express_courier_search/create_ticket.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_courier_search/create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] На заказ 777 от 01-11-2020 не был вовремя назначен курьер Go")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "",
                "",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовая сд:1", "СД")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(91243L)
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов")
    @DatabaseSetup("/service/processor/qualityrule/before/express_courier_search/close_ticket.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_courier_search/close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueTest() {
        handlePlanFacts()
        val captor = argumentCaptor<IssueUpdate>()
        verify(transitions).execute(eq("MONITORINGSNDBX-1"), any<String>(), captor.capture())

        val issueUpdate = captor.lastValue
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        softly.assertThat(commentString).isEqualTo("Тикет автоматически закрыт.")
        softly.assertThat((values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
    }

    @DisplayName("Закрытие закрытого тикета в Startrek")
    @DatabaseSetup("/service/processor/qualityrule/before/express_courier_search/close_closed_ticket.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_courier_search/close_closed_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeClosedIssueTest() {
        whenever(issueStatusRef.key).thenReturn("closed")

        handlePlanFacts()
        verify(transitions, never()).execute(any<String>(), any<Transition>(), any<IssueUpdate>())
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_courier_search/reopen_closed_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")

        val transition = mock<Transition>()
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handlePlanFacts()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Проверка, что обработка план-факта с группой переносится на 15 минут вне дефолтного 9-23.")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/plan_fact_with_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/plan_fact_with_ticket_reschedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun reschedule15MinutesLaterDefaultHours() {
        clock.setFixed(Instant.parse("2021-03-01T20:01:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handlePlanFacts()
        verify(issues, never()).create(any())
    }
}
