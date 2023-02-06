package ru.yandex.market.logistics.mqm.service.handler


import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.LockTimeoutException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.SchedulingProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.BasePlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.entity.processing.SchedulingEntityType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.QualityRuleService
import ru.yandex.market.logistics.mqm.service.SchedulingLockService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.QualityRuleSingleProcessor
import ru.yandex.market.logistics.mqm.utils.TestableTransactionManager

@ExtendWith(MockitoExtension::class)
internal class PlanFactHandlerImplTest: AbstractTest() {

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var qualityRuleService: QualityRuleService

    @Mock
    private lateinit var qualityRuleProcessor: QualityRuleSingleProcessor

    @Mock
    private lateinit var schedulingLockService: SchedulingLockService

    private val transactionManager = TestableTransactionManager()

    private val clock = TestableClock()

    @BeforeEach
    private fun setUp() {
        clock.setFixed(DEFAULT_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Успешная обработка, обновление schedulingTime")
    fun successHandling() {
        val planFact = preparePlanFact()
        val handler = prepareTestData()
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.scheduleTime shouldBe NEW_SCHEDULE_TIME
            planFact.planFactStatus shouldBe PlanFactStatus.ACTIVE
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Проставление inTime")
    fun setInTime() {
        val planFact = preparePlanFact(
            planFactStatus = PlanFactStatus.CREATED,
            factTime = DEFAULT_TIME.minus(Duration.ofHours(2)),
        )
        val handler = prepareTestData(
            mockProcessors = false,
        )
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.scheduleTime shouldBe null
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor, never()).canProcess(any())
            verify(qualityRuleProcessor, never()).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Проставление active")
    fun setActive() {
        val planFact = preparePlanFact(
            planFactStatus = PlanFactStatus.CREATED,
        )
        val handler = prepareTestData()
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.scheduleTime shouldBe NEW_SCHEDULE_TIME
            planFact.planFactStatus shouldBe PlanFactStatus.ACTIVE
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Оставить в CREATED, если план еще не прошел")
    fun keepCreated() {
        val planFact = preparePlanFact(
            planFactStatus = PlanFactStatus.CREATED,
            expectedTime = DEFAULT_TIME.plusSeconds(1)
        )
        val handler = prepareTestData()
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.scheduleTime shouldBe DEFAULT_TIME.plusSeconds(1)
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Если не удалось захватить лок на один из п-ф то обрабатывать следующие")
    fun skipIfCanNotAcquireLock() {
        val planFact = preparePlanFact()
        val handler = prepareTestData()
        whenever(planFactService.getPlanFactWithLock(any()))
            .thenThrow(LockTimeoutException("Test"))
            .thenReturn(planFact)
        handler.handle(listOf(1, 2), DEFAULT_TIME)
        assertSoftly {
            verify(planFactService, times(2)).getPlanFactWithLock(any())
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1, 2), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 1
        }
    }

    @Test
    @DisplayName("Ловить ошибки обработки и проставлять failed")
    fun catchException() {
        val planFact = preparePlanFact()
        val handler = prepareTestData(throwExceptionOnProcessing = true)
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.processingStatus shouldBe ProcessingStatus.FAILED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Не обрабатывать план-факты у которых scheduledTime позднее текущего времени")
    fun doNotHandleIfScheduledTimeIsNull() {
        val planFact = preparePlanFact(
            scheduleTime = DEFAULT_TIME.plus(1, ChronoUnit.MINUTES)
        )
        val handler = prepareTestData(
            mockProcessors = false,
            mockLoadRules = false,
        )
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService, never()).findActiveSingleEventRules()
            verify(qualityRuleProcessor, never()).canProcess(any())
            verify(qualityRuleProcessor, never()).process(any(), any())
            verify(planFactService, never()).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Процессор применяется, игнорируя предыдущий статус обработки")
    fun successHandlingAfterItWasCompleted() {
        val testRuleId = 1L
        val planFact = preparePlanFact().apply {
            setData(BasePlanFactAdditionalData(processingStatuses = mutableMapOf(testRuleId to true)))
        }
        val handler = prepareTestData(
            rule = prepareRule(id = testRuleId),
            ignoreProcessingStatuses = true,
        )
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.scheduleTime shouldBe NEW_SCHEDULE_TIME
            planFact.planFactStatus shouldBe PlanFactStatus.ACTIVE
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    @Test
    @DisplayName("Процессор не применяется, если статус обработки не игнорируется")
    fun notHandlingAfterItWasCompletedLegacy() {
        val testRuleId = 1L
        val planFact = preparePlanFact().apply {
            setData(BasePlanFactAdditionalData(processingStatuses = mutableMapOf(testRuleId to true)))
        }
        val handler = prepareTestData(
            rule = prepareRule(id = testRuleId),
            mockProcessors = false,
        )
        handler.handle(listOf(1L), DEFAULT_TIME)
        assertSoftly {
            planFact.scheduleTime shouldBe null
            planFact.planFactStatus shouldBe PlanFactStatus.ACTIVE
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            verify(planFactService).getPlanFactWithLock(1)
            verify(qualityRuleService).findActiveSingleEventRules()
            verifyNoMoreInteractions(qualityRuleProcessor)
            verify(planFactService).save(any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.PLAN_FACT)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 0
        }
    }

    private fun preparePlanFact(
        planFactStatus: PlanFactStatus = PlanFactStatus.ACTIVE,
        factTime: Instant? = null,
        scheduleTime: Instant = DEFAULT_TIME.minus(1, ChronoUnit.HOURS),
        expectedTime: Instant = scheduleTime,
    ) = PlanFact(
        id = 1,
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatusDatetime = expectedTime,
        expectedStatus = "OUT",
        planFactStatus = planFactStatus,
        scheduleTime = scheduleTime,
        factStatusDatetime = factTime,
    ).apply {
        created = DEFAULT_TIME.minus(1, ChronoUnit.DAYS)
        whenever(planFactService.getPlanFactWithLock(any())).thenReturn(this)
    }

    private fun prepareRule(id: Long = 101) = QualityRule(
        id = id,
        enabled = true,
        expectedStatus = "OUT",
        aggregationType = AggregationType.NONE,
        ruleProcessor = QualityRuleProcessorType.STARTREK,
    )

    private fun prepareTestData(
        rule: QualityRule = prepareRule(),
        mockProcessors: Boolean = true,
        mockLoadRules: Boolean = true,
        throwExceptionOnProcessing: Boolean = false,
        ignoreProcessingStatuses: Boolean = false,
    ): PlanFactHandlerImpl {

        if (mockLoadRules) {
            whenever(qualityRuleService.findActiveSingleEventRules()).thenReturn(listOf(rule))
        }
        if (mockProcessors) {
            whenever(qualityRuleProcessor.canProcess(any())).thenReturn(true)
            if (!throwExceptionOnProcessing) {
                whenever(qualityRuleProcessor.process(any(), any())).thenReturn(NEW_SCHEDULE_TIME)
            } else {
                whenever(qualityRuleProcessor.process(any(), any())).thenThrow(RuntimeException("Test"))
            }
        }
        return PlanFactHandlerImpl(
            planFactService = planFactService,
            qualityRuleService = qualityRuleService,
            qualityRuleProcessors = listOf(qualityRuleProcessor),
            ignoreProcessingStatuses = ignoreProcessingStatuses,
            clock = clock,
            platformTransactionManager = transactionManager,
            schedulingLockService = schedulingLockService,
            properties = SchedulingProperties(useLock = true),
        )
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private val NEW_SCHEDULE_TIME = DEFAULT_TIME.plus(5, ChronoUnit.MINUTES)
    }
}
