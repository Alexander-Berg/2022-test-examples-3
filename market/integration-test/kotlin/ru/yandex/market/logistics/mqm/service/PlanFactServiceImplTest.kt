package ru.yandex.market.logistics.mqm.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.additionaldata.ClientReturnFirstCteIntakePlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.additionaldata.PlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.repository.LomWaybillSegmentRepository
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactProcessor
import ru.yandex.market.logistics.mqm.service.processor.planfact.WaybillSegmentPlanFactProcessor
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.BaseSelfSufficientRddPlanFactProcessor

@DisplayName("Проверка сервиса работы с план-фактами")
class PlanFactServiceImplTest : AbstractContextualTest() {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    lateinit var planFactService: PlanFactService

    @Autowired
    lateinit var partnerService: PartnerService

    @Autowired
    lateinit var lomClient: LomClient

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    lateinit var waybillSegmentRepository: LomWaybillSegmentRepository

    @Autowired
    lateinit var selfSufficientPlanFactProcessors: List<BaseSelfSufficientPlanFactProcessor>

    @Autowired
    lateinit var rddPlanFactProcessors: List<BaseSelfSufficientRddPlanFactProcessor>

    @AfterEach
    fun tearDown() {
        verifyZeroInteractions(lomClient)
    }

    // Обработка получения IN и закрытие предыдущих план-фактов.

    @Test
    @DisplayName(
        "Проверка сохранения фактической даты для ПФ сегмента (статус IN) и закрытие план-фактов на предыдущих сегментах"
    )
    @DatabaseSetup("/service/plan_fact_service/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactStatusDatetimeIn() {
        val factTime = Instant.parse("2021-05-20T05:00:00Z")
        clock.setFixed(factTime.plus(Duration.ofHours(1)), ZoneId.systemDefault())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            102,
            "IN",
            factTime,
        )
    }

    // Обработка получения TRACK_RECEIVED, если есть предыдущий сегмент.

    @Test
    @DisplayName("Проверка сохранения фактической даты для ПФ сегмента (статус STARTED)")
    @DatabaseSetup("/service/plan_fact_service/before/setup_started.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactStatusDatetimeStarted() {
        val factTime = Instant.parse("2021-05-20T05:00:00Z")
        clock.setFixed(factTime.plusSeconds(3600), ZoneId.systemDefault())
        doReturn(LocalTime.of(23, 59)).whenever(partnerService).findCutoffTime(any())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            102000,
            "TRACK_RECEIVED",
            factTime,
        )
    }

    @Test
    @DisplayName("Проверка сохранения фактической даты для ПФ сегмента (статус STARTED), дропшип")
    @DatabaseSetup("/service/plan_fact_service/before/setup_ds.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact_started_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactStatusDatetimeStartedDs() {
        val factTime = Instant.parse("2021-05-20T05:00:00Z")
        clock.setFixed(factTime.plusSeconds(3600), ZoneId.systemDefault())
        doReturn(LocalTime.of(23, 59)).whenever(partnerService).findCutoffTime(any())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            102000,
            "TRACK_RECEIVED",
            factTime,
        )
    }

    // Обработка получения TRANSIT_PREPARED и закрытие предыдущих план-фактов.

    @Test
    @DisplayName(
        "Проверка сохранения фактической даты для ПФ сегмента с пересчётом ПФ следующего этапа" +
            " (статус OUT)"
    )
    @DatabaseSetup("/service/plan_fact_service/before/setup_recalculate.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact_with_recalculate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactStatusDatetimeWithRecalculateOut() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        val factTime = Instant.parse("2021-05-20T05:00:00Z")
        clock.setFixed(factTime.plusSeconds(3600), ZoneId.systemDefault())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            101,
            "TRANSIT_PREPARED",
            factTime,
        )
    }

    @Test
    @DisplayName(
        "Проверка сохранения фактической даты для ПФ сегмента с пересчётом ПФ следующего этапа" +
            " (статус OUT, статус уже получен)"
    )
    @DatabaseSetup("/service/plan_fact_service/before/setup_with_history.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact_with_recalculate_with_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactStatusDatetimeInWithStatusInHistory() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        val factTime = Instant.parse("2021-05-20T05:00:00Z")
        clock.setFixed(factTime.plusSeconds(3600), ZoneId.systemDefault())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            101,
            "TRANSIT_PREPARED",
            factTime,
        )
    }

    // Обработка получения TRANSIT_COURIER_FOUND (32).

    @Test
    @DisplayName("Проверка сохранения фактической даты для ПФ сегмента для 32-34")
    @DatabaseSetup("/service/plan_fact_service/before/setup_received_fact_32.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact_after_32.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillFactStatusDatetimeFor32() {
        val factTime = Instant.parse("2021-05-20T05:00:00Z")
        clock.setFixed(factTime.plusSeconds(3600), ZoneId.systemDefault())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            101,
            SegmentStatus.TRANSIT_COURIER_FOUND.name,
            factTime,
        )
    }

    // Клиентские возвраты.

    @Test
    @DisplayName("Проверка проставления статуса notActual план-фактам предыдущих сегментов клиентских возвратов")
    @DatabaseSetup("/service/plan_fact_service/before/mark_client_returns_not_actual.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/mark_client_returns_not_actual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun markNotActualPreviousClientReturnSegments() {
        clock.setFixed(Instant.parse("2020-12-01T00:00:00.00Z"), ZoneId.systemDefault());
        val planFact1: PlanFact = generateClientReturnPlanFact(21L, clock.instant())
        val planFact2: PlanFact = generateClientReturnPlanFact(
            31L,
            clock.instant().plus(2, ChronoUnit.HOURS)
        )
        val planFact3: PlanFact = generateClientReturnPlanFact(
            31L,
            clock.instant().plus(10, ChronoUnit.HOURS)
        )
        transactionTemplate.executeWithoutResult {
            planFactService.markPreviousClientReturnsPlanFactsNotActual(listOf(planFact1, planFact2, planFact3))
        }
    }

    // Обработка получения RETURNED.

    @Test
    @DisplayName("Проверка проставления статуса NOT_ACTUAL план-факту предыдущего сегмента для обратной логистики")
    @DatabaseSetup("/service/plan_fact_service/before/mark_return_logistics_not_actual.xml")
    fun markNotActualForReturnLogistics() {
        val factTime = Instant.parse("2021-05-12T11:40:51.00Z")
        clock.setFixed(factTime, ZoneId.systemDefault())
        transactionTemplate.executeWithoutResult {
            planFactService.saveFactStatusDatetime(
                EntityType.LOM_WAYBILL_SEGMENT,
                101,
                SegmentStatus.RETURNED.name,
                factTime
            )
            val planFact = planFactService.getByIdOrThrow(1)
            assertSoftly {
                planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            }
        }
    }

    // Обработка TRANSIT_COURIER_RECEIVED.

    @Test
    @DisplayName("Проверка проставления статуса NOT_ACTUAL предыдущему план-факту")
    @DatabaseSetup("/service/plan_fact_service/before/mark_not_actual_or_expired.xml")
    fun markPreviousPlanFactNotActual() {
        val factTime = Instant.parse("2021-05-12T11:40:51.00Z")
        clock.setFixed(factTime, ZoneId.systemDefault())
        transactionTemplate.executeWithoutResult {
            planFactService.saveFactStatusDatetime(
                EntityType.LOM_WAYBILL_SEGMENT,
                101,
                SegmentStatus.TRANSIT_COURIER_RECEIVED.name,
                factTime
            )
            val planFact = planFactService.getByIdOrThrow(1)
            assertSoftly {
                planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            }
        }
    }

    @Test
    @DisplayName("Проверка проставления статуса EXPIRED предыдущему план-факту")
    @DatabaseSetup("/service/plan_fact_service/before/mark_not_actual_or_expired.xml")
    fun markPreviousPlanFactExpired() {
        val factTime = Instant.parse("2021-05-12T10:30:51.00Z")
        clock.setFixed(factTime, ZoneId.systemDefault())
        transactionTemplate.executeWithoutResult {
            planFactService.saveFactStatusDatetime(
                EntityType.LOM_WAYBILL_SEGMENT,
                101,
                SegmentStatus.TRANSIT_COURIER_RECEIVED.name,
                factTime
            )
            val planFact = planFactService.getByIdOrThrow(1)
            assertSoftly {
                planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            }
        }
    }

    // LRM.

    @Test
    @DisplayName("Проверка проставления статуса EXPIRED план-фактам PVZ на IN в статусе CREATED через 14 дней")
    @DatabaseSetup("/service/plan_fact_service/before/mark_lrm_returns_created_planfact_as_expired.xml")
    fun markPvzCreatedToInPlanFactExpiredExpiredAfterCurtainTimeout() {
        val expireBefore = Instant.parse("2021-05-12T10:30:51.00Z")
        transactionTemplate.executeWithoutResult {
            planFactService.markExpiredForLrmReturnSegmentPlanFactsCreatedBefore(
                "IN",
                expireBefore
            )
            assertSoftly {
                planFactService.getByIdOrThrow(1).planFactStatus shouldBe PlanFactStatus.EXPIRED
                planFactService.getByIdOrThrow(2).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(3).planFactStatus shouldBe PlanFactStatus.EXPIRED
                planFactService.getByIdOrThrow(4).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(5).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(6).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(7).planFactStatus shouldBe PlanFactStatus.IN_TIME
                planFactService.getByIdOrThrow(8).planFactStatus shouldBe PlanFactStatus.CREATED
            }
        }
    }

    @Test
    @DisplayName("Проверка проставления статуса NOT_ACTUAL старым план-фактам по LOM заказам")
    @DatabaseSetup("/service/plan_fact_service/before/mark_not_actual_old_lom_planfacts.xml")
    fun markNotActualOldLomPlanFacts() {
        transactionTemplate.executeWithoutResult {
            planFactService.markNotActualOldReturnPlanFacts("make_them_not_actual")
            assertSoftly {
                planFactService.getByIdOrThrow(1).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(2).planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
                planFactService.getByIdOrThrow(3).planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
                planFactService.getByIdOrThrow(4).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(5).planFactStatus shouldBe PlanFactStatus.CREATED
                planFactService.getByIdOrThrow(6).planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            }
        }
    }

    // Создание план-фактов.

    @Test
    @DisplayName("Проверка сохранение план-факта")
    fun createPlanFact() {
        val testDeadline = Instant.parse("2021-03-04T09:00:00.00Z")
        assertSoftly {
            planFactService.createPlanFactWithProcessor(
                WaybillSegment(
                    id = 1,
                    segmentType = SegmentType.COURIER,
                ),
                TestPlanFactProcessor(testDeadline)
            ) shouldBe PlanFact(
                waybillSegmentType = SegmentType.COURIER,
                expectedStatus = SegmentStatus.IN.name,
                producerName = TestPlanFactProcessor::class.simpleName,
                expectedStatusDatetime = testDeadline,
                processingStatus = ProcessingStatus.ENQUEUED,
                planFactStatus = PlanFactStatus.CREATED,
                entityType = EntityType.LOM_WAYBILL_SEGMENT,
                scheduleTime = testDeadline
            ).apply {
                setData(TestPlanFactProcessor.TestPlanFactAdditionalData("testValue"))
                entityId = 1
            }
        }
    }

    @Test
    @DisplayName("Проверка, загрузки план-фактов, готовых для группировки")
    @DatabaseSetup("/service/plan_fact_service/before/find_ready_to_aggregation.xml")
    fun findReadyToAggregationPlanFacts() {
        val planFacts = transactionTemplate.execute {
            planFactService.findReadyToAggregationPlanFacts(0, 100)
        }!!
        assertSoftly {
            planFacts.size shouldBe 2
            val orderPlanFact = planFacts.first { it.entityType == EntityType.LOM_ORDER }.entity as LomOrder
            orderPlanFact.barcode shouldBe "test_barcode"
            val wsPlanFact =
                planFacts.first { it.entityType == EntityType.LOM_WAYBILL_SEGMENT }.entity as WaybillSegment
            wsPlanFact.externalId shouldBe "ext101"
        }
    }

    @Test
    @DisplayName(
        "Проверка, что новый план-факт не сохраняется, если есть план-факт на тот же статус, но от другого процессора"
    )
    @DatabaseSetup("/service/plan_fact_service/before/save_plan_fact_with_other_producer.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/before/save_plan_fact_with_other_producer.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveNewAndExpireOldPlanFactsIgnorePlanFactFromAnotherProducerTest() {
        transactionTemplate.executeWithoutResult {
            planFactService.saveNewAndExpireOldPlanFacts(
                PlanFact(
                    producerName = "other_producer",
                    entityId = 1,
                    entityType = EntityType.LOM_WAYBILL_SEGMENT,
                    expectedStatus = "IN",
                    expectedStatusDatetime = Instant.parse("2021-10-02T08:00:00Z"),
                    planFactStatus = PlanFactStatus.ACTIVE,
                    processingStatus = ProcessingStatus.ENQUEUED,
                )
            )
        }
    }

    @Test
    @DisplayName("Проверка, что все selfSufficient план-факты добавлены в неподдерживаемые planFactService")
    fun addAllSelfSufficientIntoUnsupported() {
        for (processorName in selfSufficientPlanFactProcessors.map { it.producerName() }) {
            (planFactService as PlanFactServiceImpl).isSupportedProcessor(processorName) shouldBe false
        }
    }

    @Test
    @DisplayName("Проверка, что все rddPlanFactProcessors добавлены в planFactService")
    fun addAllRddPlanFactProcessorsIntoPlanFactService() {
        for (processorName in rddPlanFactProcessors.map { it.producerName() }) {
            PlanFactServiceImpl.isRddPlanFactProcessor(processorName) shouldBe true
        }
    }

    // Обработка CANCELLED.

    @Test
    @DisplayName("Проверка закрытие предыдущих план-фактов при CANCELLED")
    @DatabaseSetup("/service/plan_fact_service/before/setup_pickup.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_service/after/save_waybill_segment_fact_pickup_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactStatusDatetimeCancelled() {
        val factTime = Instant.parse("2020-11-01T05:00:00Z")
        clock.setFixed(factTime, ZoneId.systemDefault())
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            1,
            "CANCELLED",
            factTime,
        )
    }

    private fun generateClientReturnPlanFact(returnId: Long, factDateTime: Instant): PlanFact {
        val data = ClientReturnFirstCteIntakePlanFactAdditionalData(
            returnId,
            1,
            "barcode_1",
            LocalDateTime.ofInstant(factDateTime.minus(1, ChronoUnit.DAYS), DateTimeUtils.MOSCOW_ZONE)
        )
        return PlanFact(factStatusDatetime = factDateTime)
            .setData(data)
    }

    private class TestPlanFactProcessor(
        private val expectedDateTime: Instant
    ) : WaybillSegmentPlanFactProcessor {
        override fun calculateExpectedDatetime(waybillSegment: WaybillSegment): Instant = expectedDateTime

        override fun isEligible(waybillSegment: WaybillSegment) = true

        override val expectedStatus: SegmentStatus = SegmentStatus.IN

        override fun getPayload(waybillSegment: WaybillSegment): PlanFactAdditionalData {
            return TestPlanFactAdditionalData("testValue")
        }

        data class TestPlanFactAdditionalData(private val testField: String) : PlanFactAdditionalData()
    }
}
