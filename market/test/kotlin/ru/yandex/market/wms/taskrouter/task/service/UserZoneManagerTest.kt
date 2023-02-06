package ru.yandex.market.wms.taskrouter.task.service

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.wms.taskrouter.task.dao.ZoneDao
import ru.yandex.market.wms.taskrouter.task.model.Zone

@ExtendWith(SpringExtension::class)
@ContextConfiguration(
    classes = [
        UserZoneManager::class
    ]
)
internal class UserZoneManagerTest {
    @MockBean
    lateinit var zoneDao: ZoneDao

    @MockBean
    lateinit var taskAssignService: TaskAssignService

    @Autowired
    lateinit var userZoneManager: UserZoneManager

    private val zones: List<Zone> = listOf(
        Zone(
            name = "MEZONIN1",
            priority = 160,
            maxTasks = 10
        ),
        Zone(
            name = "MEZONIN2",
            priority = 90,
            maxTasks = 10
        ),
        Zone(
            name = "MEZONIN3",
            priority = 130,
            maxTasks = 10
        )
    )

    @BeforeEach
    fun configureMock() {
        Mockito.`when`(zoneDao.findAllZonesWithPriority())
            .thenReturn(zones)
    }

    @Test
    fun `orderZone should order ad return zone in right order when assign zone empty`() {

        Mockito.`when`(zoneDao.findPreviousZone("")).thenReturn(zones.first { it.name == "MEZONIN2" })

        Mockito.`when`(taskAssignService.assignZoneName("")).thenReturn(null)

        val result = userZoneManager.orderZone("")

        assertAll(
            { assertEquals(3, result.size) },
            {
                MatcherAssert.assertThat(
                    result, Matchers.containsInRelativeOrder(
                        Zone(
                            name = "MEZONIN2",
                            priority = 90,
                            maxTasks = 10
                        ),
                        Zone(
                            name = "MEZONIN3",
                            priority = 130,
                            maxTasks = 10
                        ),
                        Zone(
                            name = "MEZONIN1",
                            priority = 160,
                            maxTasks = 10
                        )
                    )
                )
            }
        )
    }

    @Test
    fun `orderZone should order ad return zone in right order when last zone empty`() {

        Mockito.`when`(zoneDao.findPreviousZone("")).thenReturn(null)

        Mockito.`when`(taskAssignService.assignZoneName("")).thenReturn("MEZONIN1")

        val result = userZoneManager.orderZone("")

        assertAll(
            { assertEquals(3, result.size) },
            {
                MatcherAssert.assertThat(
                    result, Matchers.containsInRelativeOrder(
                        Zone(
                            name = "MEZONIN1",
                            priority = 160,
                            maxTasks = 10
                        ),
                        Zone(
                            name = "MEZONIN2",
                            priority = 90,
                            maxTasks = 10
                        ),
                        Zone(
                            name = "MEZONIN3",
                            priority = 130,
                            maxTasks = 10
                        )
                    )
                )
            }
        )
    }

    @Test
    fun `orderZone should order ad return zone in right order`() {

        Mockito.`when`(zoneDao.findPreviousZone("")).thenReturn(zones.first { it.name == "MEZONIN2" })

        Mockito.`when`(taskAssignService.assignZoneName("")).thenReturn("MEZONIN1")

        val result = userZoneManager.orderZone("")

        assertAll(
            { assertEquals(3, result.size) },
            {
                MatcherAssert.assertThat(
                    result, Matchers.containsInRelativeOrder(
                        Zone(
                            name = "MEZONIN1",
                            priority = 160,
                            maxTasks = 10
                        ),
                        Zone(
                            name = "MEZONIN2",
                            priority = 90,
                            maxTasks = 10
                        ),
                        Zone(
                            name = "MEZONIN3",
                            priority = 130,
                            maxTasks = 10
                        )
                    )
                )
            }
        )
    }
}
