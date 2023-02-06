package ru.yandex.market.logistics.mqm.repository

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus

class PlanFactGroupRelationsRepositoryImplTest: AbstractContextualTest() {

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    lateinit var repository: PlanFactGroupRelationsRepository

    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @Autowired
    private lateinit var planFactGroupRepository: PlanFactGroupRepository

    @Test
    fun linkNew() {
        val testGroup = mockGroup()
        val testPlanFact = mockPlanFact()

        transactionTemplate.execute {
            repository.link(
                planFactGroupRepository.getOne(testGroup.id),
                setOf(planFactRepository.getOne(testPlanFact.id))
            )
        }

        transactionTemplate.execute { verifyLinked(testGroup, setOf(testPlanFact)) }
    }

    @Test
    fun linkOneNewAndOneExisted() {
        val testGroup = mockGroup()
        val testPlanFactExisted = mockPlanFact()
        transactionTemplate.execute {
            testGroup.planFacts.add(testPlanFactExisted)
            testPlanFactExisted.planFactGroups.add(testGroup)
        }
        val testPlanFact = mockPlanFact()

        transactionTemplate.execute {
            repository.link(
                planFactGroupRepository.getOne(testGroup.id),
                setOf(
                    planFactRepository.getOne(testPlanFact.id),
                    planFactRepository.getOne(testPlanFactExisted.id),
                )
            )
        }

        transactionTemplate.execute { verifyLinked(testGroup, setOf(testPlanFactExisted, testPlanFact)) }
    }

    private fun mockGroup() = planFactGroupRepository.saveAndFlush(
        PlanFactGroup(
            processingStatus = ProcessingStatus.ENQUEUED,
            aggregationKey = "TEST",
            expectedStatus = "TEST",
            aggregationType = AggregationType.DATE_DELIVERY_TYPE_FOR_CONTRACT_DELIVERY,
        )
    )

    private fun mockPlanFact() = planFactRepository.saveAndFlush(
        PlanFact(
            expectedStatusDatetime = clock.instant(),
            expectedStatus = "TEST",
            planFactStatus = PlanFactStatus.ACTIVE,
            processingStatus = ProcessingStatus.ENQUEUED,
        )
    )

    private fun verifyLinked(planFactGroup: PlanFactGroup, planFacts: Set<PlanFact>) {
        planFacts.forEach { planFact ->
            planFactRepository.findByIdOrNull(planFact.id)!!.planFactGroups shouldContainExactlyInAnyOrder
                setOf(planFactGroup)
        }
        planFactGroupRepository.findByIdOrNull(planFactGroup.id)!!.planFacts shouldContainExactlyInAnyOrder planFacts
    }
}
