package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.PlanFactAggregationServiceProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.repository.PlanFactGroupRelationsRepository
import ru.yandex.market.logistics.mqm.repository.PlanFactGroupRepository
import ru.yandex.market.logistics.mqm.service.processor.aggregationentity.BaseAggregationEntityProducer
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
internal class PlanFactAggregationServiceImplTest : AbstractTest() {

    @Mock
    lateinit var clock: Clock

    @Mock
    lateinit var planFactGroupRepository: PlanFactGroupRepository

    @Mock
    lateinit var planFactService: PlanFactService

    @Mock
    lateinit var planFactGroupRelationsRepository: PlanFactGroupRelationsRepository

    @Captor
    lateinit var groupCaptor: ArgumentCaptor<PlanFactGroup>

    private fun createAggregationService(manualLink: Boolean = false): PlanFactAggregationService {
        return PlanFactAggregationServiceImpl(
            planFactGroupRepository = planFactGroupRepository,
            planFactGroupRelationsRepository = planFactGroupRelationsRepository,
            clock = clock,
            aggregationEntityProducers = listOf(TestDateProducer()),
            planFactService = planFactService,
            properties = PlanFactAggregationServiceProperties(
                manualLink = manualLink,
            )
        )
    }

    @Test
    @DisplayName("Создание новой группы при группировке план-факта")
    fun createNewGroupLegacy() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(planFactGroupRepository.saveAndFlush(any())).thenReturn(PlanFactGroup())
        whenever(planFactGroupRepository.findGroupForAggregationLegacy(any(), any(), any(), anyOrNull()))
            .thenReturn(listOf())
        val aggregationService = createAggregationService()
        val planFact = mockPlanFact()
        val groups = aggregationService.linkPlanFactsToGroups(listOf(planFact))
        assertSoftly {
            verify(planFactGroupRepository, times(1))
                .findGroupForAggregationLegacy(any(), any(), any(), anyOrNull())
            verify(planFactGroupRepository, times(1)).saveAndFlush(groupCaptor.capture())
            groupCaptor.value.aggregationType shouldBe AggregationType.DATE
            groupCaptor.value.expectedStatus shouldBe "OUT"
            groupCaptor.value.aggregationKey shouldBe "date:2021-09-27;"
            groups.size shouldBe 1
            groups.first().planFacts.shouldContainExactlyInAnyOrder(planFact)
            planFact.planFactGroups.shouldContainExactlyInAnyOrder(groups.first())
        }
    }

    @Test
    @DisplayName("Создание новой группы при группировке план-факта")
    fun createNewGroup() {
        val testGroup = PlanFactGroup(id = 1)
        val testPlanFact = mockPlanFact()
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(planFactGroupRepository.saveAndFlush(any())).thenReturn(testGroup)
        whenever(planFactGroupRepository.findGroupForAggregation(any(), any(), any(), anyOrNull()))
            .thenReturn(listOf())
        doNothing().whenever(planFactGroupRelationsRepository).link(testGroup, setOf(testPlanFact))

        val aggregationService = createAggregationService(manualLink = true)

        aggregationService.linkPlanFactsToGroups(listOf(testPlanFact))
        assertSoftly {
            verify(planFactGroupRepository, times(1))
                .findGroupForAggregation(any(), any(), any(), anyOrNull())
            verify(planFactGroupRepository, times(1)).saveAndFlush(groupCaptor.capture())
            groupCaptor.value.aggregationType shouldBe AggregationType.DATE
            groupCaptor.value.expectedStatus shouldBe "OUT"
            groupCaptor.value.aggregationKey shouldBe "date:2021-09-27;"
        }
        verify(planFactGroupRelationsRepository).link(testGroup, setOf(testPlanFact))
    }

    @Test
    @DisplayName("Группировка в существующую группу")
    fun aggregateIntoExistingGroupLegacy() {
        val existingGroup = PlanFactGroup(
            expectedStatus = "OUT",
            aggregationType = AggregationType.DATE,
            aggregationKey = "date:2021-09-27;"

        )
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(planFactGroupRepository.findGroupForAggregationLegacy(any(), any(), any(), anyOrNull()))
            .thenReturn(listOf(existingGroup))
        val aggregationService = createAggregationService()
        val planFact = mockPlanFact()
        val groups = aggregationService.linkPlanFactsToGroups(listOf(planFact))
        assertSoftly {
            verify(planFactGroupRepository, times(1))
                .findGroupForAggregationLegacy(any(), any(), any(), anyOrNull())
            verify(planFactGroupRepository, never()).saveAndFlush(any())
            groups.size shouldBe 1
            groups.first().planFacts.shouldContainExactlyInAnyOrder(planFact)
            planFact.planFactGroups.shouldContainExactlyInAnyOrder(groups.first())
        }
    }

    @Test
    @DisplayName("Группировка в существующую группу")
    fun aggregateIntoExistingGroup() {
        val existingGroup = PlanFactGroup(
            expectedStatus = "OUT",
            aggregationType = AggregationType.DATE,
            aggregationKey = "date:2021-09-27;"

        )
        val testPlanFact = mockPlanFact()
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(planFactGroupRepository.findGroupForAggregation(any(), any(), any(), anyOrNull()))
            .thenReturn(listOf(existingGroup))
        doNothing().whenever(planFactGroupRelationsRepository).link(existingGroup, setOf(testPlanFact))

        val aggregationService = createAggregationService(manualLink = true)

        aggregationService.linkPlanFactsToGroups(listOf(testPlanFact))
        assertSoftly {
            verify(planFactGroupRepository, times(1))
                .findGroupForAggregation(any(), any(), any(), anyOrNull())
            verify(planFactGroupRepository, never()).saveAndFlush(any())
        }
        verify(planFactGroupRelationsRepository).link(existingGroup, setOf(testPlanFact))
    }

    private fun mockPlanFact() = PlanFact(
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatus = "OUT",
        expectedStatusDatetime = DEFAULT_TIME.plus(1, ChronoUnit.DAYS),
        processingStatus = ProcessingStatus.ENQUEUED,
        planFactStatus = PlanFactStatus.ACTIVE,
        producerName = "TestProducer",
    ).apply {
        entity = WaybillSegment().apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
    }

    companion object {
        val DEFAULT_TIME = Instant.parse("2021-09-26T11:00:00.00Z")
    }

    private class TestDateProducer : BaseAggregationEntityProducer(
        EntityType.LOM_WAYBILL_SEGMENT,
        AggregationType.DATE,
        supportedProducers = setOf("TestProducer")
    ) {

        override fun produceEntity(planFact: PlanFact) = AggregationEntity(
            date = planFact.expectedStatusDatetime?.atZone(DateTimeUtils.MOSCOW_ZONE)?.toLocalDate()
        )
    }
}


