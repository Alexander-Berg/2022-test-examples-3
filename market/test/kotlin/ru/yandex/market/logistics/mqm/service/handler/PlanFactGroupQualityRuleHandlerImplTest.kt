package ru.yandex.market.logistics.mqm.service.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.PlanFactAdditionalDataMap
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.QualityRuleService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.QualityRuleAggregatedProcessor
import ru.yandex.market.logistics.mqm.utils.TestableTransactionManager
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.LockTimeoutException
import ru.yandex.market.logistics.mqm.configuration.properties.SchedulingProperties
import ru.yandex.market.logistics.mqm.entity.processing.SchedulingEntityType
import ru.yandex.market.logistics.mqm.service.SchedulingLockService

@ExtendWith(MockitoExtension::class)
internal class PlanFactGroupQualityRuleHandlerImplTest: AbstractTest() {

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var qualityRuleService: QualityRuleService

    @Mock
    private lateinit var qualityRuleProcessor: QualityRuleAggregatedProcessor

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
        val (planFactGroup, handler) = prepareTestData()
        handler.handle(listOf(1), DEFAULT_TIME)
        assertSoftly {
            planFactGroup.scheduleTime shouldBe NEW_SCHEDULE_TIME
            planFactGroup.processingStatus shouldBe ProcessingStatus.ENQUEUED
            verify(qualityRuleService).findActiveRulesWithAggregation()
            verify(planFactService).getPlanFactGroupWithLock(1)
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.GROUP)
            transactionManager.commitsNumber shouldBe 1
        }
    }

    @Test
    @DisplayName("Проставление Failed при ошибке")
    fun setFailed() {
        val (planFactGroup, handler) = prepareTestData(throwExceptionOnProcessing = true)
        handler.handle(listOf(1), DEFAULT_TIME)
        assertSoftly {
            planFactGroup.scheduleTime shouldBe null
            planFactGroup.processingStatus shouldBe ProcessingStatus.FAILED
            verify(qualityRuleService).findActiveRulesWithAggregation()
            verify(planFactService).getPlanFactGroupWithLock(1)
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.GROUP)
            transactionManager.commitsNumber shouldBe 1
        }
    }


    @Test
    @DisplayName("Проставление Processed если нет нового scheduled_time")
    fun setProcessed() {
        val (planFactGroup, handler) = prepareTestData(newScheduledTime = null)
        handler.handle(listOf(1), DEFAULT_TIME)
        assertSoftly {
            planFactGroup.scheduleTime shouldBe null
            planFactGroup.processingStatus shouldBe ProcessingStatus.PROCESSED
            verify(qualityRuleService).findActiveRulesWithAggregation()
            verify(planFactService).getPlanFactGroupWithLock(1)
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.GROUP)
            transactionManager.commitsNumber shouldBe 1
        }
    }


    @Test
    @DisplayName("Если не удалось захватить лок на одну из групп то обрабатывать следующие")
    fun skipIfCanNotAcquireLock() {
        val (group, handler) = prepareTestData()
        whenever(planFactService.getPlanFactGroupWithLock(any()))
            .thenThrow(LockTimeoutException("Test"))
            .thenReturn(group)
        handler.handle(listOf(1, 2), DEFAULT_TIME)
        assertSoftly {
            verify(planFactService, times(2)).getPlanFactGroupWithLock(any())
            verify(qualityRuleService).findActiveRulesWithAggregation()
            verify(qualityRuleProcessor).canProcess(any())
            verify(qualityRuleProcessor).process(any(), any())
            verify(schedulingLockService).releaseLock(setOf(1, 2), SchedulingEntityType.GROUP)
            transactionManager.commitsNumber shouldBe 1
            transactionManager.rollbacksNumber shouldBe 1
        }
    }

    @Test
    @DisplayName("Не обрабатывать группы у которых scheduledTime позднее текущего времени")
    fun doNotHandleIfScheduledTimeLater() {
        val (_, handler) = prepareTestData(
            mockProcessors = false,
            scheduleTime = DEFAULT_TIME.plus(1, ChronoUnit.MINUTES)
        )
        handler.handle(listOf(1), DEFAULT_TIME)
        assertSoftly {
            verify(planFactService).getPlanFactGroupWithLock(1)
            verify(qualityRuleService, never()).findActiveRulesWithAggregation()
            verify(qualityRuleProcessor, never()).canProcess(any())
            verify(qualityRuleProcessor, never()).process(any(), any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.GROUP)
            transactionManager.commitsNumber shouldBe 1
        }
    }

    @Test
    @DisplayName("Не обрабатывать группы у которых scheduledTime отсутствует")
    fun doNotHandleIfScheduledTimeIsNull() {
        val (_, handler) = prepareTestData(
            mockProcessors = false,
            scheduleTime = null,
        )
        handler.handle(listOf(1), DEFAULT_TIME)
        assertSoftly {
            verify(planFactService).getPlanFactGroupWithLock(1)
            verify(qualityRuleService, never()).findActiveRulesWithAggregation()
            verify(qualityRuleProcessor, never()).canProcess(any())
            verify(qualityRuleProcessor, never()).process(any(), any())
            verify(schedulingLockService).releaseLock(setOf(1), SchedulingEntityType.GROUP)
            transactionManager.commitsNumber shouldBe 1
        }
    }

    private fun prepareTestData(
        mockProcessors: Boolean = true,
        scheduleTime: Instant? = DEFAULT_TIME.minus(1, ChronoUnit.HOURS),
        throwExceptionOnProcessing: Boolean = false,
        newScheduledTime: Instant? = NEW_SCHEDULE_TIME
    ): TestData {
        transactionManager.reset()
        val planFactGroup = PlanFactGroup(
            id = 1,
            expectedStatus = "OUT",
            scheduleTime = scheduleTime,
            waybillSegmentType = null,
            aggregationType = AggregationType.DATE,
            aggregationKey = "key1",
            processingStatus = null,
            data = PlanFactAdditionalDataMap(),
        ).apply {
            getData().aggregationEntity = AggregationEntity(date = DEFAULT_TIME.toLocalDate())
            created = DEFAULT_TIME - Duration.ofDays(1)
        }

        whenever(planFactService.getPlanFactGroupWithLock(any())).thenReturn(planFactGroup)

        val rule = QualityRule(
            id = 101,
            enabled = true,
            expectedStatus = "OUT",
            aggregationType = AggregationType.DATE,
            ruleProcessor = QualityRuleProcessorType.STARTREK,
        )

        if (mockProcessors) {
            whenever(qualityRuleService.findActiveRulesWithAggregation()).thenReturn(listOf(rule))
            whenever(qualityRuleProcessor.canProcess(any())).thenReturn(true)
            if (!throwExceptionOnProcessing) {
                whenever(qualityRuleProcessor.process(any(), any())).thenReturn(newScheduledTime)
            } else {
                whenever(qualityRuleProcessor.process(any(), any())).thenThrow(NullPointerException("Test exception"))
            }
        }
        val planFactGroupHandler = PlanFactGroupHandlerImpl(
            planFactService = planFactService,
            qualityRuleService = qualityRuleService,
            qualityRuleProcessors = listOf(qualityRuleProcessor),
            platformTransactionManager = transactionManager,
            clock = clock,
            schedulingLockService = schedulingLockService,
            properties= SchedulingProperties(useLock = true),
        )
        return TestData(planFactGroup, planFactGroupHandler)
    }

    private data class TestData(
        val planFactGroup: PlanFactGroup,
        val handler: PlanFactGroupHandlerImpl
    )

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private val NEW_SCHEDULE_TIME = DEFAULT_TIME.plus(5, ChronoUnit.MINUTES)
    }
}
