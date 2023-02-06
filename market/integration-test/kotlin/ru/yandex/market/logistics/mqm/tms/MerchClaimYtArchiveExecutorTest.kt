package ru.yandex.market.logistics.mqm.tms

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.Optional
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
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.converter.yt.YtTreeMapNodeConverter.Companion.create
import ru.yandex.market.logistics.mqm.entity.ClaimUnit
import ru.yandex.market.logistics.mqm.entity.enums.ClaimStatus
import ru.yandex.market.logistics.mqm.repository.ClaimUnitRepository
import ru.yandex.market.logistics.mqm.service.ClaimService
import ru.yandex.market.logistics.mqm.service.enums.ClaimCompensationType
import ru.yandex.market.logistics.mqm.service.yt.dto.YtMerchClaim
import ru.yandex.market.logistics.mqm.tms.claim.MerchClaimYtArchiveExecutor
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

@DisplayName("Проверка работы архиватора претензий")
class MerchClaimYtArchiveExecutorTest: AbstractContextualTest() {

    private val EXPECTED_TABLE_PATH = "//home/market/testing/fulfillment/mqm-compensation/merch-claims"

    @Autowired
    private lateinit var merchClaimYtArchiveExecutor: MerchClaimYtArchiveExecutor

    @Autowired
    private lateinit var claimUnitRepository: ClaimUnitRepository

    @Autowired
    private lateinit var claimService: ClaimService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var yt: Yt

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
        clock.setFixed(Instant.parse("2021-05-01T22:00:00Z"), ZoneId.systemDefault())
        whenever(yt.tables()).thenReturn(ytTables)
        whenever(yt.operations()).thenReturn(ytOperations)
        whenever(yt.transactions()).thenReturn(ytTransactions)
        whenever(ytTransactions.startAndGet(Mockito.any<Optional<GUID>>(), Mockito.anyBoolean(), Mockito.any())).thenReturn(transaction)
        whenever(transaction.id).thenReturn(transactionId)
    }

    @Test
    @DatabaseSetup("/service/claim_archivator/before.xml")
    fun testFilter() {
        val archiveClaims = claimUnitRepository.findAll()
            .filter { unit -> unit.claim?.id == 3L }
            .map { unit -> convertNode(unit) }

        merchClaimYtArchiveExecutor.run()

        val expected = claimService.findById(3)

        verifyWriteRows(archiveClaims)
        verifyMergeChunks()
        Assertions.assertEquals(expected?.status, ClaimStatus.SEND_FOR_PAYMENT)
    }

    private fun verifyWriteRows(nodes: List<YTreeMapNode>) {
        Mockito.verify(ytTables).write(
            ArgumentMatchers.eq(Optional.of(StartrekClaimYtArchiverTest.transactionId)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(YPath.simple(EXPECTED_TABLE_PATH).append(true)),
            ArgumentMatchers.eq(YTableEntryTypes.YSON),
            captor!!.capture()
        )

        val actual = listOf(captor.value)[0].next()
        Assertions.assertEquals(nodes.first(), actual)
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

    private fun convertNode(
        claimUnit: ClaimUnit,
    ) = create(YtMerchClaim::class.java)
        .convert(
            YtMerchClaim(
                id = claimUnit.id,
                orderId = claimUnit.additionalInfo.first().orderId,
                partnerId = claimUnit.additionalInfo.first().partnerId,
                itemId = claimUnit.additionalInfo.first().checkouterItemId,
                amount = claimUnit.additionalInfo.first().assessedCostRub?.times(BigDecimal.valueOf(100))?.toLong(),
                type = ClaimCompensationType.LOST.name,
                finishedAt = claimUnit.claim?.updatedAt.toString(),
                claimId = claimUnit.claim?.id,
                claimDate = claimUnit.claim?.createdAt.toString(),
                issueLink = claimUnit.issueTicket,
                okUuid = claimUnit.claim?.ok_uuid.toString(),
                count = 1,
                unitCost = claimUnit.additionalInfo.first().assessedCostRub?.times(BigDecimal.valueOf(100))?.toLong(),

            )
        )

    companion object {
        val expectedStatusDatetime = Instant.parse("2019-03-30T19:00:00Z")
        val updated = expectedStatusDatetime
        val created = Instant.parse("2019-03-30T23:00:00Z")
        val transactionId = GUID.valueOf("1a363088-52d1-432f-ae72")
    }
}
