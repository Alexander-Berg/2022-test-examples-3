package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import java.time.Instant

@DisplayName("Тесты обработчика отгрузки OnDemand")
class OnDemandShipmentProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T19:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта отгрузки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_shipment/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_shipment/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] 07-11-2020: Маркет курьерка вовремя не отгрузил заказы в Лавка."
        )
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                "https://tariff-editor.taxi.yandex-team.ru/dragon-orders/102/info?cluster=platform",
                "Дата создания заказа: 01-11-2020",
                "Дедлайн отгрузки из МК: 07-11-2020 14:00",
                "Трек MK: 101",
                "Трек СД: 102"
            ).joinToString("\n")
        )
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("tags") as Array<String?>)
            .containsExactly("Маркет курьерка:987654321", "Лавка:987654322")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(123L)
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов отгрузки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_shipment/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_shipment/"
            + "create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        val attachment = mock<Attachment>()

        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        whenever(attachments.upload(any<String>(), any())).thenReturn(attachment)

        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] 07-11-2020: Маркет курьерка вовремя не отгрузил заказы в Лавка."
        )
        softly.assertThat(values.getOrThrow("description")).isEqualTo("Список заказов в приложении (2 шт.)")
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(2)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>)
            .containsExactly("Маркет курьерка:987654321", "Лавка:987654322")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(123L)

        verify(attachments).upload(any<String>(), any())
    }

    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов отгрузки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_shipment/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_shipment/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = mock<Attachment>()
        whenever(attachments.upload(any<String>(), any())).thenReturn(attachment)

        handleGroups()
        val captor = argumentCaptor<IssueUpdate>()
        verify(issues).update(eq("MONITORINGSNDBX-1"), captor.capture())

        val issueUpdate = captor.lastValue
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        softly.assertThat(commentString).isEqualTo(
            listOf(
                "Информация в тикете была автоматически изменена.",
                "",
                "Удалены неактуальные заказы (1 шт.): 778.",
                "Добавлены новые заказы (1 шт.): 779.",
                "Список заказов в приложении (2 шт.)."
            ).joinToString("\n")
        )
        softly.assertThat((values.getOrThrow("customerOrderNumber") as ScalarUpdate<*>).set.get())
            .isEqualTo("777, 778, 779")
        softly.assertThat((values.getOrThrow("defectOrders") as ScalarUpdate<*>).set.get()).isEqualTo(3)

        verify(attachments).upload(any<String>(), any())
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов отгрузки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_shipment/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_shipment/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllAggregatedIssueTest() {
        handleGroups()
        val captor = argumentCaptor<IssueUpdate>()
        verify(transitions).execute(eq("MONITORINGSNDBX-1"), any<String>(), captor.capture())

        val issueUpdate = captor.lastValue
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        softly.assertThat(commentString).isEqualTo("Тикет автоматически закрыт.")
        softly.assertThat((values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_shipment/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")

        val transition = mock<Transition>()
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handleGroups()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Проверка, что процессор не создает тикет, если был вызван до рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/on_demand_shipment/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/on_demand_shipment/schedule_on_work_hour.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateTicketIfIfCalledBeforeWorkHourTest() {
        clock.setFixed(Instant.parse("2020-11-07T23:15:30.00Z"), DateTimeUtils.MOSCOW_ZONE)

        handleGroups()
        verifyNoInteractions(issues)
    }
}
