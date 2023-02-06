package ru.yandex.market.logistics.mqm.listeners

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.logging.enums.PlanFactProcessorLogEvent.DELETE_PLAN_FACT
import ru.yandex.market.logistics.mqm.logging.enums.PlanFactProcessorLogEvent.SAVE_PLAN_FACT
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetEntity
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class PlanFactLogListenerTest: AbstractContextualTest() {

    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @DisplayName("Проверка, что при схранении план-факта, вызывается листенер, который пишет в лог о событии.")
    @Test
    @ExpectedDatabase(
        value = "/listeners/after/create_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun savePlanFactTest() {
        val planFact = mockPlanFact()
        planFactRepository.saveAll(listOf(planFact))

        backLogCaptor.results.size shouldBe 1

        verifyPlanFactLogsEntityAndExtra(planFact, backLogCaptor.results[0], SAVE_PLAN_FACT.name)
    }

    @DisplayName("Проверка, что при изменении план-факта, вызывается листенер, который пишет в лог о событии.")
    @Test
    @DatabaseSetup("/listeners/before/alter_plan_fact.xml")
    @ExpectedDatabase(
        value = "/listeners/after/alter_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updatePlanFactTest() {
        val planFact = planFactRepository.findById(1L).get()
        val processingStatus = ProcessingStatus.PROCESSED
        val planFactStatus = PlanFactStatus.IN_TIME
        planFact.planFactStatus = planFactStatus
        planFact.processingStatus = processingStatus
        planFactRepository.saveAll(listOf(planFact))

        backLogCaptor.results.size shouldBe 1

        verifyPlanFactLogsEntityAndExtra(planFact, backLogCaptor.results[0], SAVE_PLAN_FACT.name)
    }

    @DisplayName("Проверка, что при удалении план-факта, вызывается листенер, который пишет о событии в лог.")
    @Test
    @DatabaseSetup("/listeners/before/delete_plan_fact.xml")
    @ExpectedDatabase(
        value = "/listeners/after/delete_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deletePlanFactTest() {
        val planFact = planFactRepository.findById(2L).get()
        planFactRepository.deleteById(2L)

        backLogCaptor.results.size shouldBe 1

        verifyPlanFactLogsEntityAndExtra(planFact, backLogCaptor.results[0], DELETE_PLAN_FACT.name)
    }

    private fun verifyPlanFactLogsEntityAndExtra(planFact: PlanFact, log: String, codeValue: String) {
        assertSoftly {
            tskvGetExtra(log) shouldContainExactlyInAnyOrder extraKeys.zip(planFact.getExtraValues())
            tskvGetEntity(log) shouldContainExactlyInAnyOrder entityKeys.zip(planFact.getEntityValues())
            tskvGetByKey(log, PAYLOAD_KEY) shouldBe Pair(PAYLOAD_KEY, codeValue)
        }
    }

    private fun mockPlanFact() = PlanFact(
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatus = "TRANSIT_DELIVERY_TRANSPORTATION",
        expectedStatusDatetime = Instant.parse("2021-03-30T19:00:00.00Z"),
        processingStatus = ProcessingStatus.ENQUEUED,
        planFactStatus = PlanFactStatus.CREATED,
    ).apply {
        entityId = 1332
        producerName = "test_producer"
    }

    private fun PlanFact.getExtraValues() = listOf(
        scheduleTime.toString(),
        planFactStatus.toString(),
        processingStatus.toString(),
        created.toString(),
        factStatusDatetime.toString(),
        producerName.toString()
    )

    private fun PlanFact.getEntityValues() = listOf(
        "planFact:$id",
        "entityId:$entityId",
        "entityType:$entityType",
    )

    companion object {
        private val extraKeys = setOf(
            "scheduleTime",
            "status",
            "processing_status",
            "createdTime",
            "factStatusDatetime",
            "processor"
        )

        private val entityKeys = setOf(
            "planFact",
            "entityId",
            "entityType"
        )
        private const val PAYLOAD_KEY = "payload"
    }

}
