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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.bolts.collection.MapF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import java.time.Instant

@DisplayName("Тесты на создание тикетов для просрочек по возврату в ПВЗ/Лавку")
class OnDemandReturnProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T19:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek c тэгами для Лавки для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_return/create_ticket_taxi_lavka.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_return/create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createTaxiLavkaReturnIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        assertCommonTicketValues(issueCreate.values, "Яндекс.Go", "Лавка:987654322")
    }

    @DisplayName("Создание тикета в Startrek с тэгами для собственных ПВЗ для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_return/create_ticket_pickup_point.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_return/create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createOwnPickupPointReturnIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        assertCommonTicketValues(issueCreate.values, "Партнерские ПВЗ", "Партнерский ПВЗ:987654321")
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов, после получения чекпоинта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_return/close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_return/close_ticket.xml",
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

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_return/reopen_ticket.xml")
    fun reopenClosedIssueTest() {
        whenever(issueStatusRef.key).thenReturn("closed")

        val transition = mock<Transition>()
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handlePlanFacts()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Не создавать тикет если нет признака ON_DEMAND")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_return/do_not_create.xml")
    fun doNotCreateIssueTest() {
        handlePlanFacts()
        verify(issues, never()).create(any())
    }

    private fun assertCommonTicketValues(
        values: MapF<String, Any>,
        partnerSubtype: String,
        partnerNameIdTag: String
    ) {
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(String.format("[MQM] Заказ 777 не был вовремя возвращен в %s", partnerSubtype))
        softly.assertThat(values.getOrThrow("tags") as Array<String?>)
            .containsExactly(partnerSubtype, partnerNameIdTag)
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "Дедлайн: 01-11-2020 15:00:00",
                "",
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                "https://tariff-editor.taxi.yandex-team.ru/dragon-orders/102/info?cluster=platform"
            ).joinToString("\n")
        )
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MQMONDEMAND")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(123)
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
    }
}
