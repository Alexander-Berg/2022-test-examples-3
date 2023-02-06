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

class MkMiddleMileRecipientAggregatedProcessorTest : StartrekProcessorTest() {

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-02T07:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета для одного просроченного план-факта для курьерской доставки (своя курьерка)")
    @DatabaseSetup("/service/processor/qualityrule/before/mk_middle_mile_recipient/create_single_mc.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/mk_middle_mile_recipient/create_single_mc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createSingleIssueTest() {
        val summary = SUMMARY
        val description =
            """
            Дедлайн передачи на среднюю милю: 01-11-2020 14:00:00
            
            https://abo.market.yandex-team.ru/order/777
            https://ow.market.yandex-team.ru/order/777
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            """.trimIndent()
        val tags = arrayOf("Своя курьерка:987654321")

        createIssue(summary, description, tags)
    }

    @DisplayName(
        "Заказ YANDEX_GO. Создание тикета для одного просроченного план-факта для курьерской доставки (своя курьерка)"
    )
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/mk_middle_mile_recipient/create_single_mc.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/mk_middle_mile_recipient/go_order_one_planfact.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/mk_middle_mile_recipient/go_order_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun goOrderCreateSingleIssueTest() {
        val summary = GO_ORDER_SUMMARY
        val description =
            """
            Дедлайн передачи на среднюю милю: 01-11-2020 14:00:00
            
            https://abo.market.yandex-team.ru/order/777
            
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            """.trimIndent()
        val tags = arrayOf("yandex_go-доставка_наружу", "Своя курьерка:987654321")

        createIssue(summary, description, tags)
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов для курьерской доставки (своя курьерка)")
    @DatabaseSetup("/service/processor/qualityrule/before/mk_middle_mile_recipient/create_aggregated_mc.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/mk_middle_mile_recipient/create_aggregated_mc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createAggregatedIssueTest() {
        val summary = SUMMARY
        val description = "Список заказов в приложении (кол-во заказов: 2)"
        val tags = arrayOf("Своя курьерка:987654321")

        createIssue(summary, description, tags)
    }

    @DisplayName(
        "Заказ YANDEX_GO. Создание тикета для группы просроченных план-фактов для курьерской доставки (своя курьерка)"
    )
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/mk_middle_mile_recipient/create_aggregated_mc.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/mk_middle_mile_recipient/go_order_some_planfacts.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/mk_middle_mile_recipient/go_order_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun goOrderCreateAggregatedIssueTest() {
        val summary = GO_ORDER_SUMMARY
        val description = "Список заказов в приложении (кол-во заказов: 2)"
        val tags = arrayOf("yandex_go-доставка_наружу", "Своя курьерка:987654321")

        createIssue(summary, description, tags)
    }

    private fun createIssue(summary: String, description: String, tags: Array<String>) {
        whenever(issues.create(any())).thenReturn(issue)
        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())
        val values = captor.firstValue.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe summary
            values.getOrThrow("description") shouldBe description
            values.getOrThrow("tags") shouldBe tags
            values.getOrThrow("components") shouldBe longArrayOf(109618)
        }
    }

    companion object {
        const val SUMMARY = "[MQM] 01-11-2020: МК (Своя курьерка) – Своя курьерка – вовремя не передали на среднюю милю"
        const val GO_ORDER_SUMMARY = "[MQM][Доставка Наружу] 01-11-2020: " +
                "МК (Своя курьерка) – Своя курьерка – вовремя не передали на среднюю милю"
    }
}
