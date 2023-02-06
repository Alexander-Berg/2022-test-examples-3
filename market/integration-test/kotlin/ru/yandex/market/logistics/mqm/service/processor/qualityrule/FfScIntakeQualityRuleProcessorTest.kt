package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class FfScIntakeQualityRuleProcessorTest: StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-07T11:15:30.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта отгрузки ФФ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/ff_sc/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/ff_sc/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssueTest() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
            )

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] 07-11-2020 13:00: СЦ Тестовый СЦ вовремя не приняла заказы со склада Тестовый склад."
            issueValues.getOrThrow("description") shouldBe
                    """
                    https://abo.market.yandex-team.ru/order/777
                    https://ow.market.yandex-team.ru/order/777
                    https://lms-admin.market.yandex-team.ru/lom/orders/100111
                    Дата создания заказа: 01-11-2020
                    Дедлайн приема в СЦ: 07-11-2020 09:11
                    Трек склада: 101
                    Трек СЦ: 102
                    """.trimIndent()
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 1
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777"
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder
                    listOf("Тестовый склад:987654321", "Тестовый СЦ:987654322")
        }
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов отгрузки ФФ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/ff_sc/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/ff_sc/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
            )
        whenever(attachments.upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] 07-11-2020 13:00: СЦ Тестовый СЦ вовремя не приняла заказы со склада Тестовый склад."
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777, 778"
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder
                    listOf("Тестовый склад:987654321", "Тестовый СЦ:987654322")
        }
    }

    @DisplayName("Добавление комментариев в тикеты Startrek для просроченных план-фактов отгрузки ФФ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/ff_sc/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/ff_sc/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
            )
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
                    
                    Удалены неактуальные заказы (1 шт.): 778.
                    Добавлены новые заказы (1 шт.): 779.
                    Список заказов в приложении (2 шт.).
                    """.trimIndent()
            values.getAsScalarUpdate("customerOrderNumber") shouldBe "777, 778, 779"
            values.getAsScalarUpdate("defectOrders") shouldBe 3
            verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        }
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов отгрузки ФФ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/ff_sc/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/ff_sc/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllAggregatedIssueTest() {
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

    @DisplayName("Переоткрытие тикета")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/ff_sc/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = Mockito.mock(
            Transition::class.java
        )
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handleGroups()

        verify(transitions, Mockito.times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    private fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()
}
