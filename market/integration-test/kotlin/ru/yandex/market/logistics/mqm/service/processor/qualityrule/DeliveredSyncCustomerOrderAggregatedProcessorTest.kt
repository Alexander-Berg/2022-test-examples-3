package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
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
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DeliveredSyncCustomerOrderAggregatedProcessorTest : StartrekProcessorTest() {
    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2022-01-13T08:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Тикеты не создаются до рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivered_sync_customerorder/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivered_sync_customerorder/scheduled_today.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateTicketsBeforeWorkingHours() {
        clock.setFixed(
            LocalDateTime.of(
                LocalDate.of(2022, 1, 13),
                LocalTime.of(9, 59)
            )
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toInstant(),
            DateTimeUtils.MOSCOW_ZONE
        )
        handleGroups()
    }

    @DisplayName("Тикеты не создаются после рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivered_sync_customerorder/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivered_sync_customerorder/scheduled_tomorrow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateTicketsAfterWorkingHours() {
        clock.setFixed(
            LocalDateTime.of(
                LocalDate.of(2022, 1, 13),
                LocalTime.of(19, 1)
            )
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toInstant(),
            DateTimeUtils.MOSCOW_ZONE
        )
        handleGroups()
    }

    @DisplayName("Cоздание тикета для одного просроченного план-факта синхронизации статуса DELIVERED")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivered_sync_customerorder/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivered_sync_customerorder/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssueTest() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        Mockito.verify(issues).create(captor.capture())
        val issueCreate = captor.value
        val values = issueCreate.values
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 12-01-2022: Заказы вовремя не получили статус DELIVERED в чекаутере."
            )
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                java.lang.String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "Дата/время 50 чп в LOM: 11-01-2022 09:00",
                    "Дедлайн получения DELIVERED в чекаутере: 12-01-2022"
                )
            )
    }

    @DisplayName("Создание тикета для просроченных план-фактов синхронизации статуса DELIVERED")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivered_sync_customerorder/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivered_sync_customerorder/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        Mockito.verify(issues).create(captor.capture())
        val issueCreate = captor.value
        val values = issueCreate.values
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 12-01-2022: Заказы вовремя не получили статус DELIVERED в чекаутере."
            )
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (2 шт.)")
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 888")
        Mockito.verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @DisplayName("Создание только одного тикета по расписанию cо всеми просрочками до 10:00 (предыдущего дня)")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/delivered_sync_customerorder/create_ticket_once.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/delivered_sync_customerorder/create_ticket_once.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueOnlyOnceWithEverythingUntil10() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()
    }
}
