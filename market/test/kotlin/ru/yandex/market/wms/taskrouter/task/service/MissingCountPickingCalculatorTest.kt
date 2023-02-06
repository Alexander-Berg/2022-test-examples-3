package ru.yandex.market.wms.taskrouter.task.service

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.wms.common.model.enums.ProcessType
import ru.yandex.market.wms.taskrouter.task.dao.PickingTaskDao
import ru.yandex.market.wms.taskrouter.task.model.CountPeopleOnZone
import ru.yandex.market.wms.taskrouter.task.model.CountTaskOnZone
import ru.yandex.market.wms.taskrouter.task.model.NormPerformance

@ExtendWith(SpringExtension::class)
@ContextConfiguration(
    classes = [
        MissingCountPickingCalculator::class
    ]
)
internal class MissingCountPickingCalculatorTest {

    @MockBean
    lateinit var taskDao: PickingTaskDao

    @Autowired
    lateinit var missingCountPickingCalculator: MissingCountPickingCalculator

    private val normPerformanceByFloor = listOf(
        NormPerformance(
            zoneId = "MEZONIN1",
            performance = 110,
            processType = ProcessType.PICKING
        ),
        NormPerformance(
            zoneId = "MEZONIN2",
            performance = 120,
            processType = ProcessType.PICKING
        ),
        NormPerformance(
            zoneId = "MEZONIN3",
            performance = 130,
            processType = ProcessType.PICKING
        ),
        NormPerformance(
            zoneId = "MEZONIN4",
            performance = 140,
            processType = ProcessType.PICKING
        )
    )

    private val backLogTaskCount = listOf(
        CountTaskOnZone(
            zoneId = "MEZONIN1",
            processType = ProcessType.PICKING,
            count = 2000
        ),
        CountTaskOnZone(
            zoneId = "MEZONIN2",
            processType = ProcessType.PICKING,
            count = 3000
        ),
        CountTaskOnZone(
            zoneId = "MEZONIN3",
            processType = ProcessType.PICKING,
            count = 4000
        ),
        CountTaskOnZone(
            zoneId = "MEZONIN4",
            processType = ProcessType.PICKING,
            count = 5000
        )
    )

    private val countUserOnZone: Collection<CountPeopleOnZone> = listOf(
        CountPeopleOnZone(
            zoneId = "MEZONIN1",
            processType = ProcessType.PICKING,
            count = 100
        ),
        CountPeopleOnZone(
            zoneId = "MEZONIN2",
            processType = ProcessType.PICKING,
            count = 200
        ),
        CountPeopleOnZone(
            zoneId = "MEZONIN3",
            processType = ProcessType.PICKING,
            count = 300
        ),
        CountPeopleOnZone(
            zoneId = "MEZONIN4",
            processType = ProcessType.PICKING,
            count = 400
        )
    )

    @Test
        /*
        * см https://st.yandex-team.ru/MARKETWMS-15189
        * */
    fun calculate() {

        val normPerformanceByFloorMap = normPerformanceByFloor.associate { it.zoneId to it.performance }

        Mockito.`when`(taskDao.backLogCount()).thenReturn(backLogTaskCount)

        val result = missingCountPickingCalculator.calculate(normPerformanceByFloorMap, countUserOnZone)

        val expected = arrayOf(
            CountPeopleOnZone(
                zoneId = "MEZONIN1",
                processType = ProcessType.PICKING,
                count = 166
            ),
            CountPeopleOnZone(
                zoneId = "MEZONIN2",
                processType = ProcessType.PICKING,
                count = 228
            ),
            CountPeopleOnZone(
                zoneId = "MEZONIN3",
                processType = ProcessType.PICKING,
                count = 280
            ),
            CountPeopleOnZone(
                zoneId = "MEZONIN4",
                processType = ProcessType.PICKING,
                count = 326
            )
        )

        assertAll(
            { assertEquals(4, result.size) },
            {
                MatcherAssert.assertThat(
                    result,
                    Matchers.containsInAnyOrder(expected[0], expected[1], expected[2], expected[3])
                )
            },
        )
    }

    @Test
    fun calculateWhenPeopleDeficit() {

        val normPerformanceTest = listOf(
            NormPerformance(zoneId = "MEZONIN_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_3", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_4", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN2_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN3_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN4_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_1", processType = ProcessType.PICKING, performance = 70),
            NormPerformance(zoneId = "MEZONIN1_2", processType = ProcessType.PICKING, performance = 70),
            NormPerformance(zoneId = "FASHION_1", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "FASHION1_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "FASHION5_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN2_1", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN5_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_5", processType = ProcessType.PICKING, performance = 130),
        )

        val countUserOnZone = listOf(
            CountPeopleOnZone(zoneId = "FASHION_1", count = 0, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "FASHION1_2", count = 0, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_1", count = 4, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_2", count = 1, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_3", count = 3, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_4", count = 2, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_5", count = 2, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN1_2", count = 3, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN2_1", count = 0, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN2_2", count = 3, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN3_2", count = 2, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN4_2", count = 1, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN5_2", count = 0, processType = ProcessType.PICKING),
        )

        val backLogTaskCountTest = listOf(
            CountTaskOnZone(zoneId = "FASHION_1", count = 2, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "FASHION1_2", count = 1, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_1", count = 26, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_2", count = 16, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_3", count = 10, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_4", count = 7, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_5", count = 2, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN1_2", count = 15, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN2_1", count = 5, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN2_2", count = 7, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN3_2", count = 7, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN4_2", count = 7, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN5_2", count = 2, processType = ProcessType.PICKING),
        )

        val normPerformanceByFloorMap = normPerformanceTest.associate { it.zoneId to it.performance }

        Mockito.`when`(taskDao.backLogCount()).thenReturn(backLogTaskCountTest)

        val result = missingCountPickingCalculator.calculate(normPerformanceByFloorMap, countUserOnZone)

        val expected = arrayOf(
            CountPeopleOnZone(zoneId = "FASHION_1", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "FASHION1_2", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN_1", processType = ProcessType.PICKING, count = 6),
            CountPeopleOnZone(zoneId = "MEZONIN_2", processType = ProcessType.PICKING, count = 2),
            CountPeopleOnZone(zoneId = "MEZONIN_3", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN_4", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN_5", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN1_2", processType = ProcessType.PICKING, count = 3),
            CountPeopleOnZone(zoneId = "MEZONIN2_1", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN2_2", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN3_2", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN4_2", processType = ProcessType.PICKING, count = 1),
            CountPeopleOnZone(zoneId = "MEZONIN5_2", processType = ProcessType.PICKING, count = 1)
        )

        assertAll(
            { assertEquals(expected.size, result.size) },
            {
                MatcherAssert.assertThat(
                    result,
                    Matchers.containsInAnyOrder(
                        expected[0], expected[1], expected[2], expected[3], expected[4],
                        expected[5], expected[6], expected[7], expected[8], expected[9],
                        expected[10], expected[11], expected[12]
                    )
                )
            },
        )
    }


    @Test
    fun calculateWhenPeopleProficit() {

        val normPerformanceTest = listOf(
            NormPerformance(zoneId = "MEZONIN_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_3", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_4", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN2_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN3_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN4_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_1", processType = ProcessType.PICKING, performance = 70),
            NormPerformance(zoneId = "MEZONIN1_2", processType = ProcessType.PICKING, performance = 70),
            NormPerformance(zoneId = "FASHION_1", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "FASHION1_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "FASHION5_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN2_1", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN5_2", processType = ProcessType.PICKING, performance = 130),
            NormPerformance(zoneId = "MEZONIN_5", processType = ProcessType.PICKING, performance = 130),
        )

        val countUserOnZone = listOf(
            CountPeopleOnZone(zoneId = "FASHION_1", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "FASHION1_2", count = 20000, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_1", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_2", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_3", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_4", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN_5", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN1_2", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN2_1", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN2_2", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN3_2", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN4_2", count = 10, processType = ProcessType.PICKING),
            CountPeopleOnZone(zoneId = "MEZONIN5_2", count = 10, processType = ProcessType.PICKING),
        )

        val backLogTaskCountTest = listOf(
            CountTaskOnZone(zoneId = "FASHION_1", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "FASHION1_2", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_1", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_2", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_3", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_4", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN_5", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN1_2", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN2_1", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN2_2", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN3_2", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN4_2", count = 20, processType = ProcessType.PICKING),
            CountTaskOnZone(zoneId = "MEZONIN5_2", count = 20, processType = ProcessType.PICKING),
        )

        val normPerformanceByFloorMap = normPerformanceTest.associate { it.zoneId to it.performance }

        Mockito.`when`(taskDao.backLogCount()).thenReturn(backLogTaskCountTest)

        val result = missingCountPickingCalculator.calculate(normPerformanceByFloorMap, countUserOnZone)

        val expected = arrayOf(
            CountPeopleOnZone(zoneId = "FASHION_1", processType = ProcessType.PICKING, count = 1360),
            CountPeopleOnZone(zoneId = "FASHION1_2", processType = ProcessType.PICKING, count = 1360),
            CountPeopleOnZone(zoneId = "MEZONIN_1", processType = ProcessType.PICKING, count = 2583),
            CountPeopleOnZone(zoneId = "MEZONIN_2", processType = ProcessType.PICKING, count = 1360),
            CountPeopleOnZone(zoneId = "MEZONIN_3", processType = ProcessType.PICKING, count = 1360),
            CountPeopleOnZone(zoneId = "MEZONIN_4", processType = ProcessType.PICKING, count = 1360),
            CountPeopleOnZone(zoneId = "MEZONIN_5", processType = ProcessType.PICKING, count = 1359),
            CountPeopleOnZone(zoneId = "MEZONIN1_2", processType = ProcessType.PICKING, count = 2583),
            CountPeopleOnZone(zoneId = "MEZONIN2_1", processType = ProcessType.PICKING, count = 1359),
            CountPeopleOnZone(zoneId = "MEZONIN2_2", processType = ProcessType.PICKING, count = 1359),
            CountPeopleOnZone(zoneId = "MEZONIN3_2", processType = ProcessType.PICKING, count = 1359),
            CountPeopleOnZone(zoneId = "MEZONIN4_2", processType = ProcessType.PICKING, count = 1359),
            CountPeopleOnZone(zoneId = "MEZONIN5_2", processType = ProcessType.PICKING, count = 1359)
        )

        assertAll(
            { assertEquals(expected.size, result.size) },
            {
                MatcherAssert.assertThat(
                    result,
                    Matchers.containsInAnyOrder(
                        expected[0], expected[1], expected[2], expected[3], expected[4],
                        expected[5], expected[6], expected[7], expected[8], expected[9],
                        expected[10], expected[11], expected[12]
                    )
                )
            },
        )
    }
}
