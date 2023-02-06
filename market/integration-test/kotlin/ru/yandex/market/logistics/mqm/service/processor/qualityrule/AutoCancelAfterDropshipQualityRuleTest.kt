package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
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
import java.time.Instant

class AutoCancelAfterDropshipQualityRuleTest: StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-11-22T07:01:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Тикет в Startrek не создается, если обработка выполняется слишком рано")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/order_not_shipped/create_ticket_with_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_not_shipped/not_created_ticket_too_early.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateAggregatedIssueIfBeforeCreationTimeTest() {
        clock.setFixed(Instant.parse("2021-11-22T05:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verifyZeroInteractions(issues)
        verifyZeroInteractions(attachments)
    }

    @DisplayName("Создание тикета в Startrek")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/order_not_shipped/create_ticket_with_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_not_shipped/create_ticket_with_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
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
            issueValues.getOrThrow("summary") shouldBe "[MQM] 22-11-2021: Отмена неотгруженных заказов."
            issueValues.getOrThrow("description") shouldBe
                "Необходимо отменить данный список заказов с сабстатусом WAREHOUSE_FAILED_TO_SHIP.\n" +
                "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777, 778"
            issueValues.getOrThrow("defectOrders") shouldBe 2
        }
    }

    @DisplayName("Игнорировать тикеты других producer_name")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/order_not_shipped/unsupported_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_not_shipped/unsupported_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateAggregatedIssueForUnsupportedProducersTest() {
        handleGroups()
        verifyZeroInteractions(issues)
        verifyZeroInteractions(attachments)
    }
}
