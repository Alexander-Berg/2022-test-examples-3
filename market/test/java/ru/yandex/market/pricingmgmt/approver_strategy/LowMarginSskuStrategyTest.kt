package ru.yandex.market.pricingmgmt.approver_strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.api.JournalApiService
import ru.yandex.market.pricingmgmt.approver_strategy.dto.JournalKey
import ru.yandex.market.pricingmgmt.approver_strategy.dto.LowMarginSskuInfo
import ru.yandex.market.pricingmgmt.approver_strategy.dto.LowMarginSskuMeta
import ru.yandex.market.pricingmgmt.executor.CreateTicketsExecutor
import ru.yandex.market.pricingmgmt.model.postgres.JournalApproveDto
import ru.yandex.market.pricingmgmt.repository.postgres.CatteamRepository
import ru.yandex.market.pricingmgmt.repository.postgres.JournalRepository
import ru.yandex.market.pricingmgmt.repository.postgres.PriceImportJobRepository
import ru.yandex.market.pricingmgmt.repository.postgres.UserRepository
import ru.yandex.market.pricingmgmt.service.*
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.error.StartrekInternalClientError
import java.time.OffsetDateTime

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class LowMarginSskuStrategyTest(
    @Autowired private val journalService: JournalService,
    @Autowired private val ticketDataRepository: TrackerApproverDataRepository,
    @Autowired private val ticketStatusRepository: TrackerApproverTicketRepository,
    @Autowired private val transactionTemplate: TransactionTemplate,
    @Autowired private val journalRepository: JournalRepository,
    @Autowired private val priceService: PriceService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val catteamRepository: CatteamRepository,
    @Autowired private val priceErrorService: PriceErrorService,
    @Autowired private val environmentService: EnvironmentService,
    @Autowired private val userService: UserService,
    @Autowired private val priceValidationService: PriceValidationService,
    @Autowired private val priceImportService: PriceImportService,
    @Autowired private val priceImportJobRepository: PriceImportJobRepository,
    @Autowired private val createTicketsExecutor: CreateTicketsExecutor
) : ControllerTest() {

    @MockBean
    private lateinit var session: Session

    private var strategy: LowMarginSskuStrategy? = null

    private var journalApiService: JournalApiService? = null

    companion object {
        private val TEST_DTO = JournalApproveDto(
            id = 1,
            catteamId = 1,
            approvalComment = "test",
            startAt = OffsetDateTime.now(),
            endAt = OffsetDateTime.now()
        )
    }

    @BeforeEach
    fun setUp() {
        strategy = LowMarginSskuStrategy(
            session,
            "TEST_QUEUE",
            journalService,
        )

        val factory = TrackerApproverFactory(
            ticketDataRepository,
            ticketStatusRepository,
            transactionTemplate,
            objectMapper
        )

        factory.registerStrategy(strategy)

        journalApiService = JournalApiService(
            journalRepository,
            journalService,
            priceService,
            userRepository,
            catteamRepository,
            priceErrorService,
            environmentService,
            userService,
            priceImportJobRepository,
            priceValidationService,
            priceImportService,
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["LowMarginSskuStrategyTest.processJournals.before.csv"],
        after = ["LowMarginSskuStrategyTest.processJournals.after.csv"]
    )
    fun testProcessingOnTicketCreationJournals() {
        TrackerTestUtil.createTicket(session, "TEST-1")
        createTicketsExecutor.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["LowMarginSskuStrategyTest.startApproval.before.csv"],
        after = ["LowMarginSskuStrategyTest.startApproval.after.csv"]
    )
    fun testStartApproval() {
        TrackerTestUtil.createTicket(session, "TEST-1")
        createTicketsExecutor.startApproval(TEST_DTO)
    }

    @Test
    @DbUnitDataSet(
        before = ["LowMarginSskuStrategyTest.startApproval.before.csv"]
    )
    fun testStartApprovalRaisedException() {
        val exception = assertThrows<StartrekInternalClientError> {
            TrackerTestUtil.createTicketRaisedException(session)
            createTicketsExecutor.startApproval(TEST_DTO)
        }

        assertEquals(
            "java.io.IOException: test IO exception",
            exception.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["LowMarginSskuStrategyTest.processTicketSolved.before.csv"],
        after = ["LowMarginSskuStrategyTest.processTicketSolved.after.csv"]
    )
    fun testProcessWhenTicketClosedAndSolved() {
        val ticket = "TEST-1"
        val key = JournalKey(1)
        val meta = LowMarginSskuMeta("test")
        val keyMetaMap = hashMapOf(
            key to LowMarginSskuInfo("comment")
        )
        TrackerTestUtil.createTicket(session, ticket, "closed", "fixed")

        val request = ProcessRequest.of<JournalKey, LowMarginSskuMeta, LowMarginSskuInfo>(
            ticket, listOf(key), meta, keyMetaMap
        )
        strategy!!.process(request)
    }

    @Test
    @DbUnitDataSet(
        before = ["LowMarginSskuStrategyTest.processTicketSolved.before.csv"],
        after = ["LowMarginSskuStrategyTest.processTicketUnsolved.after.csv"]
    )
    fun testProcessWhenTicketClosedAndUnsolved() {
        val ticket = "TEST=1"
        val key = JournalKey(1)
        val meta = LowMarginSskuMeta("test")
        val keyMetaMap = hashMapOf(
            key to LowMarginSskuInfo("comment")
        )
        TrackerTestUtil.createTicket(session, ticket, "closed", "won't fixed")

        val request = ProcessRequest.of<JournalKey, LowMarginSskuMeta, LowMarginSskuInfo>(
            ticket, listOf(key), meta, keyMetaMap
        )
        strategy!!.process(request)
    }
}
