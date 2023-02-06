package ru.yandex.direct.core.copyentity.adgroup

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyAdGroupStatusAutobudgetShowTest : BaseCopyAdGroupTest() {
    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    private fun statusAutobudgetShows() = listOf(
        true,
        false,
    )

    @Test
    @Parameters(method = "statusAutobudgetShows")
    fun copyAdGroupWithAllAutobudgetStatusesShow(statusAutobudgetShows: Boolean?) {
        val adGroup = steps.adGroupSteps().createAdGroup(
            AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(
                TestGroups.activeTextAdGroup(null)
                .withStatusAutobudgetShow(statusAutobudgetShows)))

        val copiedAdGroup: TextAdGroup = copyValidAdGroup(adGroup)

        assertThatKt(copiedAdGroup.statusAutobudgetShow).isTrue
    }
}
