package ru.yandex.market.wms.core.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.wms.common.model.enums.UserActivityStatus
import ru.yandex.market.wms.common.spring.service.NamedCounterService
import ru.yandex.market.wms.core.base.dto.UserActivityDto
import ru.yandex.market.wms.core.entity.UserActivity
import ru.yandex.market.wms.core.entity.UserActivityKey
import ru.yandex.market.wms.core.service.impl.IndirectActivityQueueProducer
import java.time.LocalDateTime

internal class DurationSplitterTest {

    private val counterService: NamedCounterService = Mockito.mock(NamedCounterService::class.java)

    private val userActivityService: UserActivityService = Mockito.mock(UserActivityService::class.java)
    private val indirectActivityQueueProducer: IndirectActivityQueueProducer =
        Mockito.mock(IndirectActivityQueueProducer::class.java)

    private val durationSplitter: DurationSplitter =
        DurationSplitter(
            namedCounterService = counterService,
            userActivityService = userActivityService,
            indirectActivityQueueProducer = indirectActivityQueueProducer
        )

    private val existEvents = listOf(
        UserActivityDto(
            activity = "activity",
            userActivityKey = "userActivityKey",
            user = "test",
            startTime = LocalDateTime.parse("2022-05-06T11:00:00"),
            endTime = LocalDateTime.parse("2022-05-06T14:00:00")
        ),
        UserActivityDto(
            activity = "activity",
            userActivityKey = "userActivityKey",
            user = "test",
            startTime = LocalDateTime.parse("2022-05-06T16:00:00"),
            endTime = LocalDateTime.parse("2022-05-06T18:00:00")
        ),
        UserActivityDto(
            activity = "activity",
            userActivityKey = "userActivityKey",
            user = "test",
            startTime = LocalDateTime.parse("2022-05-06T20:00:00"),
            endTime = LocalDateTime.parse("2022-05-06T22:00:00")
        )
    )

    @BeforeEach
    fun setupMock() {
        Mockito.doNothing().`when`(userActivityService).completeUserActivity(Mockito.anyList(), Mockito.anyString())
        Mockito.`when`(counterService.userActivityKey).thenReturn("")
    }

    @Test
    fun splitWhenExistEventsIsEmpty() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
            status = UserActivityStatus.IN_PROCESS,
            expectedEndTime = LocalDateTime.parse("2022-05-06T20:00:00"),
            endTime = null
        )

        val existEvents = emptyList<UserActivityDto>()

        val currentTime = LocalDateTime.parse("2022-05-06T09:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)

        Assertions.assertThat(result).hasSize(1)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrder(event)
    }

    @Test
    fun splitWhenStartBeforeFirstEvent() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
            status = UserActivityStatus.IN_PROCESS,
            endTime = LocalDateTime.parse("2022-05-06T10:00:00"),
            expectedEndTime = null
        )

        val currentTime = LocalDateTime.parse("2022-05-06T09:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)
        Assertions.assertThat(result).hasSize(1)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrder(event)
    }

    @Test
    fun splitWhenEndAfterLastEvent() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T23:00:00"),
            status = UserActivityStatus.IN_PROCESS,
            expectedEndTime = LocalDateTime.parse("2022-05-06T23:30:00"),
            endTime = null
        )

        val currentTime = LocalDateTime.parse("2022-05-06T09:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)

        Assertions.assertThat(result).hasSize(1)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrder(event)
    }

    @Test
    fun splitWhenCrossOne() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
            status = UserActivityStatus.IN_PROCESS,
            expectedEndTime = LocalDateTime.parse("2022-05-06T15:00:00"),
            endTime = null
        )

        val currentTime = LocalDateTime.parse("2022-05-06T09:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)

        val expected = listOf(
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
                endTime = LocalDateTime.parse("2022-05-06T11:00:00"),
                status = UserActivityStatus.COMPLETED,
                expectedEndTime = null
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T14:00:00"),
                status = UserActivityStatus.COMPLETED,
                endTime = LocalDateTime.parse("2022-05-06T15:00:00"),
                expectedEndTime = null
            )
        )

        Assertions.assertThat(result).hasSize(expected.size)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun splitWhenCrossAll() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
            status = UserActivityStatus.IN_PROCESS,
            expectedEndTime = LocalDateTime.parse("2022-05-06T23:30:00"),
            endTime = null
        )

        val currentTime = LocalDateTime.parse("2022-05-06T09:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)

        val expected = listOf(
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
                status = UserActivityStatus.COMPLETED,
                expectedEndTime = null,
                endTime = LocalDateTime.parse("2022-05-06T11:00:00")
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T14:00:00"),
                status = UserActivityStatus.COMPLETED,
                expectedEndTime = null,
                endTime = LocalDateTime.parse("2022-05-06T16:00:00")
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T18:00:00"),
                status = UserActivityStatus.COMPLETED,
                expectedEndTime = null,
                endTime = LocalDateTime.parse("2022-05-06T20:00:00")
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T22:00:00"),
                status = UserActivityStatus.IN_PROCESS,
                expectedEndTime = LocalDateTime.parse("2022-05-06T23:30:00"),
                endTime = null
            )
        )

        Assertions.assertThat(result).hasSize(expected.size)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun splitWhenCrossAllLeft() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
            status = UserActivityStatus.IN_PROCESS,
            expectedEndTime = LocalDateTime.parse("2022-05-06T21:00:00"),
            endTime = null
        )

        val currentTime = LocalDateTime.parse("2022-05-06T09:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)

        val expected = listOf(
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T08:00:00"),
                status = UserActivityStatus.COMPLETED,
                endTime = LocalDateTime.parse("2022-05-06T11:00:00"),
                expectedEndTime = null
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T14:00:00"),
                status = UserActivityStatus.COMPLETED,
                endTime = LocalDateTime.parse("2022-05-06T16:00:00"),
                expectedEndTime = null
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T18:00:00"),
                status = UserActivityStatus.COMPLETED,
                endTime = LocalDateTime.parse("2022-05-06T20:00:00"),
                expectedEndTime = null
            )
        )

        Assertions.assertThat(result)
            .hasSize(expected.size)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun splitWhenCrossAllRight() {
        val event = createActivity(
            startTime = LocalDateTime.parse("2022-05-06T12:00:00"),
            expectedEndTime = LocalDateTime.parse("2022-05-06T23:30:00"),
            status = UserActivityStatus.IN_PROCESS,
            endTime = null
        )

        val currentTime = LocalDateTime.parse("2022-05-06T23:00:00")

        val result = durationSplitter.split(event, existEvents, currentTime)

        val expected = listOf(
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T14:00:00"),
                status = UserActivityStatus.COMPLETED,
                expectedEndTime = null,
                endTime = LocalDateTime.parse("2022-05-06T16:00:00")
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T18:00:00"),
                status = UserActivityStatus.COMPLETED,
                expectedEndTime = null,
                endTime = LocalDateTime.parse("2022-05-06T20:00:00")
            ),
            createActivity(
                startTime = LocalDateTime.parse("2022-05-06T23:00:00"),
                status = UserActivityStatus.IN_PROCESS,
                expectedEndTime = LocalDateTime.parse("2022-05-06T23:30:00"),
                endTime = null
            )
        )

        Assertions.assertThat(result)
            .hasSize(expected.size)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("userActivityKey")
            .containsExactlyInAnyOrderElementsOf(expected)
    }

    private fun createActivity(
        startTime: LocalDateTime,
        status: UserActivityStatus,
        expectedEndTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): UserActivity = UserActivity(
        userActivityKey = userActivityKey,
        type = "activityType",
        userId = "userId",
        status = status,
        userAttendanceKey = "userAttendanceKey",
        actype = "actype",
        uassignmentNumber = "uassignmentNumber",
        equipment = "equipment",
        equipmentId = "equipmentId",
        paid = "paid",
        startTime = startTime,
        expectedEndTime = expectedEndTime,
        addWho = "test",
        endTime = endTime
    )

    companion object {
        val userActivityKey = UserActivityKey(resource = { "" })
    }
}
