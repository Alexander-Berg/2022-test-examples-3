package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.tms.context.EmptyContext
import java.time.Instant
import javax.persistence.EntityManager


@DisplayName("Тесты AggregationExecutor")
internal class AggregationExecutorTest: AbstractContextualTest() {
    @Autowired
    lateinit var aggregationExecutor: AggregationExecutor

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var planFactService: PlanFactService

    @Autowired
    lateinit var partnerService: LmsPartnerService

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-09-26T11:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Создание новой группы при группировке план-факта")
    @DatabaseSetup("/tms/AggregationExecutor/before/createNewGroup.xml")
    @ExpectedDatabase(
        value = "/tms/AggregationExecutor/after/createNewGroup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createNewGroup() {
        aggregationExecutor.doJob(null)
    }

    @Test
    @DisplayName("Группировка нескольких план-фактов в одну существующую группу")
    @DatabaseSetup("/tms/AggregationExecutor/before/groupBatchIntoExistingGroup.xml")
    @ExpectedDatabase(
        value = "/tms/AggregationExecutor/after/groupBatchIntoExistingGroup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun groupBatchIntoExistingGroup() {
        aggregationExecutor.doJob(null)
    }

    @Test
    @DisplayName("Группировка одного план-факта в несколько групп")
    @DatabaseSetup("/tms/AggregationExecutor/before/groupPlanFactIntoSeveralGroups.xml")
    @ExpectedDatabase(
        value = "/tms/AggregationExecutor/after/groupPlanFactIntoSeveralGroups.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun groupPlanFactIntoSeveralGroups() {
        aggregationExecutor.doJob(null)
    }

    @Test
    @DisplayName("Доставать правильное количество планфактов")
    @DatabaseSetup("/tms/AggregationExecutor/before/loadRightNumberOfPlanFacts.xml")
    fun loadRightNumberOfPlanFacts() {
        val batch = transactionTemplate.execute {
            aggregationExecutor.getBatch(0, EmptyContext())
        }!!
        batch.size shouldBe 3
    }

    @Test
    @DisplayName("Не группировать изменившиеся план-факты")
    @DatabaseSetup("/tms/AggregationExecutor/before/failAggregationIfPlanFactChanged.xml")
    fun failAggregationIfPlanFactChanged() {
        doAnswer {
            updatePlanFactInNewTransaction(1L)
            it.callRealMethod()
        }.whenever(planFactService).savePlanFactAggregationEvent(any())
        aggregationExecutor.doJob(null)
        transactionTemplate.executeWithoutResult {
            val planFact = entityManager.find(PlanFact::class.java, 1L)
            planFact.planFactGroups.size shouldBe 0
        }

    }

    private fun updatePlanFactInNewTransaction(planFactId: Long) {
        val definition = DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW)
        val status = transactionTemplate.transactionManager!!.getTransaction(definition)
        val planFact = entityManager.find(PlanFact::class.java, planFactId)
        planFact.planFactStatus = PlanFactStatus.NOT_ACTUAL
        transactionTemplate.transactionManager!!.commit(status)
    }
}
