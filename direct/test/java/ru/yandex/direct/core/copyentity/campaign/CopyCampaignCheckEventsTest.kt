package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.rbac.RbacRole

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignCheckEventsTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var userService: UserService

    @Before
    fun before() {
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
    }

    private fun customCheckEvents() = listOf(
        listOf(true, CampaignWarnPlaceInterval._15),
        listOf(true, CampaignWarnPlaceInterval._30),
        listOf(true, CampaignWarnPlaceInterval._60),
        listOf(false, CampaignWarnPlaceInterval._15),
        listOf(false, CampaignWarnPlaceInterval._30),
        listOf(false, CampaignWarnPlaceInterval._60),
    )

    @Test
    @TestCaseName("{method}(enableCheckPositionEvent={0}, campaignWarnPlaceInterval={1})")
    @Parameters(method = "customCheckEvents")
    fun testCustomCheckEvents(
        enableCheckPositionEvent: Boolean,
        campaignWarnPlaceInterval: CampaignWarnPlaceInterval,
    ) {
        val textCampaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withEnableCheckPositionEvent(enableCheckPositionEvent)
                .withCheckPositionIntervalEvent(campaignWarnPlaceInterval)
        )

        val copiedCampaign: TextCampaign = copyValidCampaign(textCampaign)

        softly {
            assertThat(copiedCampaign.enableCheckPositionEvent).isEqualTo(enableCheckPositionEvent)
            assertThat(copiedCampaign.checkPositionIntervalEvent).isEqualTo(campaignWarnPlaceInterval)
        }
    }

    @Test
    fun testForbiddenCheckEvents() {
        val cpmBannerCampaign = steps.cpmBannerCampaignSteps().createDefaultCampaign(client)

        val copiedCampaign: CpmBannerCampaign = copyValidCampaign(cpmBannerCampaign)

        softly {
            assertThat(copiedCampaign.enableCheckPositionEvent).isFalse
            assertThat(copiedCampaign.checkPositionIntervalEvent).isEqualTo(CampaignWarnPlaceInterval._60)
        }
    }

    private fun customEnableCheckEvents() = listOf(
        listOf(false, false, false, false),
        listOf(false, false, true, false),
        listOf(false, true, false, true),
        listOf(false, true, true, false),
        listOf(true, false, false, false),
        listOf(true, false, true, true),
        listOf(true, true, false, true),
        listOf(true, true, true, true),
    )

    @Test
    @TestCaseName(
        "{method}(campaign.enableCheckPositionEvent={0}, targetChiefUser.sendWarn={1}, copyNotificationSettings={2})"
    )
    @Parameters(method = "customEnableCheckEvents")
    fun testEnableCheckPositionEventCopy(
        enableCheckPositionEvent: Boolean,
        sendWarn: Boolean,
        copyNotificationSettings: Boolean,
        expectedEnableCheckPositionEvent: Boolean
    ) {
        val user: User = TestUsers.generateNewUser().withSendWarn(sendWarn)

        targetClient = steps.clientSteps().createDefaultClient(user)

        val chiefUser: User = userService.getUser(targetClient.chiefUserInfo!!.chiefUid)!!

        assertThat(chiefUser.sendWarn)
            .describedAs("Saved target chief user has unexpected 'sendWarn' value")
            .isEqualTo(sendWarn)

        val textCampaign: TextCampaignInfo = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withEnableCheckPositionEvent(enableCheckPositionEvent)
        )

        val copiedCampaign: TextCampaign = copyValidCampaignBetweenClients(
            campaign = textCampaign,
            flags = CopyCampaignFlags(isCopyNotificationSettings = copyNotificationSettings)
        )

        assertThat(copiedCampaign.enableCheckPositionEvent)
            .describedAs("Copied text campaign has wrong 'enableCheckPositionEvent' value")
            .isEqualTo(expectedEnableCheckPositionEvent)
    }
}
