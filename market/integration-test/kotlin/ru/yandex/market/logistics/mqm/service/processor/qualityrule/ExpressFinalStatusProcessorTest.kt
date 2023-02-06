package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate

@DisplayName("Тесты обработчика финального статуса для Экспресс-доставки")
class ExpressFinalStatusProcessorTest : StartrekProcessorTest() {

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_final_status-create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_final_status-create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        clock.setFixed(Instant.parse("2021-03-01T20:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))

        handlePlanFacts()

        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 вовремя не получил финальный статус")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "Дедлайн: 01-11-2020 15:00:00",
                "",
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777"
            ).joinToString("\n")
        )
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MQMEXPRESS")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(123)
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
    }

    @DisplayName("Не обрабатывать план-факты без признака экспресс-доставки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_final_status-non_express.xml")
    fun doNotCreateIssueForNonExpressTest() {
        handlePlanFacts()
        verify(issues, never()).create(any())
    }
}
