package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
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
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import java.time.Instant

class MisdirectProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-09-08T12:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для засылов прямого потока")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/misdirect/setup_create_ticket_direct_flow.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/misdirect/create_ticket_direct_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueForDirectFlowTest() {
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
            issueValues.getOrThrow("summary") shouldBe "[MQM] 08-09-2021: " +
                "На СЦ 105120,_Москва,_Золоторожский_Вал,_дом_42 обнаружены засылы прямого потока."
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "111, 22"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            (issueValues.getOrThrow("tags") as Array<String?>) shouldContainExactlyInAnyOrder arrayOf(
                "105120__Москва__Золоторожский_Вал__дом_42",
                "Прямой",
            )
        }
    }

    @DisplayName("Создание тикета в Startrek для засылов обратного")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/misdirect/setup_create_ticket_reverse_flow.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/misdirect/create_ticket_reverse_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueForReverseFlowTest() {
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
            issueValues.getOrThrow("summary") shouldBe "[MQM] 08-09-2021: " +
                "На СЦ Адрес CЦ, в который по ошибке прислали заказ обнаружены засылы обратного потока."
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "111, 22"
            issueValues.getOrThrow("defectOrders") shouldBe 2
        }
    }

    @DisplayName("Обновление csv в тикете, если состав заказов поменялся")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/misdirect/update_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/misdirect/update_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(attachments.upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(issues).update(eq("MONITORINGSNDBX-1"), captor.capture())
        val commentString = captor.value.comment.get().comment.get()
        assertSoftly {
            commentString shouldBe
                """
                Информация в тикете была автоматически изменена.
                
                Добавлены новые заказы (1 шт.): 222.
                Список заказов в приложении (2 шт.).
                """.trimIndent()
            verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        }
    }

    @DisplayName("Тикет не создается, если процессор вызван до установленного времени создания тикетов")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/misdirect/setup_create_ticket_direct_flow.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/misdirect/reschedule_direct_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueDoNothingIfCalledBeforeTimeToCreate() {
        clock.setFixed(Instant.parse("2021-09-08T07:00:59.00Z"), DateTimeUtils.MOSCOW_ZONE)

        handleGroups()

        verifyZeroInteractions(issues)
        verifyZeroInteractions(attachments)
    }
}
