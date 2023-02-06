package ru.yandex.direct.core.entity.uac

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.banner.model.ButtonAction
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacButtonAction
import ru.yandex.direct.test.utils.checkEquals

@RunWith(JUnitParamsRunner::class)
class UacCpmAssetButtonEnumButtonTest {
    fun testCampaignActionData() = ButtonAction.values()

    @Test
    @Parameters(method = "testCampaignActionData")
    @TestCaseName("check conversion for campaign action= {0}")
    fun testActionsConversion(action: ButtonAction) {
        UacButtonAction.fromName(action.name).name.checkEquals(action.name)
    }
}
