package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalTime
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.MapF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseFlow
import ru.yandex.market.logistics.mqm.service.yt.PvzContactInformationCache
import ru.yandex.market.logistics.mqm.service.yt.dto.PvzContactInformation
import ru.yandex.market.logistics.mqm.utils.clearCache
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import ru.yandex.startrek.client.model.Update

class DropshipDropoffIntakeAggregatedProcessorTest: StartrekProcessorTest() {

    @Autowired
    @Qualifier("caffeineCacheManager")
    private lateinit var caffeineCacheManager: CacheManager

    @Autowired
    @Qualifier("mbiApiClientLogged")
    private lateinit var mbiApiClient: MbiApiClient

    @Autowired
    private lateinit var pvzContactInformationCache : PvzContactInformationCache

    @Autowired
    private lateinit var lmsClient: LMSClient

    @BeforeEach
    fun setUp() {
        clearCache(caffeineCacheManager)
        clock.setFixed(Instant.parse("2021-03-01T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта приемки ДШ-ДО")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_dropoff/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_dropoff/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        whenever(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(BusinessOwnerDTO(1, 2, "test_login", TEST_EMAILS))
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687231L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone21"))
        mockLogisticsPointsResponse(10003345341L)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())

        val issueValues = captor.value.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] 07.11.2020: ДО-партнеры вовремя не приняли заказы, " +
                "отгруженные ДШ Тестовый dropship (id: 12345678)."
            issueValues.getOrThrow("description") shouldBe
                """
                https://abo.market.yandex-team.ru/order/777
                https://ow.market.yandex-team.ru/order/777
                https://lms-admin.market.yandex-team.ru/lom/orders/100111

                Дата создания заказа: 01-11-2020
                Дедлайн приемки на ДО Тестовый дропофф: 07-11-2020 10:00:00

                Трек ДШ: 101
                Трек ДО Тестовый дропофф: 102
                Адрес точки доставки ДО: Россия Московская область Красногорск Светлая улица 3А 143409
                Email точки доставки ДО: email1@mail.com
                Email главного представителя: test1@mail.com, test2@mail.com
                Расписание ДШ: пн 10:00-19:00;вт 10:00-18:00
                """.trimIndent()
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777"
            issueValues.getOrThrow("defectOrders") shouldBe 1
            issueValues.getOrThrow("idMagazina") shouldBe TEST_SHOP_ID
            issueValues.getOrThrow("tags") shouldBe arrayOf(
                "Тестовый dropship:ДШ",
                "Тестовый дропофф:СЦ",
                "опоздание_по_вине_партнера",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "Лог. точка ДО:10001687231",
                "Лог. точка ДШ:10003345341",
            )
        }
        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID)
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов приемки ДШ-ДО вина дропшипа")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropship_dropoff/create_ticket_with_some_planfacts.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_dropoff/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueDropshipFailTest() {
        createAggregatedIssue(
            listOf(
                "Тестовый dropship:ДШ",
                "Тестовый дропофф 2:СЦ",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 5А 143409",
                "Тестовый дропофф 1:СЦ",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "опоздание_по_вине_партнера",
                "Лог. точка ДО:10001687231",
                "Лог. точка ДО:10001687232",
                "Лог. точка ДШ:10003345341",
                "Лог. точка ДШ:10003345345",
            )
        )
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов приемки ДШ-ДО вина дропофа")
    @Test
    @DatabaseSetup(
        value = [
            "/service/processor/qualityrule/before/intake/dropship_dropoff/create_ticket_with_some_planfacts.xml",
            "/service/processor/qualityrule/before/intake/dropship_dropoff/dr_do_transportation.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_dropoff/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssueDropoffFailTest() {
        createAggregatedIssue(
            listOf(
                "Тестовый dropship:ДШ",
                "Тестовый дропофф 2:СЦ",
                "Тестовый дропофф 1:СЦ",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 5А 143409",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "Лог. точка ДО:10001687232",
                "Лог. точка ДО:10001687231",
                "Лог. точка ДШ:10003345341",
                "Лог. точка ДШ:10003345345",
            )
        )
    }

    private fun createAggregatedIssue(tags: List<String>) {
        whenever(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(BusinessOwnerDTO(1, 2, "test_login", TEST_EMAILS))
        val attachment = mock(Attachment::class.java)
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        whenever(attachments.upload(anyString(), any())).thenReturn(attachment)
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687231L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone12"))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687232L)))
            .thenReturn(PvzContactInformation(2, "email2@mail.com", "phone21", "phone22"))
        mockLogisticsPointsResponse(10003345341L)
        mockLogisticsPointsResponse(10003345345L)

        handleGroups()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values
        verify(attachments).upload(anyString(), any())
        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] 07.11.2020: ДО-партнеры вовремя не приняли заказы, " +
                    "отгруженные ДШ Тестовый dropship (id: 12345678)."
            issueValues.getOrThrow("description") shouldBe
                    """
                        Список заказов в приложении (2 шт.)
                        Расписание ДШ: пн 10:00-19:00;вт 10:00-18:00
                    """.trimIndent()
            issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777, 778"
            (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder tags
            issueValues.getOrThrow("defectOrders") shouldBe 2
            issueValues.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL) shouldBe "test1@mail.com"
            issueValues.getOrThrow("idMagazina") shouldBe TEST_SHOP_ID
        }

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID)
    }

    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов приемки ДШ-ДО")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_dropoff/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_dropoff/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        whenever(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(BusinessOwnerDTO(1, 2, "test_login", TEST_EMAILS))
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

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов приемки ДШ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_dropoff/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_dropoff/close_all_planfacts.xml",
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
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_dropoff/reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = mock(
            Transition::class.java
        )
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)
        handleGroups()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    private fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()

    private fun createScheduleDayResponse() = setOf(
        ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(19, 0)),
        ScheduleDayResponse(2L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0)),
    )

    private fun mockLogisticsPointsResponse(logisticsPointId: Long) {
        whenever(lmsClient.getLogisticsPoint(logisticsPointId))
            .thenReturn(
                Optional.ofNullable(
                    LogisticsPointResponse.newBuilder()
                        .schedule(
                            createScheduleDayResponse()
                        )
                        .build()
                )
            )
    }

    companion object{
        private const val TEST_SHOP_ID = 12345678L
        private val TEST_EMAILS = setOf("test1@mail.com", "test2@mail.com")
    }
}
