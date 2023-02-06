package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.bolts.collection.MapF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import ru.yandex.startrek.client.model.Update
import java.time.Instant

class ScScIntakeAggregatedProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-11-10T07:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта приёмки СЦ-СЦ")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/sc_sc_intake/setup_with_order.xml",
        "/service/processor/qualityrule/before/sc_sc_intake/group_with_one_planfact.xml",
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        val summary = "[MQM] 10.11.2021 10:00: Тестовый СЦ 2 вовремя не принял заказы, отгруженные Тестовый СЦ 1."
        val description =
            """
            https://abo.market.yandex-team.ru/order/777
            https://ow.market.yandex-team.ru/order/777
            https://lms-admin.market.yandex-team.ru/lom/orders/1
            
            Дата создания заказа: 10-11-2021
            Дедлайн приемки: 10-11-2021 10:00:00
            
            Трек отправившего СЦ: 101
            Трек принимающего СЦ: 102
            """.trimIndent()
        val tags = arrayOf("Тестовый СЦ 1:СЦ", "Тестовый СЦ 2:СЦ")

        createIssue(summary, description, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для одного просроченного план-факта приёмки СЦ-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/sc_sc_intake/setup_with_order.xml"),
            DatabaseSetup("/service/processor/qualityrule/before/sc_sc_intake/group_with_one_planfact.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/sc_sc_intake/setup_with_go_order.xml"],
                type = DatabaseOperation.UPDATE
            ),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/sc_sc_intake/group_with_one_planfact_go_order.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_one_planfact_go_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateIssueTest() {
        val summary = "[MQM][Доставка Наружу] 10.11.2021 10:00: Тестовый СЦ 2 вовремя не принял заказы, " +
                "отгруженные Тестовый СЦ 1."
        val description =
            """
            https://abo.market.yandex-team.ru/order/777
            
            https://lms-admin.market.yandex-team.ru/lom/orders/1
            
            Дата создания заказа: 10-11-2021
            Дедлайн приемки: 10-11-2021 10:00:00
            
            Трек отправившего СЦ: 101
            Трек принимающего СЦ: 102
            """.trimIndent()
        val tags = arrayOf("yandex_go-доставка_наружу", "Тестовый СЦ 1:СЦ", "Тестовый СЦ 2:СЦ")

        createIssue(summary, description, tags)
    }

    private fun createIssue(summary: String, description: String, tags: Array<String>) {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())

        val issueValues = captor.value.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe summary
            issueValues.getOrThrow("description") shouldBe description
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777"
            issueValues.getOrThrow("defectOrders") shouldBe 1
            issueValues.getOrThrow("tags") shouldBe tags
        }
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов приёмки СЦ-СЦ")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/sc_sc_intake/setup_with_two_orders.xml",
        "/service/processor/qualityrule/before/sc_sc_intake/group_two_planfacts.xml",
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_two_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        val summary = "[MQM] 10.11.2021 10:00: Тестовый СЦ 2 вовремя не принял заказы, отгруженные Тестовый СЦ 1."
        val tags = listOf("Тестовый СЦ 1:СЦ", "Тестовый СЦ 2:СЦ")

        createAggregatedIssue(summary, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для просроченных план-фактов приёмки СЦ-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/sc_sc_intake/setup_with_two_orders.xml"),
            DatabaseSetup("/service/processor/qualityrule/before/sc_sc_intake/group_two_planfacts.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/sc_sc_intake/setup_with_two_go_orders.xml"],
                type = DatabaseOperation.UPDATE
            ),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/sc_sc_intake/group_two_planfacts_go_orders.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_two_planfacts_go_orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateAggregatedIssueTest() {
        val summary = "[MQM][Доставка Наружу] 10.11.2021 10:00: Тестовый СЦ 2 вовремя не принял заказы, " +
                "отгруженные Тестовый СЦ 1."
        val tags = listOf("yandex_go-доставка_наружу", "Тестовый СЦ 1:СЦ", "Тестовый СЦ 2:СЦ")

        createAggregatedIssue(summary, tags)
    }

    private fun createAggregatedIssue(summary: String, tags: List<String>) {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(attachments.upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values
        verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe summary
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777, 778"
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder tags
            issueValues.getOrThrow("defectOrders") shouldBe 2
        }
    }

    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов приёмки СЦ-СЦ")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/sc_sc_intake/setup_with_order.xml",
        "/service/processor/qualityrule/before/sc_sc_intake/group_with_ticket.xml",
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(attachments.upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(issues).update(eq("MONITORINGSNDBX-1"), captor.capture())
        val commentString = captor.value.comment.get().comment.get()
        val values = captor.value.values
        assertSoftly {
            commentString shouldBe
                    """
                Информация в тикете была автоматически изменена.
                
                Удалены неактуальные заказы (1 шт.): 001.
                Добавлены новые заказы (1 шт.): 777.
                Список заказов в приложении (1 шт.).
                """.trimIndent()
            values.getAsScalarUpdate("customerOrderNumber") shouldBe "001, 777"
            values.getAsScalarUpdate("defectOrders") shouldBe 2
            verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        }
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов приёмки СЦ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/sc_sc_intake/empty_group_with_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/empty_group_with_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun closeIssueTest() {
        handleGroups()

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

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/sc_sc_intake/setup_with_order.xml",
        "/service/processor/qualityrule/before/sc_sc_intake/group_with_ticket.xml",
    )
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = Mockito.mock(Transition::class.java)
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handleGroups()

        verify(transitions).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Решедулить группу если сейчас не время создания тикетов")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/sc_sc_intake/setup_with_order.xml",
        "/service/processor/qualityrule/before/sc_sc_intake/group_with_one_planfact.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_one_planfact_rescheduled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun rescheduleIssueFlowTest() {
        clock.setFixed(Instant.parse("2021-03-01T04:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verify(issues, Mockito.never()).create(ArgumentMatchers.any())
    }

    @DisplayName("Перенос обработки группы на следующие 15 минут после рабочих часов для группы с тикетом.")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/sc_sc_intake/setup_with_order.xml",
        "/service/processor/qualityrule/before/sc_sc_intake/group_with_one_planfact_out_of_work_hours.xml",
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_sc_intake/group_with_one_planfact_out_of_work_hours.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun rescheduleDuringNonWorkHoursForGroupWithTicket() {
        clock.setFixed(Instant.parse("2021-11-10T17:02:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verify(issues, Mockito.never()).create(ArgumentMatchers.any())
    }

    private fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()
}
