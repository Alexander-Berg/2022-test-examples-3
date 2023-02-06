package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.mqm.configuration.CacheConfiguration
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate

class ExpressCourierArrivingQualityRuleProcessorTest: StartrekProcessorTest() {

    @Autowired
    private lateinit var lmsClient: LMSClient

    @Autowired
    @Qualifier("caffeineCacheManager")
    private lateinit var  cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-01-03T09:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @AfterEach
    fun clearCache(){
        cacheManager.getCache(CacheConfiguration.LMS_PHONE_FOR_PARTNER)?.clear()
    }

    @DisplayName("Создание тикета")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/express_courier_arrival/setup_create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/express_courier_arrival/verify_create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(
                Issue(null, null, "MQMEXPRESS-1", null, 1, EmptyMap(), null)
            )
        lmsClient.mockLmsPartnerNumber(987654321, "+71231231231")
        handlePlanFacts()

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val issueValues = captor.value.values

        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] Для заказа 777 от 13-09-2021 курьер Go вовремя не прибыл в магазин."
            issueValues.getOrThrow("description") shouldBe
                """
                https://abo.market.yandex-team.ru/order/777
                https://ow.market.yandex-team.ru/order/777
                https://lms-admin.market.yandex-team.ru/lom/orders/100111
                https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777
                Телефон магазина: +71231231231
                
                Дедлайн: 01-01-2020 09:00:00
                """.trimIndent()
            issueValues.getOrThrow("defectOrders") shouldBe 1
            issueValues.getOrThrow("customerOrderNumber") shouldBe "777"
            issueValues.getOrThrow("queue") shouldBe "MQMEXPRESS"
        }
    }
}
