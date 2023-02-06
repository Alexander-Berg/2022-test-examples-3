package ru.yandex.market.wms.taskrouter.task.service

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.wms.common.model.enums.ProcessType
import ru.yandex.market.wms.taskrouter.task.dao.PickingTaskDao
import ru.yandex.market.wms.taskrouter.task.model.AssignZoneModel
import ru.yandex.market.wms.taskrouter.task.model.Zone
import ru.yandex.market.wms.taskrouter.task.model.dto.CurrentUserZone
import ru.yandex.market.wms.taskrouter.task.model.dto.ReplacementPlanItem
import ru.yandex.market.wms.taskrouter.task.model.dto.ReplacementPlanKey
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ContextConfiguration(
    classes = [
        ReplacementPlanUserPicker::class
    ]
)
internal class ReplacementPlanUserPickerTest {

    @MockBean
    lateinit var taskDao: PickingTaskDao

    @Autowired
    lateinit var replacementPlanUserPicker: ReplacementPlanUserPicker

    companion object {
        val zones = mapOf(
            "Zone1" to Zone(
                name = "Zone1",
                priority = 1,
                maxTasks = 10
            ),
            "Zone2" to Zone(
                name = "Zone2",
                priority = 2,
                maxTasks = 10
            ),
            "Zone3" to Zone(
                name = "Zone3",
                priority = 3,
                maxTasks = 10
            )
        )

        val currentUserZones = listOf(
            CurrentUserZone(
                zone = "Zone1",
                user = "user1",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = emptyList()
            ),
            CurrentUserZone(
                zone = "Zone1",
                user = "user2",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = emptyList()
            ), CurrentUserZone(
                zone = "Zone1",
                user = "user3",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = emptyList()
            ), CurrentUserZone(
                zone = "Zone1",
                user = "user4",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = emptyList()
            ),
            CurrentUserZone(
                zone = "Zone1",
                user = "user5",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = emptyList()
            ),
            CurrentUserZone(
                zone = "Zone1",
                user = "user6",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = emptyList()
            )

        )
    }

    @Test
    fun pickWithoutSkill() {

        val replacementPlan = listOf(
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone1"]!!, toZone = zones["Zone2"]!!),
                count = 2
            ),
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone1"]!!, toZone = zones["Zone3"]!!),
                count = 2
            )
        )
        Mockito.`when`(taskDao.userZoneWithSkill()).thenReturn(currentUserZones)

        val result = replacementPlanUserPicker.pick(replacementPlan, "test")

        Assertions.assertAll(
            { Assertions.assertEquals(4, result.size) },
            {
                MatcherAssert.assertThat(
                    result,
                    Matchers.containsInAnyOrder(
                        AssignZoneModel(
                            userName = "user1",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone2",
                            addWho = "test"
                        ),
                        AssignZoneModel(
                            userName = "user2",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone2",
                            addWho = "test"
                        ),
                        AssignZoneModel(
                            userName = "user3",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone3",
                            addWho = "test"
                        ),
                        AssignZoneModel(
                            userName = "user4",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone3",
                            addWho = "test"
                        )
                    )
                )
            }
        )
    }

    @Test
    fun pickWitSkill() {

        val replacementPlan = listOf(
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone1"]!!, toZone = zones["Zone2"]!!),
                count = 2
            ),
            ReplacementPlanItem(
                key = ReplacementPlanKey(fromZone = zones["Zone1"]!!, toZone = zones["Zone3"]!!),
                count = 2
            )
        )
        Mockito.`when`(taskDao.userZoneWithSkill()).thenReturn(listOf(
            CurrentUserZone(
                zone = "Zone1",
                user = "user1",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = listOf("Zone1", "Zone2")
            ),
            CurrentUserZone(
                zone = "Zone1",
                user = "user2",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = listOf("Zone1", "Zone2")
            ), CurrentUserZone(
                zone = "Zone1",
                user = "user3",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = listOf("Zone1", "Zone3")
            ), CurrentUserZone(
                zone = "Zone1",
                user = "user4",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = listOf("Zone1", "Zone3")
            ),
            CurrentUserZone(
                zone = "Zone1",
                user = "user5",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = listOf("Zone1", "Zone3")
            ),
            CurrentUserZone(
                zone = "Zone1",
                user = "user6",
                startTime = LocalDateTime.now(),
                processType = ProcessType.PICKING,
                skills = listOf("Zone1", "Zone3")
            )
        ))

        val result = replacementPlanUserPicker.pick(replacementPlan, "test")

        Assertions.assertAll(
            { Assertions.assertEquals(4, result.size) },
            {
                MatcherAssert.assertThat(
                    result,
                    Matchers.containsInAnyOrder(
                        AssignZoneModel(
                            userName = "user1",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone2",
                            addWho = "test"
                        ),
                        AssignZoneModel(
                            userName = "user2",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone2",
                            addWho = "test"
                        ),
                        AssignZoneModel(
                            userName = "user3",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone3",
                            addWho = "test"
                        ),
                        AssignZoneModel(
                            userName = "user4",
                            processType = ProcessType.PICKING,
                            zoneId = "Zone3",
                            addWho = "test"
                        )
                    )
                )
            }
        )
    }
}
