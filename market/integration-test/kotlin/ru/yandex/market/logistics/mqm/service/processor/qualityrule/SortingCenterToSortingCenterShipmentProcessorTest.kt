package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SortingCenterToSortingCenterShipmentProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-07T11:15:30.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Проверка, что процессор не создает тикеты, если вызвать до рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/scheduled_today_when_work_starts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateTicketsIfBeforeWorkingHoursTest() {
        clock.setFixed(
            LocalDateTime.of(
                LocalDate.of(2021, 3, 22),
                LocalTime.of(9, 59)
            )
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toInstant(),
            DateTimeUtils.MOSCOW_ZONE
        )
        handleGroups()
    }

    @DisplayName("Проверка, что процессор не создает тикеты, если вызвать после рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/scheduled_tomorrow_when_work_starts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateTicketsIfAfterWorkingHoursTest() {
        clock.setFixed(
            LocalDateTime.of(
                LocalDate.of(2021, 3, 22),
                LocalTime.of(19, 1)
            )
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toInstant(),
            DateTimeUtils.MOSCOW_ZONE
        )
        handleGroups()
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта отгрузки СЦ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssueTest() {
        val summary = "[MQM] 07-11-2020: СЦ Тестовый СЦ 1 вовремя не отгрузил заказы."
        val description = java.lang.String.join(
            "\n",
            "https://abo.market.yandex-team.ru/order/777",
            "https://ow.market.yandex-team.ru/order/777",
            "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
            "Дата создания заказа: 01-11-2020",
            "Дедлайн отгрузки: 07-11-2020 11:11",
            "Трек отгружающего СЦ: 101",
            "Трек принимающего СЦ: 102"
        )

        val tags = arrayOf(
            "Тестовый СЦ 1:987654321"
        )
        createSingleIssue(summary, description, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для одного просроченного план-факта отгрузки СЦ-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/create_ticket_with_one_planfact.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/shipment/sc_sc/go_order_one_planfact.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/go_order_create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateSingleIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07-11-2020: СЦ Тестовый СЦ 1 вовремя не отгрузил заказы."
        val description = java.lang.String.join(
            "\n",
            "https://abo.market.yandex-team.ru/order/777",
            "",
            "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
            "Дата создания заказа: 01-11-2020",
            "Дедлайн отгрузки: 07-11-2020 11:11",
            "Трек отгружающего СЦ: 101",
            "Трек принимающего СЦ: 102"
        )

        val tags = arrayOf(
            "yandex_go-доставка_наружу",
            "Тестовый СЦ 1:987654321"
        )
        createSingleIssue(summary, description, tags)
    }

    private fun createSingleIssue(summary: String, description: String, tags: Array<String>) {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        Mockito.verify(issues).create(captor.capture())
        val issueCreate = captor.value
        val values = issueCreate.values
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(summary)
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(description)
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1)
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777")
        softly.assertThat<String>(values.getOrThrow("tags") as Array<String?>)
            .containsExactly(*tags)
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов отгрузки СЦ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        createAggregatedIssue(
            "[MQM] 07-11-2020: СЦ Тестовый СЦ 1 вовремя не отгрузил заказы.",
            arrayOf("Тестовый СЦ 1:987654321")
        )
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для просроченных план-фактов отгрузки СЦ-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/create_ticket_with_some_planfacts.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/shipment/sc_sc/go_order_some_planfacts.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/go_order_create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateAggregatedIssueTest() {
        createAggregatedIssue(
            "[MQM][Доставка Наружу] 07-11-2020: СЦ Тестовый СЦ 1 вовремя не отгрузил заказы.",
            arrayOf("yandex_go-доставка_наружу", "Тестовый СЦ 1:987654321")
        )
    }

    private fun createAggregatedIssue(summary: String, tags: Array<String>) {
        val attachment = Mockito.mock(
            Attachment::class.java
        )
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(
            attachments.upload(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any()
            )
        ).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        Mockito.verify(issues).create(captor.capture())
        val issueCreate = captor.value
        val values = issueCreate.values
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(summary)
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (2 шт.)")
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2)
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 778")
        softly.assertThat(values.getOrThrow("tags") as Array<String>)
            .containsExactly(*tags)
        Mockito.verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @DisplayName("Добавление комментариев в тикета Startrek для просроченных план-фактов отгрузки СЦ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = Mockito.mock(
            Attachment::class.java
        )
        whenever(
            attachments.upload(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any()
            )
        ).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        Mockito.verify(issues).update(ArgumentMatchers.eq("MONITORINGSNDBX-1"), captor.capture())
        val issueUpdate = captor.value
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values
        softly.assertThat(commentString).isEqualTo(
            java.lang.String.join(
                "\n",
                "Информация в тикете была автоматически изменена.",
                "",
                "Удалены неактуальные заказы (1 шт.): 778.",
                "Добавлены новые заказы (1 шт.): 779.",
                "Список заказов в приложении (2 шт.)."
            )
        )
        softly.assertThat((values.getOrThrow("customerOrderNumber") as ScalarUpdate<*>).set.get())
            .isEqualTo("777, 778, 779")
        softly.assertThat((values.getOrThrow("defectOrders") as ScalarUpdate<*>).set.get())
            .isEqualTo(3)
        Mockito.verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов отгрузки СЦ-СД")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_sc/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllAggregatedIssueTest() {
        handleGroups()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        Mockito.verify(transitions).execute(
            ArgumentMatchers.eq("MONITORINGSNDBX-1"), ArgumentMatchers.any(
                String::class.java
            ), captor.capture()
        )
        val issueUpdate = captor.value
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values
        softly.assertThat(commentString).isEqualTo("Тикет автоматически закрыт.")
        softly.assertThat((values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_sc/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = Mockito.mock(
            Transition::class.java
        )
        whenever(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition)

        handleGroups()

        Mockito.verify(transitions, Mockito.times(1))
            .execute(ArgumentMatchers.eq("MONITORINGSNDBX-1"), ArgumentMatchers.eq(transition))
    }
}
