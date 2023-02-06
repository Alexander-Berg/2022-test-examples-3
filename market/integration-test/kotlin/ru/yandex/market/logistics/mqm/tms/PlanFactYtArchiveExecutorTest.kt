package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Optional
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
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
import ru.yandex.market.logistics.mqm.configuration.properties.PlanFactYtArchiveDuration
import ru.yandex.market.logistics.mqm.configuration.properties.PlanFactYtArchiveProperties
import ru.yandex.market.logistics.mqm.converter.yt.YtPlanFactConverter
import ru.yandex.market.logistics.mqm.converter.yt.YtTreeMapNodeConverter
import ru.yandex.market.logistics.mqm.converter.yt.YtTreeMapNodeConverter.Companion.create
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtPlanFact
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq

@DisplayName("Проверка работы архиватора устаревших План-фактов")
class PlanFactYtArchiveExecutorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var planFactYtArchiveExecutor: PlanFactYtArchiveExecutor

    @Autowired
    private lateinit var planFactService: PlanFactService

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
    private lateinit var argumentCaptor: ArgumentCaptor<Iterator<YTreeMapNode>>

    @Autowired
    private lateinit var ytPlanFactConverter: YtPlanFactConverter

    private val converter: YtTreeMapNodeConverter<YtPlanFact> = create(YtPlanFact::class.java)

    @Autowired
    private lateinit var planFactYtArchiveProperties: PlanFactYtArchiveProperties

    private lateinit var planFactYtArchivePropertiesDurations: Set<PlanFactYtArchiveDuration>

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-05-01T22:00:00Z"), ZoneId.systemDefault())
        whenever(yt.tables()).thenReturn(ytTables)
        whenever(yt.operations()).thenReturn(ytOperations)
        whenever(yt.transactions()).thenReturn(ytTransactions)
        whenever(ytTransactions.startAndGet(any<Optional<GUID>>(), anyBoolean(), any())).thenReturn(transaction)
        whenever(transaction.id).thenReturn(transactionId)
        planFactYtArchivePropertiesDurations = planFactYtArchiveProperties.durations.toSet()
    }

    @AfterEach
    fun clean() {
        planFactYtArchiveProperties.durations = planFactYtArchivePropertiesDurations
    }

    @Test
    @DisplayName("Проверка архивации план-фактов")
    @DatabaseSetup("/service/plan_fact_yt_archivator/success/plan-facts.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_yt_archivator/success/plan-facts-after-clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successArchive() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.IN_TIME,
                threshold = Duration.ofDays(30),
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.EXPIRED,
                threshold = Duration.ofDays(30),
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.OUTDATED,
                threshold = Duration.ofDays(30),
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.NOT_ACTUAL,
                threshold = Duration.ofDays(30),
            ),
        )

        val archivedNodes = findPlanFactsAndTransform(listOf(2L, 3L, 4L, 5L))

        planFactYtArchiveExecutor.run()

        verifyWriteRows(archivedNodes)
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Не архивировать стрельбовые заказы")
    @DatabaseSetup("/service/plan_fact_yt_archivator/do_not_archive_shooting_orders/plan-facts.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_yt_archivator/do_not_archive_shooting_orders/plan-facts-after-clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotArchiveShootingOrders() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.IN_TIME,
                threshold = Duration.ofDays(30),
            ),
        )

        planFactYtArchiveExecutor.run()
        verify(ytTables, never()).write(any(YPath::class.java), any(), any(Iterable::class.java))
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Архивация, когда есть общая настройка статуса и для статуса и продюсера")
    @DatabaseSetup("/service/plan_fact_yt_archivator/success_common_and_producer/plan-facts.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_yt_archivator/success_common_and_producer/plan-facts-after-clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successArchiveWithCommonAndProducer() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.NOT_ACTUAL,
                threshold = Duration.ofDays(30),
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.NOT_ACTUAL,
                threshold = Duration.ofDays(10),
                producerName = "ShootingPlanFactProcessor",
            ),
        )

        val archivedNodes = findPlanFactsAndTransform(
            listOf(
                7L, // По времени
                8L, // По времени и продюсеру
            )
        )

        planFactYtArchiveExecutor.run()

        verifyWriteRows(archivedNodes)
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Архивация, когда есть общая настройка статуса и для статуса и продюсера и в общей настройке длительность меньше")
    @DatabaseSetup("/service/plan_fact_yt_archivator/success_common_and_producer_common_less/plan-facts.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_yt_archivator/success_common_and_producer_common_less/plan-facts-after-clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successArchiveWithCommonAndProducerAndCommonDurationLess() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.NOT_ACTUAL,
                threshold = Duration.ofDays(10),
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.NOT_ACTUAL,
                threshold = Duration.ofDays(20),
                producerName = "CreatedPlanFactProcessor",
            ),
        )

        val archivedNodes = findPlanFactsAndTransform(
            listOf(
                7L,
                9L,
            )
        )

        planFactYtArchiveExecutor.run()

        verifyWriteRows(archivedNodes)
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Архивация, когда есть настройка с различными статусами")
    @DatabaseSetup("/service/plan_fact_yt_archivator/success_with_different_statuses/plan-facts.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_yt_archivator/success_with_different_statuses/plan-facts-after-clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successArchiveWithDifferentStatuses() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.EXPIRED,
                threshold = Duration.ofDays(20),
                producerName = "CreatedPlanFactProcessor",
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.NOT_ACTUAL,
                threshold = Duration.ofDays(10),
                producerName = "ShootingPlanFactProcessor",
            ),
        )

        val archivedNodes = findPlanFactsAndTransform(
            listOf(
                7L,
                9L,
            )
        )

        planFactYtArchiveExecutor.run()

        verifyWriteRows(archivedNodes)
        verifyMergeChunks()
    }

    @Test
    @DisplayName("Ошибка при валидации, когда есть 2-е записи с одинаковыми статусами")
    @DatabaseSetup("/service/plan_fact_yt_archivator/success_with_different_statuses/plan-facts.xml")
    fun errorIfTwoStatuses() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.IN_TIME,
                threshold = Duration.ofDays(30),
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.IN_TIME,
                threshold = Duration.ofDays(50),
            ),
        )

        planFactYtArchiveExecutor.run()

        verify(ytTables, never()).write(
            eq(Optional.of(transactionId)),
            eq(true),
            eq(YPath.simple(EXPECTED_TABLE_PATH).append(true)),
            eq(YTableEntryTypes.YSON),
            any(),
        )
    }

    @Test
    @DisplayName("Ошибка при валидации, когда есть 2-е записи с одинаковыми статусами и продюсером")
    @DatabaseSetup("/service/plan_fact_yt_archivator/success_with_different_statuses/plan-facts.xml")
    fun errorIfTwoStatusesWithProducer() {
        planFactYtArchiveProperties.durations = setOf(
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.IN_TIME,
                threshold = Duration.ofDays(30),
                producerName = "CreatedPlanFactProcessor",
            ),
            PlanFactYtArchiveDuration(
                planFactStatus = PlanFactStatus.IN_TIME,
                threshold = Duration.ofDays(50),
                producerName = "CreatedPlanFactProcessor",
            ),
        )

        planFactYtArchiveExecutor.run()

        verify(ytTables, never()).write(
            eq(Optional.of(transactionId)),
            eq(true),
            eq(YPath.simple(EXPECTED_TABLE_PATH).append(true)),
            eq(YTableEntryTypes.YSON),
            any(),
        )
    }

    private fun verifyWriteRows(nodes: List<YTreeMapNode>) {
        verify(ytTables).write(
            eq(Optional.of(transactionId)),
            eq(true),
            eq(YPath.simple(EXPECTED_TABLE_PATH).append(true)),
            eq(YTableEntryTypes.YSON),
            argumentCaptor.capture(),
        )
        val values = mutableListOf<YTreeMapNode>()
        argumentCaptor.value.forEach { values.add(it) }
        values shouldContainExactlyInAnyOrder nodes
    }

    private fun verifyMergeChunks() {
        val path = YPath.simple(EXPECTED_TABLE_PATH)
        verify(ytOperations).merge(
            safeRefEq(
                MergeSpec.builder()
                    .addInputTable(path)
                    .setOutputTable(path)
                    .setCombineChunks(true)
                    .build()
            )
        )
    }

    private fun findPlanFactsAndTransform(ids: List<Long>) =
        planFactService.findByIdIn(ids)
            .sortedBy { it.id }
            .map { planFact -> ytPlanFactConverter.toYtEntity(planFact) }
            .map { source -> converter.convert(source) }

    companion object {
        val expectedStatusDatetime = Instant.parse("2019-03-30T19:00:00Z")
        val updated = expectedStatusDatetime
        val created = Instant.parse("2019-03-30T23:00:00Z")
        val transactionId = GUID.valueOf("1a363088-52d1-432f-ae72")
        private const val EXPECTED_TABLE_PATH = "//home/market/testing/delivery/mqm/archive/plan_fact_archive"
    }
}
