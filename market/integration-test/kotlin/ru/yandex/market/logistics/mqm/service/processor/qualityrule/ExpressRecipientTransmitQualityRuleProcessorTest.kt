package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.mqm.configuration.CacheConfiguration
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition

@DisplayName("Тесты обработчика дедлайнов ExpressRecipientTransmit")
class ExpressRecipientTransmitQualityRuleProcessorTest : StartrekProcessorTest() {

    @Autowired
    private lateinit var lmsClient: LMSClient

    @Autowired
    @Qualifier("caffeineCacheManager")
    private lateinit var  cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T18:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }
    @AfterEach
    fun clearCache(){
        cacheManager.getCache(CacheConfiguration.LMS_PHONE_FOR_PARTNER)?.clear()
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express-recipient-transmit-create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express-recipient-transmit-create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(987654321, "+71231231231")
        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary")).isEqualTo("[MQM] Заказ 777 не был вовремя вручен пользователю")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            """
            Дедлайн: 01-11-2020 15:00:00
            https://abo.market.yandex-team.ru/order/777
            https://ow.market.yandex-team.ru/order/777
            https://lms-admin.market.yandex-team.ru/lom/orders/1
            https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777
            Телефон магазина: +71231231231
            """.trimIndent()
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(494)
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов, после получения чекпоинта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express-recipient-transmit-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express-recipient-transmit-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueTest() {
        handlePlanFacts()
        val captor = argumentCaptor<IssueUpdate>()
        verify(transitions).execute(any<String>(), any<String>(), captor.capture())

        val issueUpdate = captor.lastValue

        softly.assertThat((issueUpdate.values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
        softly.assertThat(issueUpdate.comment.get().comment.get()).isEqualTo("Тикет автоматически закрыт.")
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express-recipient-transmit-reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")

        val transition = mock<Transition>()
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handlePlanFacts()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }
}
