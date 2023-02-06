package ru.yandex.market.logistics.mqm.tms

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.Cf
import ru.yandex.bolts.collection.impl.DefaultListF
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.operations.YtOperations
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.tables.YtTables
import ru.yandex.inside.yt.kosher.transactions.Transaction
import ru.yandex.inside.yt.kosher.transactions.YtTransactions
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.mqm.dto.ClaimOrderCsvRecord
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.xlsx.reader.ClaimOrdersFileReader
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueRef
import ru.yandex.startrek.client.model.SearchRequest
import ru.yandex.startrek.client.model.StatusRef
import java.time.Instant
import java.util.Optional


@DisplayName("Проверка работы архиватора статусов претензий")
class StartrekClaimYtArchiverTest : StartrekProcessorTest()  {
    private val EXPECTED_TABLE_PATH = "//home/market/testing/delivery/mqm/archive/claim_archive"

    @Autowired
    private lateinit var startrekClaimYtArchiveExecutor: StartrekClaimYtArchiveExecutor

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var yt: Yt

    @Autowired
    private lateinit var claimOrdersParser: ClaimOrdersFileReader

    @Mock
    private lateinit var ytTables: YtTables

    @Mock
    private lateinit var ytOperations: YtOperations

    @Mock
    private lateinit var ytTransactions: YtTransactions

    @Mock
    private lateinit var transaction: Transaction

    @Captor
    private val captor: ArgumentCaptor<Iterator<YTreeMapNode>>? = null

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-05-01T22:00:00Z"), MOSCOW_ZONE)
        whenever(yt.tables()).thenReturn(ytTables)
        whenever(yt.operations()).thenReturn(ytOperations)
        whenever(yt.transactions()).thenReturn(ytTransactions)
        whenever(ytTransactions.startAndGet(Mockito.any<Optional<GUID>>(), Mockito.anyBoolean(), Mockito.any())).thenReturn(transaction)
        whenever(transaction.id).thenReturn(transactionId)
    }

    @Test
    @DisplayName("Проверка архивации статусов претензий в статусе COMPENSATE SD")
    @DatabaseSetup("/tms/claim/stYtExecutor/claim.xml")
    fun testCompensateSD() {
        val statusRef = Mockito.mock(StatusRef::class.java)
        whenever(statusRef.key).thenReturn("paid")
        whenever(issues.find(ArgumentMatchers.any<SearchRequest>()))
            .thenReturn(
                Cf.wrap(listOf(Issue(null, null, "MQMCLAIM-123", null, 1, Cf.wrap(mapOf("status" to statusRef )), startrekSession))).iterator()
            )

        startrekClaimYtArchiveExecutor.run()
        verifyWriteRows("PAID")
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Проверка архивации статусов претензий в статусе COMPENSATE SD")

    @DatabaseSetup("/tms/claim/stYtExecutor/claim.xml")
    fun testCompensateSDIn32DayClaims() {
        val statusRef = Mockito.mock(StatusRef::class.java)
        whenever(statusRef.key).thenReturn("paid")
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(attachment.mimetype).thenReturn("text/csv")
        whenever(claimOrdersParser.read(any())).thenReturn(listOf(ClaimOrderCsvRecord("1", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9","11")))
        whenever(attachments.getAll(any<String>())).thenReturn(DefaultListF(listOf(attachment)).iterator())
        whenever(attachments.getAll(any<IssueRef>())).thenReturn(DefaultListF(listOf(attachment)).iterator())
        val mock = Mockito.mock(Issue::class.java)
        whenever(mock.key).thenReturn("MQMCLAIM-123")
        whenever(mock.tags).thenReturn(DefaultListF(listOf("CLAIM")))
        whenever(mock.status).thenReturn(statusRef)
        whenever(mock.attachments).thenReturn(DefaultListF(listOf(attachment)).iterator())
        whenever(issues.find(ArgumentMatchers.any<SearchRequest>()))
            .thenReturn(
                Cf.wrap(listOf(mock).iterator()
            ))

        startrekClaimYtArchiveExecutor.run()
        verifyWriteRows("MARKET_FAULT")
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Проверка архивации претензий c неизвестным статусом")

    @DatabaseSetup("/tms/claim/stYtExecutor/claim.xml")
    fun testUnknownStatus() {
        val statusRef = Mockito.mock(StatusRef::class.java)
        whenever(statusRef.key).thenReturn("random")
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(attachment.mimetype).thenReturn("text/csv")
        whenever(claimOrdersParser.read(any())).thenReturn(listOf(ClaimOrderCsvRecord("1",  "3", "4", "COMPENSATE_SD", "6", "address", "7", "8", "9","11")))
        whenever(attachments.getAll(any<String>())).thenReturn(DefaultListF(listOf(attachment)).iterator())
        whenever(attachments.getAll(any<IssueRef>())).thenReturn(DefaultListF(listOf(attachment)).iterator())
        whenever(issues.find(ArgumentMatchers.any<SearchRequest>()))
            .thenReturn(
                Cf.wrap(listOf(Issue(null, null, "MQMCLAIM-123", null, 1, Cf.wrap(mapOf("status" to statusRef )), startrekSession))).iterator()
            )

        startrekClaimYtArchiveExecutor.run()
        verifyWriteRows("UNKNOWN")
        verifyMergeChunks()
    }
    @Test
    @DisplayName("Проверка архивации статусов, если не нашлось подходящих тикетов")

    @DatabaseSetup("/tms/claim/stYtExecutor/claim.xml")
    fun testEmpty() {
        val statusRef = Mockito.mock(StatusRef::class.java)
        whenever(statusRef.key).thenReturn("asdsad")
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(attachment.mimetype).thenReturn("text/csv")
        whenever(claimOrdersParser.read(any())).thenReturn(listOf(ClaimOrderCsvRecord("1",  "3", "4", "COMPENSATE_SD", "6", "address", "7","8", "9","11")))
        whenever(attachments.getAll(any<String>())).thenReturn(DefaultListF(listOf(attachment)).iterator())
        whenever(attachments.getAll(any<IssueRef>())).thenReturn(DefaultListF(listOf(attachment)).iterator())
        whenever(issues.find(ArgumentMatchers.any<SearchRequest>()))
            .thenReturn(
                Cf.wrap(emptyList<Issue>()).iterator()
            )

        startrekClaimYtArchiveExecutor.run()
        Mockito.verifyNoInteractions(ytTables)
    }

    private fun verifyWriteRows(status: String) {
        Mockito.verify(ytTables).write(
            ArgumentMatchers.eq(Optional.of(transactionId)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(YPath.simple(EXPECTED_TABLE_PATH).append(true)),
            ArgumentMatchers.eq(YTableEntryTypes.YSON),
            captor!!.capture()
        )

        val yTreeMapNode = listOf(captor.value)[0].next()

        val ytStatus = yTreeMapNode["status"].get().stringValue()

        Assertions.assertEquals(status, ytStatus)
    }

    private fun verifyMergeChunks() {
        val path = YPath.simple(EXPECTED_TABLE_PATH)
        Mockito.verify(ytOperations).merge(
            IntegrationTestUtils.safeRefEq(
                MergeSpec.builder()
                    .addInputTable(path)
                    .setOutputTable(path)
                    .setCombineChunks(true)
                    .build()
            )
        )
    }

    companion object {
        val expectedStatusDatetime = Instant.parse("2019-03-30T19:00:00Z")
        val updated = expectedStatusDatetime
        val created = Instant.parse("2019-03-30T23:00:00Z")
        val transactionId = GUID.valueOf("1a363088-52d1-432f-ae72")
    }
}
