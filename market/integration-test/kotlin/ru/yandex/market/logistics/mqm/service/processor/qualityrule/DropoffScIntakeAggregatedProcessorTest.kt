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
import org.mockito.Mockito
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

internal class DropoffScIntakeAggregatedProcessorTest : StartrekProcessorTest() {

    @Autowired
    private lateinit var pvzContactInformationCache: PvzContactInformationCache

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта приемки ДО-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropoff_sc/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        val summary = "[MQM] 07.11.2020: СЦ-партнеры вовремя не приняли заказы, " +
                "отгруженные ДО Тестовый Дропофф (id: 12345678)."
        val description =
            """
                https://abo.market.yandex-team.ru/order/777
                https://ow.market.yandex-team.ru/order/777
                https://lms-admin.market.yandex-team.ru/lom/orders/100111
                
                Дата создания заказа: 01-11-2020
                Дедлайн приемки на СЦ Тестовый сц: 07-11-2020 10:00:00
                
                Трек ДО: 101
                Трек СЦ Тестовый сц: 102
                Адрес точки доставки ДО: Россия Московская область Красногорск Светлая улица 3А 143409
                Email точки доставки ДО: email1@mail.com
            """.trimIndent()

        val tags = arrayOf(
            "Тестовый Дропофф:СЦ",
            "Тестовый сц:СЦ",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
        )

        createIssue(summary, description, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для одного просроченного план-факта приемки ДО-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup(
                "/service/processor/qualityrule/before/intake/dropoff_sc/create_ticket_with_one_planfact.xml"
            ),
            DatabaseSetup(
                value = [
                    "/service/processor/qualityrule/before/intake/dropoff_sc/go_order_one_planfact.xml"
                ],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/go_order_create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07.11.2020: СЦ-партнеры вовремя не приняли заказы, " +
                "отгруженные ДО Тестовый Дропофф (id: 12345678)."
        val description =
            """
                https://abo.market.yandex-team.ru/order/777
                
                https://lms-admin.market.yandex-team.ru/lom/orders/100111
                
                Дата создания заказа: 01-11-2020
                Дедлайн приемки на СЦ Тестовый сц: 07-11-2020 10:00:00
                
                Трек ДО: 101
                Трек СЦ Тестовый сц: 102
                Адрес точки доставки ДО: Россия Московская область Красногорск Светлая улица 3А 143409
                Email точки доставки ДО: email1@mail.com
            """.trimIndent()

        val tags = arrayOf(
            "yandex_go-доставка_наружу",
            "Тестовый Дропофф:СЦ",
            "Тестовый сц:СЦ",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
        )

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
            issueValues.getOrThrow(FIELD_CUSTOMER_EMAIL) shouldBe "email1@mail.com"
        }
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов приемки ДО-СЦ")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropoff_sc/create_ticket_with_some_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueTest() {
        val summary = "[MQM] 07.11.2020: СЦ-партнеры вовремя не приняли заказы, " +
                "отгруженные ДО Тестовый Дропофф (id: 1)."
        val tags = listOf(
            "Тестовый Дропофф:СЦ",
            "Тестовый сц1:СЦ",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
            "Тестовый сц2:СЦ",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 5А 143409",
            "Лог. точка:10001687232",
        )
        createAggregatedIssue(summary, tags)
    }

    @DisplayName("Заказ YANDEX_GO. Создание тикета в Startrek для просроченных план-фактов приемки ДО-СЦ")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup(
                "/service/processor/qualityrule/before/intake/dropoff_sc/create_ticket_with_some_planfacts.xml"
            ),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/intake/dropoff_sc/go_order_some_planfacts.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/go_order_create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateAggregatedIssueTest() {
        val summary = "[MQM][Доставка Наружу] 07.11.2020: СЦ-партнеры вовремя не приняли заказы, " +
                "отгруженные ДО Тестовый Дропофф (id: 1)."
        val tags = listOf(
            "yandex_go-доставка_наружу",
            "Тестовый Дропофф:СЦ",
            "Тестовый сц1:СЦ",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
            "Лог. точка:10001687231",
            "Тестовый сц2:СЦ",
            "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 5А 143409",
            "Лог. точка:10001687232",
        )
        createAggregatedIssue(summary, tags)
    }

    private fun createAggregatedIssue(summary: String, tags: List<String>) {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(attachments.upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(attachment)
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687231L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone12"))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687232L)))
            .thenReturn(PvzContactInformation(2, "email2@mail.com", "phone21", "phone22"))
        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values
        verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
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

    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов приемки ДО-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropoff_sc/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
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

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов приемки ДО-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropoff_sc/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/close_all_planfacts.xml",
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

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropoff_sc/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = Mockito.mock(
            Transition::class.java
        )
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)
        handleGroups()
        verify(transitions, Mockito.times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Решедулить группу если сейчас не время создания тикетов")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropoff_sc/create_ticket_with_some_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropoff_sc/rescheduled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun rescheduleIssueFlowTest() {
        clock.setFixed(Instant.parse("2021-03-01T07:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handleGroups()
        verify(issues, Mockito.never()).create(ArgumentMatchers.any())
    }

    private fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()
}
