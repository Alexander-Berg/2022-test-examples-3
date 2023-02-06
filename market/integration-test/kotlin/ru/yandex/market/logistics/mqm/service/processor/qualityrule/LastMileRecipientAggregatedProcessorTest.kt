package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.IssueCreate
import java.time.Instant

class LastMileRecipientAggregatedProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-05T20:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета для одного просроченного план-факта для курьерской доставки (контрактная)")
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createSingleIssueCourierTest() {
        val summary = "[MQM] 01-11-2020: Контрактная доставка – Яндекс.Go – вовремя не передали на последнюю милю"
        val description = getDescription(withOwLink = true)
        val tags = arrayOf("Яндекс.Go:987654321")
        createIssue(summary, description, tags)
    }

    @DisplayName(
        "Заказ YANDEX_GO. Создание тикета для одного просроченного план-факта для курьерской доставки (контрактная)"
    )
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_single.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/last_mile_recipient/go_order_create_single.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/go_order_create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun goOrderCreateSingleIssueCourierTest() {
        val summary = "[MQM][Доставка Наружу] 01-11-2020: Контрактная доставка – Яндекс.Go " +
                "– вовремя не передали на последнюю милю"
        val description = getDescription(withOwLink = false)
        val tags = arrayOf("yandex_go-доставка_наружу", "Яндекс.Go:987654321")
        createIssue(summary, description, tags)
    }

    @DisplayName("Создание тикета для одного просроченного план-факта для ПВЗ (контрактная)")
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_single_pickup.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/create_single_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createSingleIssuePickupTest() {
        val summary = "[MQM] 01-11-2020: Контрактная доставка – Какой-то ПВЗ – вовремя не передали на последнюю милю"
        val description = getDescription(withOwLink = true)
        val tags = arrayOf("Какой-то ПВЗ:987654321")
        createIssue(summary, description, tags)
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов для МК")
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_aggregated.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/create_aggregated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createAggregatedIssueMkTest() {
        val summary = "[MQM] 01-11-2020: МК – МК СД – вовремя не передали на последнюю милю"
        val description = "Список заказов в приложении (кол-во заказов: 2)"
        val tags = arrayOf("МК СД:987654321")
        createIssue(summary, description, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета для группы просроченных план-фактов для МК")
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_aggregated.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/last_mile_recipient/go_order_create_aggregated.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_aggregated.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/go_order_create_aggregated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun goOrderCreateAggregatedIssueMkTest() {
        val summary = "[MQM][Доставка Наружу] 01-11-2020: МК – МК СД – вовремя не передали на последнюю милю"
        val description = "Список заказов в приложении (кол-во заказов: 2)"
        val tags = arrayOf("yandex_go-доставка_наружу", "МК СД:987654321")
        createIssue(summary, description, tags)
    }

    fun createIssue(summary: String, description: String, tags: Array<String>) {
        clock.setFixed(Instant.parse("2020-11-02T07:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(issues.create(any())).thenReturn(issue)
        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())
        val values = captor.firstValue.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe summary
            values.getOrThrow("description") shouldBe description
            values.getOrThrow("tags") shouldBe tags
            values.getOrThrow("components") shouldBe longArrayOf(109617)
        }
    }

    private fun getDescription(withOwLink: Boolean): String {
        val owLink = if (withOwLink) "https://ow.market.yandex-team.ru/order/777" else ""

        return """
            Дедлайн передачи на последнюю милю: 01-11-2020 15:00:00
            
            https://abo.market.yandex-team.ru/order/777
            $owLink
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            """.trimIndent()
    }
}
