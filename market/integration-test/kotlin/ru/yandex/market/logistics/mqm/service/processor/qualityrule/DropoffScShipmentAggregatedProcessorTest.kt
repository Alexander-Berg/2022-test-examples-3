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
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.MapF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseFlow.FIELD_CUSTOMER_EMAIL
import ru.yandex.market.logistics.mqm.service.yt.PvzContactInformationCache
import ru.yandex.market.logistics.mqm.service.yt.dto.PvzContactInformation
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import ru.yandex.startrek.client.model.Update

internal class DropoffScShipmentAggregatedProcessorTest : StartrekProcessorTest() {

    @Autowired
    private lateinit var pvzContactInformationCache: PvzContactInformationCache

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T07:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта отгрузки ДО-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/dropoff_sc/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        val summary = "[MQM] 07-11-2020: Дропофф Тестовый дропофф вовремя не отгрузил заказы."
        val description =
            """
            https://abo.market.yandex-team.ru/order/777
            https://ow.market.yandex-team.ru/order/777
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            Дата создания заказа: 01-11-2020
            Дедлайн приема в СД: 07-11-2020 10:00
            Трек ДО: 101
            Трек СЦ: 102
            Адрес точки доставки ДО: Россия Московская область Красногорск Светлая улица 3А 143409
            Email точки доставки ДО: email1@mail.com
            """.trimIndent()
        val tags = arrayOf(
            "Тестовый дропофф:987654321",
            "Тестовый СЦ:987654322",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
        )
        createIssue(summary, description, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для одного просроченного план-факта отгрузки ДО-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup(
                "/service/processor/qualityrule/before/shipment/dropoff_sc/create_ticket_with_one_planfact.xml"
            ),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/shipment/dropoff_sc/go_order_group.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/go_order_create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07-11-2020: Дропофф Тестовый дропофф вовремя не отгрузил заказы."
        val description =
            """
            https://abo.market.yandex-team.ru/order/777

            https://lms-admin.market.yandex-team.ru/lom/orders/100111
            Дата создания заказа: 01-11-2020
            Дедлайн приема в СД: 07-11-2020 10:00
            Трек ДО: 101
            Трек СЦ: 102
            Адрес точки доставки ДО: Россия Московская область Красногорск Светлая улица 3А 143409
            Email точки доставки ДО: email1@mail.com
            """.trimIndent()
        val tags = arrayOf(
            "yandex_go-доставка_наружу",
            "Тестовый дропофф:987654321",
            "Тестовый СЦ:987654322",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
        )
        createIssue(summary, description, tags)
    }

    private fun createIssue(summary: String, description: String, tags: Array<String>) {
        whenever(issues.create(any()))
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
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder tags.toList()
            issueValues.getOrThrow(FIELD_CUSTOMER_EMAIL) shouldBe "email1@mail.com"
        }
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов отгрузки ДО-СЦ")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/shipment/dropoff_sc/create_ticket_with_some_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        val summary = "[MQM] 07-11-2020: Дропофф Тестовый дропофф вовремя не отгрузил заказы."
        val tags = listOf(
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 5А 143409",
            "Лог. точка:10001687232",
            "Тестовый дропофф:987654321",
            "Тестовый СЦ:987654322",
            "Тестовый СЦ 2:987654323",
        )
        createAggregatedIssue(summary, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для просроченных план-фактов отгрузки ДО-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup(
                "/service/processor/qualityrule/before/shipment/dropoff_sc/create_ticket_with_some_planfacts.xml"
            ),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/shipment/dropoff_sc/go_order_group.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/go_order_create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateAggregatedIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07-11-2020: Дропофф Тестовый дропофф вовремя не отгрузил заказы."
        val tags = listOf(
            "yandex_go-доставка_наружу",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 5А 143409",
            "Лог. точка:10001687232",
            "Тестовый дропофф:987654321",
            "Тестовый СЦ:987654322",
            "Тестовый СЦ 2:987654323",
        )
        createAggregatedIssue(summary, tags)
    }

    fun createAggregatedIssue(summary: String, tags: List<String>) {
        val attachment = mock(Attachment::class.java)
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(attachments.upload(anyString(), any())).thenReturn(attachment)
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687231L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone12"))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687232L)))
            .thenReturn(PvzContactInformation(2, "email2@mail.com", "phone21", "phone22"))

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values
        verify(attachments).upload(anyString(), any())
        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe summary
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (2 шт.)"
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777, 778"
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder tags
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow(FIELD_CUSTOMER_EMAIL) shouldBe "email1@mail.com"
        }
    }

    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов отгрузки ДО-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/dropoff_sc/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = mock(Attachment::class.java)
        whenever(attachments.upload(anyString(), any())).thenReturn(attachment)

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
            verify(attachments).upload(anyString(), any())
        }
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов отгрузки ДО-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/dropoff_sc/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllAggregatedIssueTest() {
        handleGroups()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq("MONITORINGSNDBX-1"),
            any(String::class.java),
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
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/dropoff_sc/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = mock(Transition::class.java)
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)
        handleGroups()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Решедулить группу если сейчас не время создания тикетов")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/shipment/dropoff_sc/create_ticket_with_some_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/rescheduled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun rescheduleIssueFlowTest() {
        clock.setFixed(Instant.parse("2021-03-01T04:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verify(issues, never()).create(any())
    }

    @DisplayName("Перенос обработки группы на следующие 15 минут после рабочих часов для группы с тикетом.")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/shipment/dropoff_sc/plan_fact_group_with_ticket_with_one_planfact.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/dropoff_sc/plan_fact_group_with_ticket_midnight.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun rescheduleDuringNonWorkHoursForGroupWithTicket() {
        clock.setFixed(Instant.parse("2021-11-07T21:02:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verify(issues, never()).create(any())
    }

    private fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()
}

