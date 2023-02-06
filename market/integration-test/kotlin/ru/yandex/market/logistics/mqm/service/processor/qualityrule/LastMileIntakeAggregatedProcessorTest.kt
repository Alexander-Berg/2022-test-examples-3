package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.service.yt.PvzContactInformationCache
import ru.yandex.market.logistics.mqm.service.yt.dto.PvzContactInformation
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import java.time.Instant

internal class LastMileIntakeAggregatedProcessorTest: StartrekProcessorTest() {

    @Autowired
    lateinit var pvzContactInformationCache: PvzContactInformationCache

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2022-04-04T14:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/last_mile/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/last_mile/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        val summary = "[MQM] 07-11-2020: Лавка вовремя не принял заказы, отгруженные Маркет курьерка."
        val description =
            """
            https://abo.market.yandex-team.ru/order/777
            https://ow.market.yandex-team.ru/order/777
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            https://tariff-editor.taxi.yandex-team.ru/dragon-orders/102/info?cluster=platform
            Дата создания заказа: 01-11-2020
            Дедлайн приемки из МК: 03-04-2022 10:00
            Трек MK: 101
            Трек СД: 102
            Email ПВЗ: email1@mail.com
            Телефон ПВЗ: phone11
            Телефон руководителя ПВЗ: phone21
            """.trimIndent()
        val tags = arrayOf("Маркет курьерка:987654321", "Лавка:987654322")

        createIssue(summary, description, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для одного просроченного план-факта")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/intake/last_mile/create_ticket_with_one_planfact.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/intake/last_mile/go_order_one_planfact.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/last_mile/go_order_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07-11-2020: Лавка вовремя не принял заказы, отгруженные Маркет курьерка."
        val description =
            """
            https://abo.market.yandex-team.ru/order/777

            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            https://tariff-editor.taxi.yandex-team.ru/dragon-orders/102/info?cluster=platform
            Дата создания заказа: 01-11-2020
            Дедлайн приемки из МК: 03-04-2022 10:00
            Трек MK: 101
            Трек СД: 102
            Email ПВЗ: email1@mail.com
            Телефон ПВЗ: phone11
            Телефон руководителя ПВЗ: phone21
            """.trimIndent()
        val tags = arrayOf("yandex_go-доставка_наружу", "Маркет курьерка:987654321", "Лавка:987654322")

        createIssue(summary, description, tags)
    }

    private fun createIssue(summary: String, description: String, tags: Array<String>) {
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687231L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone21"))
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

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов приемки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/last_mile/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/last_mile/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        val summary = "[MQM] 07-11-2020: Лавка вовремя не принял заказы, отгруженные Маркет курьерка."
        val tags = arrayOf("Маркет курьерка:987654321", "Лавка:987654322")

        createAggregatedIssue(summary, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для просроченных план-фактов приемки")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup(
                "/service/processor/qualityrule/before/intake/last_mile/create_ticket_with_some_planfacts.xml"
            ),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/intake/last_mile/go_order_some_planfacts.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/last_mile/go_order_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateAggregatedIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07-11-2020: Лавка вовремя не принял заказы, отгруженные Маркет курьерка."
        val tags = arrayOf("yandex_go-доставка_наружу", "Маркет курьерка:987654321", "Лавка:987654322")

        createAggregatedIssue(summary, tags)
    }

    private fun createAggregatedIssue(summary: String, tags: Array<String>) {
        val attachment = mock<Attachment>()

        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687231L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone21"))
        whenever(attachments.upload(any<String>(), any())).thenReturn(attachment)

        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueValues = captor.lastValue.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe summary
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("tags") shouldBe tags
            verify(attachments).upload(any<String>(), any())
        }
    }
}
