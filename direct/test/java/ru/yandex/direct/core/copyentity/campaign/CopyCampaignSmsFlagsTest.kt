package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.SmsFlag
import ru.yandex.direct.core.entity.time.model.TimeInterval
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns.fullMobileContentCampaign
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import java.util.EnumSet

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignSmsFlagsTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun testCopyNotificationSettings() {
        val mobileApp = steps.mobileAppSteps().createDefaultMobileApp(client)
        val campaign =
            steps.mobileContentCampaignSteps().createCampaign(client, fullMobileContentCampaign(mobileApp.mobileAppId)
                .withSmsTime(TimeInterval()
                    .withStartHour(1).withStartMinute(15)
                    .withEndHour(24).withEndMinute(0))
                .withSmsFlags(EnumSet.of(
                    SmsFlag.CAMP_FINISHED_SMS,
                    SmsFlag.MODERATE_RESULT_SMS,
                )))

        val copiedCampaign: MobileContentCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.smsTime).isEqualTo(campaign.typedCampaign.smsTime)
            assertThat(copiedCampaign.smsFlags).isEqualTo(campaign.typedCampaign.smsFlags)
        }
    }

    @Test
    fun testCopyNotificationSiteMonitoringSettings() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withSmsTime(TimeInterval()
                .withStartHour(1).withStartMinute(15)
                .withEndHour(23).withEndMinute(45))
            .withSmsFlags(EnumSet.of(
                SmsFlag.CAMP_FINISHED_SMS,
                SmsFlag.MODERATE_RESULT_SMS,
                SmsFlag.NOTIFY_METRICA_CONTROL_SMS,
            )))

        val copiedCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.smsTime).isEqualTo(campaign.typedCampaign.smsTime)
            assertThat(copiedCampaign.smsFlags).isEqualTo(campaign.typedCampaign.smsFlags)
        }
    }

}
