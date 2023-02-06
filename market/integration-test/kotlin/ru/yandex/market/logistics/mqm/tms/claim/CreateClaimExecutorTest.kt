package ru.yandex.market.logistics.mqm.tms.claim

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.CacheConfiguration
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.monitoringevent.event.EventType
import ru.yandex.market.logistics.mqm.monitoringevent.payload.AbstractMonitoringEventPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.BaseCreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.service.ClaimIdGenerator
import ru.yandex.market.logistics.mqm.service.ClaimService
import ru.yandex.market.logistics.mqm.service.enums.ClaimPartnerType
import ru.yandex.market.logistics.mqm.service.monitoringevent.MonitoringEventService
import ru.yandex.market.logistics.mqm.service.yt.dto.ClaimInfo
import ru.yandex.market.logistics.mqm.service.yt.dto.YtClaimItemInfo
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import ru.yandex.market.logistics.werewolf.client.WwClient

@DisplayName("Тест джобы создания тикетов в очередь MQMCLAIM")
class CreateClaimExecutorTest : AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @SpyBean
    private lateinit var monitoringEventService: MonitoringEventService<AbstractMonitoringEventPayload>

    @SpyBean
    private lateinit var claimIdGenerator: ClaimIdGenerator

    @Autowired
    private lateinit var notReturnedClaimExecutor: CreateNotReturnedClaimExecutor

    @Autowired
    private lateinit var fbyNotReturnedClaimExecutor: CreateFbyNotReturnedClaimExecutor

    @Autowired
    private lateinit var partnersClaimExecutor: CreatePartnersClaimExecutor

    @Autowired
    private lateinit var wwClient: WwClient

    @Autowired
    private lateinit var claim:ClaimService

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private lateinit var yqlJdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var lmsClient: LMSClient

    @Autowired
    private lateinit var cacheManager: CacheManager

    private val payloadCaptor: KArgumentCaptor<CreateStartrekIssueForClaimPayload> = KArgumentCaptor(
        ArgumentCaptor.forClass(CreateStartrekIssueForClaimPayload::class.java),
        CreateStartrekIssueForClaimPayload::class
    )

    @BeforeEach
    fun resetCache() {
        cacheManager.getCache(CacheConfiguration.LMS_LEGAL_INFO)?.clear()
    }

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг" +
            " для партнёров. Проверка получения имени из YT, если в lms не найдено"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup.xml")
    fun successPartnerTicketCreationWithYtLegalName() {
        val orderId = 1L
        val checkouterOrderId = 1001L
        val partnerId = 302L
        val partnerName = "ООО Какая-то ПВЗ из LMS 1" //"Какая-то ПВЗ из YT 1"
        val address = "Адрес ПВЗ из YT 1"

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<*>>()))
            .thenReturn(
                listOf(createClaimInfo(orderId, checkouterOrderId, partnerId, partnerName, address)),
            )

        whenever(lmsClient.getPartnerLegalInfo(eq(302))).thenReturn(
            Optional.of(
                createEmptyLegalNameInfoResponse(orderId, partnerId)
            )
        )

        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        partnersClaimExecutor.run()
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_PARTNERS)
        val log = backLogCaptor.results.toString()

        val createClaimTicketPayload = createPayload("Партнеру 302 будет выставлена претензия за просрочки")
        assertSoftly {
            payloadCaptor.firstValue.partnerInfo?.legalName shouldBe createClaimTicketPayload.partnerInfo?.legalName
            log shouldContain logMessageExpected
        }
    }

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг" +
            " для партнёров. Проверка передачи комментария о пустом legal name"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup.xml")
    fun successPartnerTicketCreationWithCommentEmptyLegalName() {
        val orderId = 1L
        val checkouterOrderId = 1001L
        val partnerId = 302L
        val partnerName = ""
        val address = "Адрес ПВЗ из YT 1"

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<*>>()))
            .thenReturn(
                listOf(createClaimInfo(orderId, checkouterOrderId, partnerId, partnerName, address)),
            )

        whenever(lmsClient.getPartnerLegalInfo(eq(302))).thenReturn(
            Optional.of(
                createEmptyLegalNameInfoResponse(orderId, partnerId)
            )
        )

        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        partnersClaimExecutor.run()
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_PARTNERS)
        val log = backLogCaptor.results.toString()

        assertSoftly {
            payloadCaptor.firstValue.comments[0] shouldBe "Партнер  с идентификатором 302 не имеет legal_info!"
            log shouldContain logMessageExpected
        }
    }

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг" +
            " для партнёров"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup.xml")
    @ExpectedDatabase(value = "/service/claim/claim-creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun successPartnerTicketCreation() {
        val orderId = 1L
        val checkouterOrderId = 1001L
        val partnerId = 302L
        val partnerName = "Какая-то ПВЗ из YT 1"
        val address = "Адрес ПВЗ из YT 1"

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Any>>()))
            .thenReturn(
                listOf(createClaimInfo(orderId, checkouterOrderId, partnerId, partnerName, address)),
                listOf((orderId + 1).toString())
            )

        whenever(lmsClient.getPartnerLegalInfo(eq(302))).thenReturn(
            Optional.of(
                createLegalInfoResponse(orderId, partnerId)
            )
        )

        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        partnersClaimExecutor.run()
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_PARTNERS)
        val log = backLogCaptor.results.toString()
        assertSoftly {
            payloadCaptor.firstValue shouldBeEqualToComparingFields createPayload("Партнеру 302 будет выставлена претензия за просрочки")
            log shouldContain logMessageExpected
        }
    }

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг" +
            " для 'просроченных' возвратов"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup.xml")
    fun successNotReturnedTicketCreation() {
        val orderId = 1L
        val checkouterOrderId = 1001L
        val partnerId = 302L
        val partnerName = "Какая-то ПВЗ из YT 1"
        val address = "Адрес ПВЗ из YT 1"

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Any>>()))
            .thenReturn(
                listOf(createClaimInfo(orderId, checkouterOrderId, partnerId, partnerName, address)),
                listOf((orderId + 1).toString())
            )


        whenever(lmsClient.getPartnerLegalInfo(eq(302))).thenReturn(
            Optional.of(
                createLegalInfoResponse(orderId, partnerId)
            )
        )
        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        notReturnedClaimExecutor.run()
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_NOT_RETURNED)
        val log = backLogCaptor.results.toString()
        assertSoftly {
            payloadCaptor.firstValue shouldBeEqualToComparingFields createPayloadNotReturned("Партнеру TESTNAME будет выставлена претензия за возврат заказов больше 32 дней ")
            log shouldContain logMessageExpected
        }
    }

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг" +
            " для 'просроченных' FBY возвратов"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup.xml")
    fun successNotReturnedFBYTicketCreation() {
        val orderId = 1L
        val partnerId = 1L

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Any>>()))
            .thenReturn(
                listOf(createYtOrderInfo(orderId, partnerId)),
                listOf((orderId + 1).toString())
            )

        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        fbyNotReturnedClaimExecutor.run()
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_NOT_RETURNED, 0)
        val log = backLogCaptor.results.toString()
        assertSoftly {
            payloadCaptor.firstValue shouldBeEqualToComparingFields createPayloadNotReturnedFby("Партнеру TESTNAME будет выставлена претензия за возврат заказов больше 32 дней ")
            log shouldContain logMessageExpected
        }
    }

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг" +
            " для 'просроченных' возвратов"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup_claim_with_deleted.xml")
    @ExpectedDatabase(value = "/tms/claim/createClaimExecutor/before/after/setup_claim_with_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun successNotReturnedTicketCreationWithDeletion() {
        val orderId = 1L
        val checkouterOrderId = 1001L
        val partnerId = 302L
        val partnerName = "Какая-то ПВЗ из YT 1"
        val address = "Адрес ПВЗ из YT 1"

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Any>>()))
            .thenReturn(
                listOf(createClaimInfo(orderId, checkouterOrderId, partnerId, partnerName, address)),
                listOf((orderId + 1).toString())
            )


        whenever(lmsClient.getPartnerLegalInfo(eq(302))).thenReturn(
            Optional.of(
                createLegalInfoResponse(orderId, partnerId)
            )
        )
        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        notReturnedClaimExecutor.run()
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_NOT_RETURNED)
        val log = backLogCaptor.results.toString()
        assertSoftly {
            payloadCaptor.firstValue shouldBeEqualToComparingFields createPayloadNotReturned("Партнеру TESTNAME будет выставлена претензия за возврат заказов больше 32 дней ")
            log shouldContain logMessageExpected
        }
    }

    @Test
    @DisplayName(
        "Не создалась тасок на создание тикетов в очередь MQMCLAIM через событийный мониторинг, т.к. нет нужных данных, они исключились." +
            " для 'просроченных' возвратов"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup.xml")
    fun unsuccessNotReturnedTicketCreationEmptyResult() {
        val orderId = 1L
        val checkouterOrderId = 1001L
        val partnerId = 302L
        val partnerName = "Какая-то ПВЗ из YT 1"
        val address = "Адрес ПВЗ из YT 1"

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Any>>()))
            .thenReturn(
                listOf(createClaimInfo(orderId, checkouterOrderId, partnerId, partnerName, address)),
                listOf(orderId.toString())
            )


        whenever(lmsClient.getPartnerLegalInfo(eq(302))).thenReturn(
            Optional.of(
                createLegalInfoResponse(orderId, partnerId)
            )
        )

        clock.setFixed(Instant.parse("2021-08-17T16:00:00.00Z"), MOSCOW_ZONE)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(CLAIM_DATA_STUB)
        whenever(claimIdGenerator.generateClaimId()).thenReturn(CLAIM_ID)

        notReturnedClaimExecutor.run()
        verify(monitoringEventService, never()).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )

    }

    private fun createClaimInfo(
        orderId: Long,
        checkouterOrderId: Long,
        partnerId: Long,
        partnerName: String,
        address: String
    ) = ClaimInfo(
        orderId = orderId,
        checkouterOrderId = checkouterOrderId,
        partnerId = partnerId,
        partnerType = ClaimPartnerType.PICKUP_POINT,
        partnerSubTypeName = "Подтип",
        partnerName = partnerName,
        address = address,
        email = "email@email.com",
        assessedCostRub = BigDecimal.valueOf(101.01),
        creationDateTime = Instant.parse("2021-12-20T17:00:00Z"),
        shipmentDateTime = Instant.parse("2021-12-20T17:00:00Z"),
        partnerSubtypeName = "Подтип партнера",
        legalPartnerName = "Юридическое имя"
    )

    private fun createYtOrderInfo(
        orderId: Long = 1,
        partnerId: Long = 1,
    ) = YtClaimItemInfo(
        orderId = orderId.toString(),
        itemCost = BigDecimal.valueOf(1231.01),
        partnerId = partnerId,
        supplierId = 1L,
        ssku = "vendorCode",
        itemCount = 1L,
        itemId = 11241123L,
    )

    private fun createLegalInfoResponse(
        orderId: Long,
        partnerId: Long
    ) = LegalInfoResponse(
        orderId, partnerId, "ООО Какая-то ПВЗ из LMS 1", null, null, null, null, null,
        Address.newBuilder().addressString("Legal адрес ПВЗ из LMS 1").build(),
        Address.newBuilder().addressString("Почтовый адрес ПВЗ из LMS 1").build(),
        null, null, null, null
    )

    private fun createLogMessage(code: CreateClaimEventCode, partnerId: Long = 302): String {
        val logMessageExpected = "level=INFO\t" +
            "format=plain\t" +
            "code=$code\t" +
            "payload=Triggered ticket creation in MQMTESTCLAIM queue for partner\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=CLAIM_MONITORING_EVENT\t" +
            "extra_keys=partnerId\t" +
            "extra_values=$partnerId\n"
        return logMessageExpected
    }

    private fun createPayload(string: String?): CreateStartrekIssueForClaimPayload = CreateStartrekIssueForClaimPayload(
        queue = "MQMTESTCLAIM",
        summary = string ?: "Партнеру 302 будет выставлена претензия за просрочки",
        description = "Заказы в выставленной претензии: \n1",
        fields = mapOf(
            "deliveryName" to "ООО Какая-то ПВЗ из LMS 1",
            "amountClaimed" to "101.01 RUB",
            ("mainEmail" to "email@email.com")
        ),
        csvAttachments = setOf(
            BaseCreateStartrekIssuePayload.CsvAttachment("2021-08-17",  listOf(
                mapOf(
                    "orderId" to "1001",
                    "shipmentDate" to "2021-12-20T17:00:00Z",
                    "cost" to BigDecimal( "101.01"),
                    "previousStatus" to "COMPENSATE_SD",
                    "deliveryService" to "Какая-то ПВЗ из YT 1",
                    "address" to "Адрес ПВЗ из YT 1",
                    "partner_id" to 302L,
                    "partner_subtype_name" to  "Подтип партнера",
                    "partner_name" to "Какая-то ПВЗ из YT 1",
                    "legal_partner_name" to "Юридическое имя"
                )
            ))),
        entities = setOf(BaseCreateStartrekIssuePayload.Entity("1", IssueLinkEntityType.CLAIM)),
        claimData = claim.collectClaimDataForYtOrders(
            AbstractCreateClaimExecutor.PartnerInfo(
                302,
                "Какая-то ПВЗ из YT 1",
                "ООО Какая-то ПВЗ из LMS 1",
                "Legal адрес ПВЗ из LMS 1"
            ),
            listOf(
                ClaimInfo(
                    1,
                    1001,
                    302,
                    ClaimPartnerType.PICKUP_POINT,
                    "Подтип",
                    "Какая-то ПВЗ из YT 1",
                    "Адрес ПВЗ из YT 1",
                    "email@email.com",
                    BigDecimal("101.01"),
                    LocalDateTime.parse("2021-12-20T20:00:00.00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(MOSCOW_ZONE).toInstant(),
                    LocalDateTime.parse("2021-12-20T20:00:00.00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(MOSCOW_ZONE).toInstant(),
                    "Подтип партнера",
                    "Юридическое имя")
            ),
        ),
        partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(
            302,
            "Какая-то ПВЗ из YT 1",
            "ООО Какая-то ПВЗ из LMS 1",
            "Legal адрес ПВЗ из LMS 1"
        ),
    )

    private fun createEmptyLegalNameInfoResponse(
        orderId: Long,
        partnerId: Long
    ) = LegalInfoResponse(
        orderId, partnerId, "", null, null, null, null, null,
        Address.newBuilder().addressString("Legal адрес ПВЗ из LMS 1").build(),
        Address.newBuilder().addressString("Почтовый адрес ПВЗ из LMS 1").build(),
        null, null, null, null
    )

    private fun createPayloadNotReturned(string: String?): CreateStartrekIssueForClaimPayload = CreateStartrekIssueForClaimPayload(
        queue = "MQMTESTCLAIM",
        summary = string ?: "Партнеру 302 будет выставлена претензия за просрочки",
        description = "Заказы в выставленной претензии: \n" +
            "1\n" +
            " Общая сумма претензии: 101.01\n" +
            "Тип партнера:Подтип  сумма:101.01",
        fields = mapOf(
            "amountClaimed" to "101.01 RUB",
        ),
        csvAttachments = setOf(BaseCreateStartrekIssuePayload.CsvAttachment("2021-08-17",  listOf(
            mapOf(
                "orderId" to "1001",
                "shipmentDate" to "2021-12-20T17:00:00Z",
                "cost" to BigDecimal( "101.01"),
                "previousStatus" to "COMPENSATE_SD",
                "deliveryService" to "Какая-то ПВЗ из YT 1",
                "address" to "Адрес ПВЗ из YT 1",
                "partner_id" to 302L,
                "partner_subtype_name" to  "Подтип партнера",
                "partner_name" to "Какая-то ПВЗ из YT 1",
                "legal_partner_name" to "Юридическое имя"
            )
        ))),
        entities = setOf(BaseCreateStartrekIssuePayload.Entity("1", IssueLinkEntityType.CLAIM)),
        partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(
            302,
            "Какая-то ПВЗ из YT 1",
            "ООО Какая-то ПВЗ из LMS 1",
            "Legal адрес ПВЗ из LMS 1"
        ),
        tags = setOf("NOT_RETURNED_CLAIM")
    )

    private fun createPayloadNotReturnedFby(string: String?): CreateStartrekIssueForClaimPayload = CreateStartrekIssueForClaimPayload(
        queue = "MQMTESTCLAIM",
        summary = string ?: "Партнеру ООО МАРКЕТ ОПЕРАЦИИ будет выставлена претензия за просрочки",
        description = "Заказы в выставленной претензии: \n" +
            "1\n" +
            " Общая сумма претензии: 1231.01",
        fields = mapOf(
            "amountClaimed" to "1231.01 RUB",
        ),
        entities = setOf(BaseCreateStartrekIssuePayload.Entity("1", IssueLinkEntityType.CLAIM)),
        partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(
            0,
            "ООО МАРКЕТ ОПЕРАЦИИ",
            "ООО МАРКЕТ ОПЕРАЦИИ",
            "ООО МАРКЕТ ОПЕРАЦИИ"
        ),
        tags = setOf("NOT_RETURNED_FBY_CLAIM")
    )
    companion object {
        private const val CLAIM_ID = "test-id"
        private val CLAIM_DATA_STUB = ByteArray(10)
    }
}
