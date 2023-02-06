package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
import ru.yandex.market.logistics.mqm.converter.yt.YtTreeMapNodeConverter
import ru.yandex.market.logistics.mqm.entity.PlanFactAnalytics
import ru.yandex.market.logistics.mqm.repository.PlanFactAnalyticsRepository
import ru.yandex.market.logistics.mqm.service.yt.dto.YtPlanFactAnalytics
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import java.time.Instant
import java.time.ZoneId
import java.util.Optional

@DisplayName("Проверка работы архиватора аналитических план-фактов без ссылки на существующий план-факт")
class PlanFactAnalyticsYtArchiveExecutorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var planFactAnalyticsYtArchiveExecutor: PlanFactAnalyticsYtArchiveExecutor

    @Autowired
    private lateinit var planFactAnalyticsRepository: PlanFactAnalyticsRepository

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
    lateinit var argumentCaptor: ArgumentCaptor<Iterator<YTreeMapNode>>

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-05-01T22:00:00Z"), ZoneId.systemDefault())
        whenever(yt.tables()).thenReturn(ytTables)
        whenever(yt.operations()).thenReturn(ytOperations)
        whenever(yt.transactions()).thenReturn(ytTransactions)
        whenever(
            ytTransactions.startAndGet(
                Mockito.any<Optional<GUID>>(),
                Mockito.anyBoolean(),
                Mockito.any()
            )
        ).thenReturn(transaction)
        whenever(transaction.id).thenReturn(TRANSACTION_ID)
    }

    @Test
    @DisplayName("Проверка архивации планфактов")
    @DatabaseSetup("/service/plan_fact_analytics_yt_archivator/plan-facts.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_yt_archivator/plan-facts-after-clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testFilter() {
        val archivedPlanFacts = planFactAnalyticsRepository.findAll()
            .filterNot { planFact -> planFact.id == 1L || planFact.id == 3L } // не должны попасть под архивацию
            .map { planFact -> convertNode(planFact) }

        planFactAnalyticsYtArchiveExecutor.run()

        verifyWriteRows(archivedPlanFacts)
        verifyMergeChunks()
    }

    private fun verifyWriteRows(nodes: List<YTreeMapNode>) {
        Mockito.verify(ytTables).write(
            ArgumentMatchers.eq(Optional.of(TRANSACTION_ID)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(YPath.simple(EXPECTED_TABLE_PATH).append(true)),
            ArgumentMatchers.eq(YTableEntryTypes.YSON),
            argumentCaptor.capture()
        )

        val values = mutableListOf<YTreeMapNode>()
        argumentCaptor.value.forEach { values.add(it) }

        assertSoftly { values shouldContainExactlyInAnyOrder nodes }
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
        planFactAnalytics: PlanFactAnalytics,
    ) = YtTreeMapNodeConverter
        .create(YtPlanFactAnalytics::class.java)
        .convert(
            YtPlanFactAnalytics(
                id = planFactAnalytics.id,
                barcode = planFactAnalytics.barcode,
                waybillSegmentId = planFactAnalytics.waybillSegmentId,
                planFactId = planFactAnalytics.planFactId,
                partnerId = planFactAnalytics.partnerId,
                expectedStatus = planFactAnalytics.expectedStatus,
                expectedCheckpoint = planFactAnalytics.expectedCheckpoint,
                initialPlanDatetime = planFactAnalytics.initialPlanDatetime,
                planDatetimeFirst = planFactAnalytics.planDatetimeFirst,
                planDatetimeLast = planFactAnalytics.planDatetimeLast,
                factDatetime = planFactAnalytics.factDatetime,
                issueTicket = planFactAnalytics.issueTicket,
                created = planFactAnalytics.created,
                updated = planFactAnalytics.updated
            )
        )

    companion object {
        private val TRANSACTION_ID = GUID.valueOf("1a363088-52d1-432f-ae72")
        private const val EXPECTED_TABLE_PATH = "//home/market/testing/delivery/mqm/archive/plan_fact_analytics_archive"
    }

}
