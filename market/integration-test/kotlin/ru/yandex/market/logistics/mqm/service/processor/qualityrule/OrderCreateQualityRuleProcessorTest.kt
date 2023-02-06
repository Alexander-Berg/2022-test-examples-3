package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseFlow
import ru.yandex.market.logistics.mqm.utils.clearCache
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO
import ru.yandex.startrek.client.model.CollectionUpdate
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition

@DisplayName("Тесты обработчика для очереди OrderCreate")
internal class OrderCreateQualityRuleProcessorTest: StartrekProcessorTest() {
    @Autowired
    @Qualifier("caffeineCacheManager")
    private lateinit var caffeineCacheManager: CacheManager

    @Autowired
    @Qualifier("mbiApiClientLogged")
    private lateinit var mbiApiClient: MbiApiClient

    @BeforeEach
    fun setUp() {
        clearCache(caffeineCacheManager)
        clock.setFixed(Instant.parse("2021-03-01T20:00:50.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        whenever(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(BusinessOwnerDTO(1, 2, "test", TEST_EMAILS))
        clock.setFixed(Instant.parse("2020-11-07T16:00:50.00Z"), DateTimeUtils.MOSCOW_ZONE)
        val description =
            """
            https://abo.market.yandex-team.ru/order/777
            https://ow.market.yandex-team.ru/order/777
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
        
            Номер заказа: 777
            Дата создания заказа: 01-11-2020
            Дедлайн трека ДШ Почта: 07-11-2020 11:11:50
            Дата отгрузки: 02-11-2020
            Cutoff: 02-11-2020 23:59:00
            """.trimIndent()
        createAndCheckIssue(
            summary = "[MQM] Заказ 777 от 01-11-2020 вовремя не получил трек от ДШ Почта",
            description = description,
            tags = listOf("Почта:987654321", "ДШ"),
            email = "test1@mail.com",
            shopId = TEST_SHOP_ID
        )
    }

    @DisplayName("Не создаём тикета в Startrek для просроченных план-фактов от стрельбовых заказов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-shooting.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-shooting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTestShooting() {
        clock.setFixed(Instant.parse("2020-11-07T16:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        handlePlanFacts()
        verify(issues, never()).create(any())
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов, после получения трэка")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueTest() {
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(any<String>(), any<String>(), captor.capture())
        val issueUpdate = captor.value
        assertSoftly {
            (issueUpdate.values.getOrThrow("resolution") as ScalarUpdate<*>).set.get() shouldBe "fixed"
            issueUpdate.comment.get().comment.get() shouldBe "Тикет автоматически закрыт."
        }
    }

    @DisplayName("Попытка закрыть тикет, который уже был закрыт вручную")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeClosedIssueTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        handlePlanFacts()
        verify(transitions, never()).execute(any<String>(), any<Transition>(), any<IssueUpdate>())
    }

    @DisplayName("При достижении последней попытки на обработку тега, план-факт отмечается обработанным")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-tag_last_attempt.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-tag_last_attempt.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun lastAttemptForSettingTagTest() {
        handlePlanFacts()
    }

    @DisplayName("Выставление тега тикету")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-tag_set.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-tag_set.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun setTagTest() {
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(issues, times(3)).update(any<String>(), captor.capture())
        val issueUpdate = captor.allValues
        val actualTags = (issueUpdate[1].values.getOrThrow("tags") as CollectionUpdate<*>).add.toSet()
        val actualProcessId = (issueUpdate[2].values.getOrThrow("processId") as ScalarUpdate<*>).set[0]
        assertSoftly {
            actualTags shouldContainExactly setOf("ошибка_фронта", "Почта:123", "BUSINESS_ERROR", "FF-TAG")
            actualProcessId shouldBe 123L
        }
    }

    @DisplayName("Выставление тегов клиентов платформы тикетам при создании")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-tag_platform_client.xml")
    fun setPlatformClientTagTest() {
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues, times(3)).create(captor.capture())
        val issueCreates = captor.allValues
        val expectedTags = sequenceOf(
            setOf("DBS", "Почта:123", "ФФ"),
            setOf("yandex_go-доставка_наружу", "Почта:123", "ФФ"),
            setOf("FAAS", "Почта:123", "ФФ"),
        )
        assertSoftly {
            issueCreates.asSequence()
                .map { (it.values.getOrThrow("tags") as Array<*>).toSet() }
                .zip(expectedTags)
                .forEach { (actualTags, expectedTags) -> actualTags shouldContainExactly expectedTags }
        }
    }

    @DisplayName("Выставление тега тикету, но без добавления специальных тэгов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-tag_set_unknown_partner.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-tag_set.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun setTagTestUnknownPartner() {
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(issues, times(3)).update(any<String>(), captor.capture())

        val issueUpdate = captor.allValues
        val actualTags = (issueUpdate[1].values.getOrThrow("tags") as CollectionUpdate<*>).add.toLinkedHashSet()
        val actualProcessId = (issueUpdate[2].values.getOrThrow("processId") as ScalarUpdate<*>).set[0]
        val actualSummary = (issueUpdate[0].values.getOrThrow("summary") as ScalarUpdate<*>).set[0]
        assertSoftly {
            actualTags shouldContainExactly setOf("ошибка_фронта", "BUSINESS_ERROR")
            actualProcessId shouldBe 123L
            actualSummary shouldBe SUMMARY_WITH_PROCESS_ID
        }
    }

    @DisplayName("Добавить информацию об ошибке в тикет, если не смогли классифицировать")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-error_comment.xml")
    fun commentErrorTest() {
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(
            CommentCreate::class.java
        )
        verify(comments).create(eq("MONITORINGSNDBX-1"), captor.capture())
        val commentCreate = captor.value
        commentCreate.comment.get() shouldBe "Сообщение об ошибке: ABC–XYZ."
    }

    @DisplayName("Установление максимального приоритета тикету")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-priority_set_last.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-priority_set_last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun setLastPriorityTest() {
        clock.setFixed(Instant.parse("2020-11-02T16:00:50.00Z"), DateTimeUtils.MOSCOW_ZONE)
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(issues).update(any<String>(), captor.capture())
        val issueUpdate = captor.value
        (issueUpdate.values.getOrThrow("priority") as ScalarUpdate<*>).set.get() shouldBe "blocker"
    }

    @DisplayName("Закрытие тикета в Startrek для неактуальных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-close_not_actual_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-close_not_actual_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueForExpiredPlanFactTest() {
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(any<String>(), any<String>(), captor.capture())
        val issueUpdate = captor.value
        assertSoftly {
            (issueUpdate.values.getOrThrow("resolution") as ScalarUpdate<*>).set.get() shouldBe "fixed"
            issueUpdate.comment.get().comment.get() shouldBe "Тикет автоматически закрыт."
        }
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-create_ticket_wo_shipment.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-create_ticket_wo_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithoutShipmentDateTest() {
        val description = java.lang.String.join(
            "\n",
            "https://abo.market.yandex-team.ru/order/777",
            "https://ow.market.yandex-team.ru/order/777",
            "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
            "",
            "Номер заказа: 777",
            "Дата создания заказа: 01-11-2020",
            "Дедлайн трека ФФ Почта: 07-11-2020 11:11:50"
        )
        createAndCheckIssue(SUMMARY, description)
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-reopen_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        val transition = mock(Transition::class.java)
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)
        handlePlanFacts()
        verify(transitions).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/order_create-create_ticket.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/go_order_create-create_ticket.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithoutOwLinkTest() {
        whenever(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(BusinessOwnerDTO(1, 2, "test", TEST_EMAILS))
        clock.setFixed(Instant.parse("2020-11-07T16:00:50.00Z"), DateTimeUtils.MOSCOW_ZONE)
        val description =
            """
            https://abo.market.yandex-team.ru/order/777
            
            https://lms-admin.market.yandex-team.ru/lom/orders/100111
        
            Номер заказа: 777
            Дата создания заказа: 01-11-2020
            Дедлайн трека ДШ Почта: 07-11-2020 11:11:50
            Дата отгрузки: 02-11-2020
            Cutoff: 02-11-2020 23:59:00
            """.trimIndent()
        createAndCheckIssue(
            summary = "[MQM] Заказ 777 от 01-11-2020 вовремя не получил трек от ДШ Почта",
            description = description,
            tags = listOf("Почта:987654321", "ДШ", "yandex_go-доставка_наружу"),
            email = "test1@mail.com",
            shopId = TEST_SHOP_ID
        )
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/order_create-create_ticket_processId.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/order_create-create_ticket_processId.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithProcessIdTest() {
        clock.setFixed(Instant.parse("2020-11-07T16:00:50.00Z"), DateTimeUtils.MOSCOW_ZONE)
        val description = java.lang.String.join(
            "\n",
            "https://abo.market.yandex-team.ru/order/777",
            "https://ow.market.yandex-team.ru/order/777",
            "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
            "",
            "Номер заказа: 777",
            "Дата создания заказа: 01-11-2020",
            "Дедлайн трека ФФ Почта: 07-11-2020 11:11:50",
            "Дата отгрузки: 02-11-2020",
            "Cutoff: 02-11-2020 23:59:00"
        )
        createAndCheckIssue(SUMMARY_WITH_PROCESS_ID, description)
    }

    private fun createAndCheckIssue(
        summary: String,
        description: String,
        tags: List<String> = listOf("Почта:987654321", "ФФ"),
        email: String? = null,
        shopId: Long? = null,
    ) {
        val issue = Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
        whenever(issues.create(any())).thenReturn(issue)
        handlePlanFacts()
        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        val values = captor.value.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe summary
            values.getOrThrow("description") shouldBe description
            values.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
            values.getOrThrow("customerOrderNumber") shouldBe "777"
            (values.getOrThrow("tags") as Array<*>).asList() shouldContainExactlyInAnyOrder tags
            (values.getOrThrow("components") as LongArray).asList() shouldContainExactly listOf(42L)
            values.getOrElse(BaseFlow.FIELD_CUSTOMER_EMAIL, null) shouldBe email
            values.getOrElse(BaseFlow.FIELD_SHOP_ID, null) shouldBe shopId

        }
    }

    companion object {
        private const val TEST_SHOP_ID = 12345678L
        private val TEST_EMAILS = setOf("test1@mail.com", "test2@mail.com")
        private const val SUMMARY = "[MQM] Заказ 777 от 01-11-2020 вовремя не получил трек от ФФ Почта"
        private const val SUMMARY_WITH_PROCESS_ID =
            "[MQM] Заказ 777, taskId=[123] от 01-11-2020 вовремя не получил трек от ФФ Почта"
    }
}
