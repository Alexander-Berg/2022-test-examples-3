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
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition

@DisplayName("Тесты обработчика для очереди ExpressShipment")
class ExpressShipmentQualityRuleProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T18:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_shipment-create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_shipment-create_ticket.xml",
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
            .isEqualTo("[MQM] Заказ 777 не был вовремя отгружен курьеру партнером Рога и Копыта (id: 12345)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "Дедлайн: 01-11-2020 15:00:00",
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Рога и Копыта:12345", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(493)
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов, после получения чекпоинта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_shipment-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_shipment-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueTest() {
        handlePlanFacts()
        val captor = argumentCaptor<IssueUpdate>()
        verify(transitions).execute(any<String>(), any<String>(), captor.capture())

        val issueUpdate = captor.lastValue

        softly.assertThat((issueUpdate.values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
        softly.assertThat(issueUpdate.comment.get().comment.get()).isEqualTo("Тикет автоматически закрыт.")
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов, после того, как планфакт устарел")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_shipment-close_ticket_after_expired.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_shipment-close_ticket_after_expired.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueAfterExpiredTest() {
        handlePlanFacts()
        val captor = argumentCaptor<IssueUpdate>()
        verify(transitions).execute(any<String>(), any<String>(), captor.capture())

        val issueUpdate = captor.lastValue

        softly.assertThat((issueUpdate.values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
        softly.assertThat(issueUpdate.comment.get().comment.get()).isEqualTo("Тикет автоматически закрыт.")
    }

    @DisplayName("Оставлять комментарий, когда тикет может быть закрыт вручную")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/close_assigned_ticket_manually.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/close_assigned_ticket_manually.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAssignedManuallyTest() {
        whenever(issue.assignee).thenReturn(Option.of(mock()))
        handlePlanFacts()
        verify(transitions, never()).execute(any<String>(), any<String>(), any<IssueUpdate>())

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment)
            .containsOnly("Тикет неактуален, так как все заказы ушли из мониторингов. Можно закрывать.")
    }

    @DisplayName("Оставлять комментарий, когда тикет может быть закрыт вручную только один раз")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/close_assigned_ticket_manually.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/close_assigned_ticket_manually.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAssignedManuallyOnlyOnceTest() {
        whenever(issue.assignee).thenReturn(Option.of(mock()))

        handlePlanFacts()
        clock.setFixed(Instant.parse("2021-03-01T18:30:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handlePlanFacts()
        verify(transitions, never()).execute(any<String>(), any<String>(), any<IssueUpdate>())
        verify(comments, times(1)).create(eq("MONITORINGSNDBX-1"), any())
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_shipment-reopen_ticket.xml")
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
