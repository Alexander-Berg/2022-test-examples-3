package ru.yandex.direct.web.entity.uac.model

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.grid.model.campaign.GdCampaignAction
import ru.yandex.direct.grid.model.campaign.GdCampaignServicedState
import ru.yandex.direct.test.utils.checkEquals

@RunWith(JUnitParamsRunner::class)
class UacCampaignEnumsConverterTest {

    fun testCampaignActionData() = GdCampaignAction.values()

    @Test
    @Parameters(method = "testCampaignActionData")
    @TestCaseName("check conversion for campaign action= {0}")
    fun testActionsConversion(action: GdCampaignAction) {
        UacCampaignAction.fromGdCampaignAction(action).name.checkEquals(action.name)
    }

    fun testServicedStatesData() = GdCampaignServicedState.values()
    @Test
    @Parameters(method = "testServicedStatesData")
    @TestCaseName("check conversion for campaign serviced state= {0}")
    fun testServicedStatesConversion(state: GdCampaignServicedState) {
        UacCampaignServicedState.fromGdCampaignServicedState(state).name.checkEquals(state.name)
    }
}
