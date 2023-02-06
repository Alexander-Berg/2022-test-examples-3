package ru.yandex.market.wms.taskrouter.task.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.wms.common.model.enums.ProcessType
import ru.yandex.market.wms.shared.libs.business.logger.BusinessLogger
import ru.yandex.market.wms.taskrouter.task.component.GreedyAlgorithm
import ru.yandex.market.wms.taskrouter.task.dao.AssignZoneDao
import ru.yandex.market.wms.taskrouter.task.dao.NormPerformanceByZoneDao
import ru.yandex.market.wms.taskrouter.task.dao.PickingTaskDao
import ru.yandex.market.wms.taskrouter.task.dao.ZoneDao
import ru.yandex.market.wms.taskrouter.task.model.CountPeopleOnZone
import ru.yandex.market.wms.taskrouter.task.model.CountTaskOnZone
import ru.yandex.market.wms.taskrouter.task.model.NormPerformance
import ru.yandex.market.wms.taskrouter.task.model.Zone
import ru.yandex.market.wms.taskrouter.task.model.dto.CurrentUserZone
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.AbstractTaskCostCalculationPropertiesService
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.AbstractUserCostCalculationPropertiesService
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.BaseTaskCostBuilder
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostBuilder.TaskCostBuilderForAbstractUser
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostCalculatorImpl.TaskCostBySkill
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.taskCostCalculatorImpl.TaskCostCalculatorByZoneDistance
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ContextConfiguration(
    classes = [
        AssignZoneService::class,
        MissingCountPickingCalculator::class,
        PlanReplacementUserZoneService::class,
        TaskAssignService::class,
        ReplacementPlanUserPicker::class,
        GreedyAlgorithm::class,
        TaskCostBuilderForAbstractUser::class,
        BaseTaskCostBuilder::class,
        TaskCostBySkill::class,
        TaskCostCalculatorByZoneDistance::class,
        AbstractTaskCostCalculationPropertiesService::class,
        AbstractUserCostCalculationPropertiesService::class,

    ]
)
internal class AssignZoneServiceTest {
    @Autowired
    private lateinit var assignZoneService: AssignZoneService

    @MockBean
    private lateinit var zoneDao: ZoneDao

    @MockBean
    private lateinit var taskDao: PickingTaskDao

    @MockBean
    private lateinit var normPerformanceByZoneDao: NormPerformanceByZoneDao

    @MockBean
    private lateinit var assignZoneDao: AssignZoneDao

    @MockBean
    private lateinit var businessLogger: BusinessLogger

    @BeforeEach
    fun init() {
        Mockito.doNothing().`when`(assignZoneDao).assignUserToZone(Mockito.anyCollection(), Mockito.anyString())
        Mockito.doNothing().`when`(businessLogger).info(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
    }

    @Test
    fun assignUserOnZone() {

        Mockito.`when`(normPerformanceByZoneDao.findAllByProcessType(ProcessType.PICKING))
            .thenReturn(
                listOf(
                    NormPerformance(zoneId = "MEZONIN_2", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN_3", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN_4", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN2_2", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN3_2", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN4_2", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN_1", ProcessType.PICKING, performance = 70),
                    NormPerformance(zoneId = "MEZONIN1_2", ProcessType.PICKING, performance = 70),
                    NormPerformance(zoneId = "FASHION_1", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "FASHION1_2", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "FASHION5_2", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN_5", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN2_1", ProcessType.PICKING, performance = 130),
                    NormPerformance(zoneId = "MEZONIN5_2", ProcessType.PICKING, performance = 130)
                )
            )

        Mockito.`when`(taskDao.countUsersOnZone()).thenReturn(
            listOf(
                CountPeopleOnZone(zoneId = "BRAK_D", count = 1, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "BRAK_SKL", count = 6, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "ELECTRON", count = 1, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "FASHION_1", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "FASHION1_2", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN_1", count = 4, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN_2", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = " MEZONIN_3", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN_5", count = 1, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN1_2", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN2_1", count = 1, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN2_2", count = 3, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN3_2", count = 3, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "MEZONIN4_2", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "OTBOR_KGT", count = 2, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "PALLET_D", count = 5, processType = ProcessType.PICKING),
                CountPeopleOnZone(zoneId = "UPK", count = 1, processType = ProcessType.PICKING),
            )
        )

        Mockito.`when`(taskDao.backLogCount()).thenReturn(
            listOf(
                CountTaskOnZone(zoneId = "BRAK_SKL", count = 1, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "ELECTRON", count = 3, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "FASHION1_2", count = 16, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN_1", count = 25, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN_2", count = 10, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN_3", count = 6, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN_4", count = 12, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN_5", count = 6, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN1_2", count = 29, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN2_1", count = 1, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN2_2", count = 4, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN3_2", count = 8, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN4_2", count = 1, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "MEZONIN5_2", count = 2, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "OTBOR_KGT", count = 16, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "PALLET_D", count = 14, processType = ProcessType.PICKING),
                CountTaskOnZone(zoneId = "UPK", count = 2, processType = ProcessType.PICKING),
            )
        )

        Mockito.`when`(zoneDao.findById(anyCollection())).thenReturn(
            listOf(
                Zone(name = "BRAK_D", maxTasks = 1, priority = null),
                Zone(name = "BRAK_SKL", maxTasks = 1, priority = null),
                Zone(name = "ELECTRON", maxTasks = 4, priority = null),
                Zone(name = " FASHION_1", maxTasks = 4, priority = 1),
                Zone(name = "FASHION1_2", maxTasks = 4, priority = 2),
                Zone(name = "MEZONIN_1", maxTasks = 4, priority = 1),
                Zone(name = "MEZONIN_2", maxTasks = 4, priority = 3),
                Zone(name = "MEZONIN_3", maxTasks = 4, priority = 5),
                Zone(name = "MEZONIN_4", maxTasks = 4, priority = 7),
                Zone(name = "MEZONIN_5", maxTasks = 4, priority = 9),
                Zone(name = "MEZONIN1_2", maxTasks = 4, priority = 2),
                Zone(name = "MEZONIN2_1", maxTasks = 4, priority = 4),
                Zone(name = "MEZONIN2_2", maxTasks = 4, priority = 4),
                Zone(name = "MEZONIN3_2", maxTasks = 4, priority = 6),
                Zone(name = "MEZONIN4_2", maxTasks = 4, priority = 8),
                Zone(name = "MEZONIN5_2", maxTasks = 4, priority = 10),
                Zone(name = "OTBOR_KGT", maxTasks = 2, priority = null),
                Zone(name = "PALLET_D", maxTasks = 2, priority = null),
                Zone(name = "UPK", maxTasks = null, priority = null),
            )
        )

        Mockito.`when`(taskDao.userZoneWithSkill()).thenReturn(
            listOf(
                CurrentUserZone(
                    user = "user-test-1",
                    zone = "BRAK_D",
                    startTime = LocalDateTime.parse("2022-05-07T06:55:47"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-2",
                    zone = "BRAK_SKL",
                    startTime = LocalDateTime.parse("2022-05-07T06:55:47"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-3",
                    zone = "ELECTRON",
                    startTime = LocalDateTime.parse("2022-05-07T08:58:43"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-4",
                    zone = "FASHION_1",
                    startTime = LocalDateTime.parse("2022-05-07T09:14:43"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-5",
                    zone = "FASHION1_2",
                    startTime = LocalDateTime.parse("2022-05-07T04:04:35"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-6",
                    zone = "MEZONIN_1",
                    startTime = LocalDateTime.parse("2022-05-07T07:16:48"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-7",
                    zone = "MEZONIN_1",
                    startTime = LocalDateTime.parse("2022-05-07T08:29:40"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-8",
                    zone = "MEZONIN_1",
                    startTime = LocalDateTime.parse("2022-05-07T08:51:57"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-9",
                    zone = "MEZONIN_1",
                    startTime = LocalDateTime.parse("2022-05-07T09:00:55"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-10",
                    zone = "MEZONIN_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:24:09"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-11",
                    zone = "MEZONIN_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:33:29"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-12",
                    zone = "MEZONIN_3",
                    startTime = LocalDateTime.parse("2022-05-07T08:29:40"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-13",
                    zone = "MEZONIN_3",
                    startTime = LocalDateTime.parse("2022-05-07T08:33:29"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-14",
                    zone = "MEZONIN_3",
                    startTime = LocalDateTime.parse("2022-05-07T08:54:23"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-15",
                    zone = "MEZONIN_4",
                    startTime = LocalDateTime.parse("2022-05-07T08:54:23"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-16",
                    zone = "MEZONIN_4",
                    startTime = LocalDateTime.parse("2022-05-07T09:04:01"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-17",
                    zone = "MEZONIN1_2",
                    startTime = LocalDateTime.parse("2022-05-07T07:16:48"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-18",
                    zone = "MEZONIN2_1",
                    startTime = LocalDateTime.parse("2022-05-07T08:58:43"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-19",
                    zone = "MEZONIN2_1",
                    startTime = LocalDateTime.parse("2022-05-07T09:14:43"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-20",
                    zone = "MEZONIN2_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:24:09"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-21",
                    zone = "MEZONIN2_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:51:57"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-22",
                    zone = "MEZONIN2_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:54:23"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-23",
                    zone = "MEZONIN3_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:51:57"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-24",
                    zone = "MEZONIN4_2",
                    startTime = LocalDateTime.parse("2022-05-07T08:58:43"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-25",
                    zone = "OTBOR_KGT",
                    startTime = LocalDateTime.parse("2022-05-07T06:25:13"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-26",
                    zone = "OTBOR_KGT",
                    startTime = LocalDateTime.parse("2022-05-07T08:55:28"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-27",
                    zone = "OTBOR_KGT",
                    startTime = LocalDateTime.parse("2022-05-07T08:55:28"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-28",
                    zone = "PALLET_D",
                    startTime = LocalDateTime.parse("2022-05-07T08:10:08"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-29",
                    zone = "PALLET_D",
                    startTime = LocalDateTime.parse("2022-05-07T08:39:57"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-30",
                    zone = "PALLET_D",
                    startTime = LocalDateTime.parse("2022-05-07T08:58:43"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-31",
                    zone = "PALLET_D",
                    startTime = LocalDateTime.parse("2022-05-07T09:00:55"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),
                CurrentUserZone(
                    user = "user-test-32",
                    zone = "UPK",
                    startTime = LocalDateTime.parse("2022-05-07T06:25:13"),
                    processType = ProcessType.PICKING,
                    skills = emptyList()
                ),

                )
        )

        Assertions.assertDoesNotThrow { assignZoneService.assignUserOnZone() }
    }
}
