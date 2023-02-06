package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.mqm.configuration.CacheConfiguration
import ru.yandex.market.logistics.mqm.entity.rules.payloads.ExpressOrderAssemblePayload
import ru.yandex.market.logistics.mqm.ow.OwClient
import ru.yandex.market.logistics.mqm.ow.dto.CreateCallTicketRequest
import ru.yandex.market.logistics.mqm.ow.dto.CreateCallTicketResponse
import ru.yandex.market.logistics.mqm.repository.QualityRuleRepository
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition

class ExpressOrderAssembleQualityRuleProcessorTest: StartrekProcessorTest() {

    @JvmField
    @RegisterExtension
    final val tskvLogCaptor = TskvLogCaptor(ExpressOrderAssembleQualityRuleProcessor::class.java.simpleName)

    @Autowired
    private lateinit var qualityRuleRepository: QualityRuleRepository

    @Autowired
    private lateinit var owClient: OwClient

    @Autowired
    private lateinit var lmsClient: LMSClient

    @Autowired
    @Qualifier("caffeineCacheManager")
    private lateinit var  cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-03-01T20:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @AfterEach
    fun clearCache(){
        cacheManager.getCache(CacheConfiguration.LMS_PHONE_FOR_PARTNER)?.clear()
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов cо звонком и комментарием")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)
        doReturn(CreateCallTicketResponse(TEST_OW_CALL_TICKET)).whenever(owClient)!!.createCallTicket(any())

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>)
            .containsExactly("Тестовый дш:1", "ДШ", "test-special-tag")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment).containsExactly(
            listOf(
                "Был запущен сценарий прозвона магазина о задержке сборки заказа.",
                "Номер тикета в OW: https://ow.tst.market.yandex-team.ru/entity/ticket@123."
            ).joinToString("\n")
        )
        verify(owClient).createCallTicket(
            eq(
                CreateCallTicketRequest(
                    "777",
                    "СКК Прозвон магазина по заказу 777",
                    "Заявка отправлена из сервиса MQM из-за задержки с исполнением Express заказа",
                    "test@yandex.ru",
                    TEST_PHONE
                )
            )
        )
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .contains("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов cо звонком на добавочный номер и комментарием")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_with_internal.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_with_internal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithInternalTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE, TEST_INTERNAL_PHONE)
        doReturn(CreateCallTicketResponse(TEST_OW_CALL_TICKET)).whenever(owClient)!!.createCallTicket(any())

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567 доб. 543",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment).containsExactly(
            listOf(
                "Был запущен сценарий прозвона магазина о задержке сборки заказа.",
                "Номер тикета в OW: https://ow.tst.market.yandex-team.ru/entity/ticket@123."
            ).joinToString("\n")
        )
        verify(owClient).createCallTicket(
            eq(
                CreateCallTicketRequest(
                    "777",
                    "СКК Прозвон магазина по заказу 777",
                    "Заявка отправлена из сервиса MQM из-за задержки с исполнением Express заказа",
                    "test@yandex.ru",
                    "$TEST_PHONE#$TEST_INTERNAL_PHONE"
                )
            )
        )
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .contains("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для ExpressReadyToShipWarehousePlanFactProcessor")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_warehouse.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueForExpressReadyToShipWarehousePlanFactProcessorTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)
        doReturn(CreateCallTicketResponse(TEST_OW_CALL_TICKET)).whenever(owClient)!!.createCallTicket(any())

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment).containsExactly(
            listOf(
                "Был запущен сценарий прозвона магазина о задержке сборки заказа.",
                "Номер тикета в OW: https://ow.tst.market.yandex-team.ru/entity/ticket@123."
            ).joinToString("\n")
        )
        verify(owClient).createCallTicket(
            eq(
                CreateCallTicketRequest(
                    "777",
                    "СКК Прозвон магазина по заказу 777",
                    "Заявка отправлена из сервиса MQM из-за задержки с исполнением Express заказа",
                    "test@yandex.ru",
                    TEST_PHONE
                )
            )
        )
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .contains("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для ExpressReadyToShipDropshipPlanFactProcessor")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_dropship.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueForExpressReadyToShipDropshipPlanFactProcessorTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)
        doReturn(CreateCallTicketResponse(TEST_OW_CALL_TICKET)).whenever(owClient)!!.createCallTicket(any())

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment).containsExactly(
            listOf(
                "Был запущен сценарий прозвона магазина о задержке сборки заказа.",
                "Номер тикета в OW: https://ow.tst.market.yandex-team.ru/entity/ticket@123."
            ).joinToString("\n")
        )
        verify(owClient).createCallTicket(
            eq(
                CreateCallTicketRequest(
                    "777",
                    "СКК Прозвон магазина по заказу 777",
                    "Заявка отправлена из сервиса MQM из-за задержки с исполнением Express заказа",
                    "test@yandex.ru",
                    TEST_PHONE
                )
            )
        )
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .contains("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов cо звонком и комментарием c добавочным номером")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_without_internal.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_without_internal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithInternalPhoneTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE, TEST_INTERNAL_PHONE)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567 доб. 543",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        verifyNoInteractions(owClient)
        verifyNoInteractions(comments)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов cо звонком и комментарием")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_override_phone.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_override_phone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueOverridePhoneTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)
        doReturn(CreateCallTicketResponse(TEST_OW_CALL_TICKET)).whenever(owClient)!!.createCallTicket(any())

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment).containsExactly(
            listOf(
                "Был запущен сценарий прозвона магазина о задержке сборки заказа.",
                "Номер тикета в OW: https://ow.tst.market.yandex-team.ru/entity/ticket@123."
            ).joinToString("\n")
        )
        verify(owClient).createCallTicket(
            eq(
                CreateCallTicketRequest(
                    "777",
                    "СКК Прозвон магазина по заказу 777",
                    "Заявка отправлена из сервиса MQM из-за задержки с исполнением Express заказа",
                    "test@yandex.ru",
                    "+78002002316"
                )
            )
        )
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .contains("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek, но без звонка, т.к. квота уже исчерпана")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_calls_limit_exceeded.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_calls_limit_exceeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueNoCallsBecauseLimitExceededTest() {
        clock.setFixed(Instant.parse("2021-03-01T19:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
        verifyNoInteractions(owClient)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment)
            .containsExactly("В магазин не позвонили, т.к. превышен лимит звонков.")
    }

    @DisplayName("Создание тикета в Startrek, но без звонка, т.к. превышено число активных план-фактов")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_on_flight_limit_exceeded.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_on_flight_limit_exceeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueNoCallsBecauseOfOnFlightLimit() {
        clock.setFixed(Instant.parse("2021-03-01T19:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
        verifyNoInteractions(owClient)

        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment)
            .containsExactly("В магазин не позвонили, т.к. слишком много открытых тикетов.")
    }

    @DisplayName("Создание тикета в Startrek, но без звонка, т.к. партнёра нет в списке")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_partner_not_in_call_list.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_partner_not_in_call_list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueNoCallsBecausePartnerNotInListTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek, но без звонка, т.к. партнёра исключён")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_partner_excluded_from_list.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_partner_not_in_call_list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueNoCallsBecausePartnerIsExcludedTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(2L, TEST_PHONE)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов c отключенным флоу звонков")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_with_disabled_calls.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_disabled_rule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueIfCallDisabledTest() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(TEST_SHOP_PARTNER, TEST_PHONE)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "Телефон магазина: +79681234567",
                "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        verifyNoInteractions(owClient)
        verifyNoInteractions(comments)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов, но без звонка, т.к. нет телефона")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/create_ticket_warehouse.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/create_ticket_without_call.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTestIfNoPhone() {
        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
        lmsClient.mockLmsPartnerNumber(987654321L)

        handlePlanFacts()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 не был вовремя собран партнером Тестовый дш (id: 1)")
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/1",
                "https://tariff-editor.taxi.yandex-team.ru/corp-claims?external_order_id=777",
                "", "",
                "Расчётное время вызова курьера: 01-11-2020 15:00:00",
                "Дедлайн: 01-11-2020 15:00:00"
            ).joinToString("\n")
        )

        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1)
        softly.assertThat(values.getOrThrow("tags") as Array<String?>).containsExactly("Тестовый дш:1", "ДШ")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(492)
        verifyNoInteractions(owClient)
        verifyNoInteractions(comments)
        softly.assertThat(tskvLogCaptor.results.stream().map { obj: String -> obj.trim { it <= ' ' } })
            .doesNotContain("Partner was notified about delay")
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/close_ticket.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueTest() {
        handlePlanFacts()
        val captor = argumentCaptor<IssueUpdate>()
        verify(transitions).execute(eq("MONITORINGSNDBX-1"), any<String>(), captor.capture())

        val issueUpdate = captor.lastValue
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        softly.assertThat(commentString).isEqualTo("Тикет автоматически закрыт.")
        softly.assertThat((values.getOrThrow("resolution") as ScalarUpdate<*>).set.get())
            .isEqualTo("can'tReproduce")
    }

    @DisplayName("Закрытие закрытого тикета в Startrek")
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/close_closed_ticket.xml")
    @Test
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/close_closed_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeClosedIssueTest() {
        whenever(issueStatusRef.key).thenReturn("closed")
        handlePlanFacts()
        verify(transitions, never()).execute(any<String>(), any<Transition>(), any<IssueUpdate>())
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/reopen_closed_ticket.xml")
    fun reopenClosedIssueFlowTest() {
        whenever(issueStatusRef.key).thenReturn("closed")

        val transition = mock<Transition>()
        whenever(transitions["MONITORINGSNDBX-1", "reopen"]).thenReturn(transition)

        handlePlanFacts()
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition))
    }

    @DisplayName("Оставить комментарий о совершённом звонке, если в предыдущий раз комментарий не был оставлен")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/comment_after_call.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/comment_after_call.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAboutCallTest() {
        whenever(issueStatusRef.key).thenReturn("opened")

        handlePlanFacts()
        val captorComment = argumentCaptor<CommentCreate>()
        verify(comments).create(eq("MONITORINGSNDBX-1"), captorComment.capture())
        softly.assertThat(captorComment.lastValue.comment).containsExactly(
            listOf(
                "Был запущен сценарий прозвона магазина о задержке сборки заказа.",
                "Номер тикета в OW: https://ow.tst.market.yandex-team.ru/entity/ticket@123."
            ).joinToString("\n")
        )
    }

    @DisplayName("Не создавать тикет для план-фактов, созданных неправильным producerName")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/unsupported_plan_fact_producer.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/express_assemble/unsupported_plan_fact_producer.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun ignoreUnsupportedProducer() {
        handlePlanFacts()
        verifyNoInteractions(comments)
    }

    @DisplayName("Загружать поля ExpressOrderAssemblePayload")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/express_assemble/complicated_payload.xml")
    fun loadStartrekPayloadFields() {
        val qualityRule = qualityRuleRepository.findById(1L).get()
        val payload = qualityRule.rule as ExpressOrderAssemblePayload?
        softly.assertThat(payload!!.queue).isEqualTo("MQMEXPRESS")
        softly.assertThat(payload.components).containsOnly(91242L)
        softly.assertThat(payload.onFlyLimit).isEqualTo(300L)
        softly.assertThat(payload.perDayLimit).isEqualTo(400L)
        softly.assertThat(payload.partnersExcludeId).containsOnly(139810L, 139815L)
        softly.assertThat(payload.partnersId).containsOnly(139810L, 139813L)
        softly.assertThat(payload.enableCallDropshipFlow).isEqualTo(true)
        softly.assertThat(payload.overrideEmail).isEqualTo("newEmail@com")
        softly.assertThat(payload.overridePhone).isEqualTo("newPhone")
        softly.assertThat(payload.closeTransitions).containsOnly("close", "closed")
        softly.assertThat(payload.tags).containsOnly("tag1", "tag2")
        softly.assertThat(payload.createTicketEnabled).isEqualTo(false)
        softly.assertThat(payload.allowedToCloseAssigned).isEqualTo(false)
        softly.assertThat(payload.checkTicketDuplicate).isEqualTo(true)
    }

    companion object {
        const val TEST_SHOP_PARTNER = 987654321L
        private const val TEST_OW_CALL_TICKET = "https://ow.tst.market.yandex-team.ru/entity/ticket@123"
        private const val TEST_PHONE = "+79681234567"
        private const val TEST_INTERNAL_PHONE = "543"
    }
}
