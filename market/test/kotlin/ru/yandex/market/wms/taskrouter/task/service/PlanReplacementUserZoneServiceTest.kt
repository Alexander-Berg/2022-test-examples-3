package ru.yandex.market.wms.taskrouter.task.service

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.wms.taskrouter.task.component.GreedyAlgorithm
import ru.yandex.market.wms.taskrouter.task.component.PickingZoneComparator
import ru.yandex.market.wms.taskrouter.task.model.Zone
import ru.yandex.market.wms.taskrouter.task.model.dto.ReplacementPlanItem
import ru.yandex.market.wms.taskrouter.task.model.dto.ReplacementPlanKey
import ru.yandex.market.wms.taskrouter.task.model.dto.ReplacementPlanRequest
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.AbstractTaskCostCalculationPropertiesService
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.AbstractUserCostCalculationPropertiesService
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.BaseTaskCostBuilder
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.TaskCostBuilderForAbstractUser
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostCalculatorImpl.TaskCostBySkill
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostCalculatorImpl.TaskCostCalculatorByZoneDistance

@ExtendWith(SpringExtension::class)
@ContextConfiguration(
    classes = [
        PlanReplacementUserZoneService::class,
        PickingZoneComparator::class,
        GreedyAlgorithm::class,
        BaseTaskCostBuilder::class,
        TaskCostBuilderForAbstractUser::class,
        TaskCostBySkill::class,
        TaskCostCalculatorByZoneDistance::class,
        AbstractUserCostCalculationPropertiesService::class,
        AbstractTaskCostCalculationPropertiesService::class
    ]
)
internal class PlanReplacementUserZoneServiceTest {

    @Autowired
    lateinit var planReplacementUserZoneService: PlanReplacementUserZoneService

    companion object {
        val zones = mapOf(
            "Zone1" to Zone(name = "Zone1", priority = 10, maxTasks = 30),
            "Zone2" to Zone(name = "Zone2", priority = 20, maxTasks = 30),
            "Zone3" to Zone(name = "Zone3", priority = 30, maxTasks = 30),
            "Zone4" to Zone(name = "Zone4", priority = 40, maxTasks = 30)
        )
    }

    @Test
    fun getPlan() {
        val from: List<ReplacementPlanRequest> = listOf(
            ReplacementPlanRequest(zone = zones["Zone1"]!!, count = 5),
            ReplacementPlanRequest(zone = zones["Zone2"]!!, count = 5)
        )

        val to: List<ReplacementPlanRequest> = listOf(
            ReplacementPlanRequest(zone = zones["Zone3"]!!, count = 8),
            ReplacementPlanRequest(zone = zones["Zone4"]!!, count = 8)
        )

        val expected = listOf(
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone1"]!!, toZone = zones["Zone3"]!!),
                count = 5
            ),
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone2"]!!, toZone = zones["Zone3"]!!),
                count = 3
            ),
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone2"]!!, toZone = zones["Zone4"]!!),
                count = 2
            )
        )

        val result = planReplacementUserZoneService.getReplacementPlan(from, to)

        assertAll(
            { assertEquals(3, result.size) },
            { assertThat(result, containsInAnyOrder(expected[0], expected[1], expected[2])) }
        )
    }
}
