package ru.yandex.direct.oneshot.oneshots

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.oneshot.oneshots.CampaignMigrationBaseOneshot.Companion.Finished
import ru.yandex.direct.oneshot.oneshots.CampaignMigrationBaseOneshot.Companion.LastPosition
import ru.yandex.direct.oneshot.oneshots.CampaignMigrationBaseOneshot.Companion.Position
import ru.yandex.direct.oneshot.oneshots.CampaignMigrationBaseOneshot.Companion.State
import ru.yandex.direct.oneshot.util.GsonUtils
import ru.yandex.direct.test.utils.checkEquals

@RunWith(JUnitParamsRunner::class)
internal class CampaignMigrationBaseOneshotStateTest {

    fun parametrizedTestData() = listOf(
        Finished,
        LastPosition(Position(547787990)),
        LastPosition(Position(1)),
        LastPosition(Position(Long.MAX_VALUE))
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    fun testSerDe(state: State) {
        val json = GsonUtils.GSON.toJson(state)
        GsonUtils.GSON.fromJson(json, State::class.java).checkEquals(state)
    }
}
