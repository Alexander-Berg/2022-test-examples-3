package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
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
import java.io.InputStream
import java.time.Instant

class RecipientTransmitAggregatedProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-02T08:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов по вручению заказа по расписанию")
    @DatabaseSetup("/service/processor/qualityrule/before/recipient_transmit/create_aggregated.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/recipient_transmit/create_aggregated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createAggregatedIssueTest() {
        whenever(issues.create(ArgumentMatchers.any())).thenReturn(issue)

        val attachment = Mockito.mock(
            Attachment::class.java
        )
        whenever(attachment.id).thenReturn("AT-123")
        whenever(
            attachments.upload(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(
                    InputStream::class.java
                )
            )
        ).thenReturn(attachment)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())

        val issueCreate = captor.value

        val listOfAttachments = issueCreate.attachments
        val values = issueCreate.values
        assertSoftly {
            listOfAttachments shouldContainExactly listOf("AT-123")
            values.getOrThrow("summary") shouldBe SUMMARY
            values.getOrThrow("components") shouldBe COMPONENTS
            values.getOrThrow("defectOrders") shouldBe 2
            values.getOrThrow("customerOrderNumber") shouldBe "777, 888"
            values.getOrThrow("queue") shouldBe QUEUE
        }
    }

    @DisplayName("Создание одиночного тикета для просроченного план-фактов по вручению заказа по расписанию")
    @DatabaseSetup("/service/processor/qualityrule/before/recipient_transmit/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/recipient_transmit/create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createSingleIssueTest() {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueCreate = captor.value
        val values = issueCreate.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe SUMMARY
            values.getOrThrow("components") shouldBe COMPONENTS
            values.getOrThrow("customerOrderNumber") shouldBe "777"
            values.getOrThrow("defectOrders") shouldBe 1
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("description") shouldBe """
                    https://abo.market.yandex-team.ru/order/777
                    https://ow.market.yandex-team.ru/order/777
                    https://lms-admin.market.yandex-team.ru/lom/orders/100111
                    Дедлайн вручения: 01-11-2020 15:00
            """.trimIndent()
        }
    }


    @DisplayName("Группа переносится на 10утра следующего дня при запуске вне интервала 10:00-19:00")
    @DatabaseSetup("/service/processor/qualityrule/before/recipient_transmit/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/recipient_transmit/reschedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun rescheduleTest() {
        clock.setFixed(Instant.parse("2020-11-02T23:01:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verify(issues, never()).create(any())
    }

    companion object {
        const val SUMMARY = "[MQM] 01-11-2020: DPD region заказы не были вовремя вручены пользователю"
        const val QUEUE = "MONITORINGSNDBX"
        private val COMPONENTS = longArrayOf(94987)
    }
}
