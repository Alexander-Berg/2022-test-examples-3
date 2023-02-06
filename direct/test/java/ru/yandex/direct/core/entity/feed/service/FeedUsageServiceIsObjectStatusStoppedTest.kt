package ru.yandex.direct.core.entity.feed.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum
import java.util.Optional

@RunWith(JUnitParamsRunner::class)
class FeedUsageServiceIsObjectStatusStoppedTest {
    fun parameters() = listOf(
        listOf(null, false),
        listOf(GdSelfStatusEnum.RUN_OK, false),
        listOf(GdSelfStatusEnum.RUN_PROCESSING, false),
        listOf(GdSelfStatusEnum.RUN_WARN, false),
        listOf(GdSelfStatusEnum.PAUSE_OK, false),
        listOf(GdSelfStatusEnum.PAUSE_WARN, false),
        listOf(GdSelfStatusEnum.PAUSE_CRIT, false),
        listOf(GdSelfStatusEnum.ON_MODERATION, false),
        listOf(GdSelfStatusEnum.STOP_OK, true),
        listOf(GdSelfStatusEnum.STOP_PROCESSING, false),
        listOf(GdSelfStatusEnum.STOP_WARN, true),
        listOf(GdSelfStatusEnum.STOP_CRIT, true),
        listOf(GdSelfStatusEnum.DRAFT, true),
        listOf(GdSelfStatusEnum.ARCHIVED, true),
    )

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("Check is group or campaign with self status {0} counts as stopped for feed usage calculation: {1}")
    fun testGetHrefWithTrackingParams(
        selfStatus: GdSelfStatusEnum?,
        expected: Boolean
    ) {
        assertThat(FeedUsageService.isObjectStatusStopped(Optional.ofNullable(selfStatus))).isEqualTo(expected)
    }
}












