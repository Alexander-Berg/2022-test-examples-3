package ru.yandex.direct.chassis.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.chassis.configuration.ChassisTest
import ru.yandex.direct.chassis.configuration.ChassisTestConfiguration
import java.time.LocalDateTime
import java.time.Month

@ChassisTest
@ExtendWith(SpringExtension::class)
internal class CommitDeployRepositoryTest {

    @Autowired
    lateinit var commitDeployRepository: CommitDeployRepository

    @Test
    fun assignCorrectedDeployTest() {
        var startTime: LocalDateTime = LocalDateTime.of(2020, Month.AUGUST, 6, 12, 30)
        var endTime: LocalDateTime = LocalDateTime.of(2020, Month.AUGUST, 10, 12, 30)
        assertThat(commitDeployRepository).isNotNull()
        assertEquals(
            172800L,
            commitDeployRepository.countOnlyWorkDays(startTime, endTime)
        )
        assertEquals(86400L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 5, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 7, 12, 30)
        assertEquals(172800L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(86400L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 8, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 8, 14, 30)
        assertEquals(0L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(0L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 8, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 9, 12, 30)
        assertEquals(0L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(0L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 5, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 5, 16, 30)
        assertEquals(14400L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(14400L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 6, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 26, 12, 30)
        assertEquals(1209600L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(604800L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 6, 7, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 6, 12, 30)
        assertEquals(18000L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(16200L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 6, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 6, 22, 30)
        assertEquals(36000L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(27000L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))

        startTime = LocalDateTime.of(2020, Month.AUGUST, 6, 12, 30)
        endTime = LocalDateTime.of(2020, Month.AUGUST, 7, 6, 30)
        assertEquals(64800L, commitDeployRepository.countOnlyWorkDays(startTime, endTime))
        assertEquals(27000L, commitDeployRepository.countOnlyDaylightHours(startTime, endTime))
    }

}
