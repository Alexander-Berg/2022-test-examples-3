package ru.yandex.market.logistics.mqm.listeners

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.hibernate.Hibernate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.logging.enums.PlanFactProcessorLogEvent.DELETE_PLAN_FACT_GROUP
import ru.yandex.market.logistics.mqm.logging.enums.PlanFactProcessorLogEvent.SAVE_PLAN_FACT_GROUP
import ru.yandex.market.logistics.mqm.repository.PlanFactGroupRepository
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetEntity
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Instant

class PlanFactGroupLogListenerTest: AbstractContextualTest() {

    @Autowired
    private lateinit var planFactGroupRepository: PlanFactGroupRepository

    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @Autowired
    private lateinit var transactionalTemplate: TransactionTemplate

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @DisplayName("Провека, что план-факт группа создается и триггерит листенер, который записывает логи.")
    @Test
    @ExpectedDatabase(
        value = "/listeners/after/create_plan_fact_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun savePlanFactGroupTest() {
        val planFactGroup = mockPlanFactGroup()
        planFactRepository.saveAll(planFactGroup.planFacts)
        planFactGroupRepository.save(planFactGroup)

        backLogCaptor.results.size shouldBe 3

        verifyPlanFactGroupLogEntityAndExtra(planFactGroup, backLogCaptor.results.last(), SAVE_PLAN_FACT_GROUP.name)
    }

    @DisplayName(
        "Проверка, что план-факт группа обновляется и триггерит листенер, который записывает логи " +
            "с не инициализированными план-фактами группы."
    )
    @Test
    @DatabaseSetup("/listeners/before/plan_fact_group_with_pf.xml")
    @ExpectedDatabase(
        value = "/listeners/after/update_plan_fact_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updatePlanFactGroupNonInitializedPlanFactsTest() {
        val planFactGroup = planFactGroupRepository.findById(1L).get()
        planFactGroup.processingStatus = ProcessingStatus.PROCESSED
        planFactGroupRepository.save(planFactGroup)

        Hibernate.isInitialized(planFactGroup.planFacts) shouldBe false
        backLogCaptor.results.size shouldBe 1

        verifyPlanFactGroupLogEntityAndExtra(planFactGroup, backLogCaptor.results[0], SAVE_PLAN_FACT_GROUP.name)
    }

    @DisplayName(
        "Проверка, что план-факт группа обновляется и триггерит листенер, который записывает логи " +
            "с инициализированными план-фактами группы."
    )
    @Test
    @DatabaseSetup("/listeners/before/plan_fact_group_with_pf.xml")
    @ExpectedDatabase(
        value = "/listeners/after/update_plan_fact_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updatePlanFactGroupInitializedPlanFactsTest() {
        val planFactGroup = transactionalTemplate.execute {
            val planFactGroup = planFactGroupRepository.findById(1L).get()

            Hibernate.isInitialized(planFactGroup.planFacts) shouldBe false
            Hibernate.initialize(planFactGroup.planFacts)
            Hibernate.isInitialized(planFactGroup.planFacts) shouldBe true

            planFactGroup.processingStatus = ProcessingStatus.PROCESSED
            return@execute planFactGroup
        }

        backLogCaptor.results.size shouldBe 1

        verifyPlanFactGroupLogEntityAndExtra(planFactGroup, backLogCaptor.results[0], SAVE_PLAN_FACT_GROUP.name)
    }

    @DisplayName("Провека, что план-факт группа удаляется и триггерит листенер, который записывает логи.")
    @Test
    @DatabaseSetup("/listeners/before/plan_fact_group_with_pf.xml")
    @ExpectedDatabase(
        value = "/listeners/after/delete_plan_fact_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deletePlanFactGroupTest() {
        val planFactGroup = transactionalTemplate.execute {
            val planFactGroup = planFactGroupRepository.findById(1L).get()

            Hibernate.isInitialized(planFactGroup.planFacts) shouldBe false
            Hibernate.initialize(planFactGroup.planFacts)
            Hibernate.isInitialized(planFactGroup.planFacts) shouldBe true

            planFactGroupRepository.deleteById(1L)
            return@execute planFactGroup
        }

        backLogCaptor.results.size shouldBe 1

        verifyPlanFactGroupLogEntityAndExtra(planFactGroup, backLogCaptor.results[0], DELETE_PLAN_FACT_GROUP.name)
    }

    @DisplayName("Провека, что при 50+ инициализированных план фактах, в логах они отображаются как [1; 2; .. 50; ...]")
    @Test
    fun saveOver50PlanFactsTest() {
        val planFacts = (1..51).map { mockPlanFact(it.toLong()) }
        val planFactGroup = mockPlanFactGroup()
        planFactGroup.planFacts = planFacts.toMutableSet()
        planFactRepository.saveAll(planFacts)
        planFactGroupRepository.save(planFactGroup)

        backLogCaptor.results.last().contains(
            (1..50).joinToString(
                separator = "; ",
                prefix = "[",
                postfix = "; ...]"
            )
        )
    }

    private fun verifyPlanFactGroupLogEntityAndExtra(
        planFactGroup: PlanFactGroup,
        log: String,
        operationTypeName: String
    ) {
        assertSoftly {
            tskvGetExtra(log) shouldContainExactlyInAnyOrder extraKeys.zip(planFactGroup.getExtraValues())
            tskvGetEntity(log) shouldContainExactlyInAnyOrder entityTypes.zip(planFactGroup.getEntityValues())
            tskvGetByKey(log, "payload") shouldBe Pair("payload", operationTypeName)
        }
    }

    private fun mockPlanFactGroup(): PlanFactGroup {
        return PlanFactGroup(
            aggregationKey = "date:2020-11-07;partnerFrom:987654321;",
            aggregationType = AggregationType.DROPOFF_SC_INTAKE,
            processingStatus = ProcessingStatus.ENQUEUED,
            expectedStatus = "IN",
            scheduleTime = Instant.parse("2021-03-30T19:00:00.00Z"),
        ).apply {
            planFacts = mutableSetOf(
                mockPlanFact(),
                mockPlanFact(id = 1442)
            )
            waybillSegmentType = SegmentType.SORTING_CENTER
        }
    }

    private fun mockPlanFact(id: Long = 1332): PlanFact {
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatus = "TRANSIT_DELIVERY_TRANSPORTATION",
            expectedStatusDatetime = Instant.parse("2021-03-30T19:00:00.00Z"),
            processingStatus = ProcessingStatus.ENQUEUED,
            planFactStatus = PlanFactStatus.CREATED,
        ).apply {
            entityId = id
            producerName = "test_producer"
        }
    }

    private fun PlanFactGroup.getExtraValues(): MutableList<String> {
        val listDefault = mutableListOf(
            this.scheduleTime.toString(),
            this.aggregationKey.toString(),
            this.expectedStatus.toString(),
            this.processingStatus.toString(),
            this.waybillSegmentType.toString(),
            this.created.toString(),
            this.aggregationType.toString()
        )

        if (Hibernate.isInitialized(this.planFacts)) {
            listDefault.add(this.planFacts.size.toString())
            listDefault.add(this.planFacts.joinToString(
                separator = "; ",
                prefix = "[",
                postfix = "]",
                limit = 50
            ) { it.id.toString() })
        } else {
            listDefault.addAll(
                listOf(
                    PlanFactGroupLogListener.NOT_INITIALIZED,
                    PlanFactGroupLogListener.NOT_INITIALIZED
                )
            )
        }
        return listDefault
    }

    private fun PlanFactGroup.getEntityValues(): MutableSet<String> = mutableSetOf("planFactGroup:$id")

    companion object {
        private val entityTypes = setOf("planFactGroup")
        private val extraKeys = setOf(
            "scheduleTime",
            "aggregationKey",
            "expectedStatus",
            "processing_status",
            "waybillSegmentType",
            "createdTime",
            "aggregationType",
            "planFactAmount",
            "planFactsIds"
        )
    }

}
