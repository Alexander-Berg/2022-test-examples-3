package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.time.Instant
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

@DisplayName("Тесты обработчика ручной обработки пересчета ПДД")
class ScScMkIntakeManualRddAggregationQualityRuleProcessorTest: StartrekProcessorTest() {
    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2022-04-07T10:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для группы")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/sc_scmk_manual_rdd/before/setup_group.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/sc_scmk_manual_rdd/after/created_ticket.xml",
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
            issueValues.getOrThrow("summary") shouldBe
                "[MQM] 10-11-2021: Меняется РДД заказов из-за задержки. Заказы в пути от Тестовый СЦ 1 до Тестовый СЦ 2."
            issueValues.getOrThrow("description") shouldBe
                "Переносится дата доставки заказов, которые едут от СЦ до СЦ МК.\n" +
                "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777, 778"
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder
                listOf("Тестовый СЦ 1:СЦ", "Тестовый СЦ 2:СЦ")
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("deliveryName") shouldBe "Тестовый МК"
        }
    }
}
