package ru.yandex.direct.bstransport.yt.service.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.direct.bstransport.yt.repository.resources.BannerResourcesYtCommonRepository
import ru.yandex.direct.bstransport.yt.service.YtHashBorders

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaseTableToQueueResyncServiceTest {

    private val repository: BannerResourcesYtCommonRepository = mock(BannerResourcesYtCommonRepository::class.java)
    private val service = BannerResourcesTableToQueueResyncService(repository)

    fun params(): List<Arguments> = listOf(
        arguments(255, 0, 24, YtHashBorders(0, 10)),
        arguments(255, 20, 21, YtHashBorders(-1, -1, true)),
        arguments(255, 19, 21, YtHashBorders(247, 255, false)),
        arguments(255, 23, 24, YtHashBorders(253, 255)),
        arguments(20, 11, 12, YtHashBorders(-1, -1, true)),
        arguments(20, 1, 12, YtHashBorders(2, 3, false)),
        arguments(0, 0, 12, YtHashBorders(0, 0, false)),
    )

    @ParameterizedTest
    @MethodSource("params")
    fun testGetYtHashBorders(ytHashMaxSize: Long, bucketNumber: Int, bucketsCnt: Int, expectedBorders: YtHashBorders) {
        `when`(repository.getYtHashMaxValue()).thenReturn(ytHashMaxSize)
        val borders = service.getYtHashBorders(bucketNumber, bucketsCnt)
        assertThat(borders).isEqualToComparingFieldByFieldRecursively(expectedBorders)
    }
}
