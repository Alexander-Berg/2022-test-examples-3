package ru.yandex.direct.core.copyentity.campaign

import org.junit.Before
import org.junit.Test
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
class CopyCampaignWithBrandLiftTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
    }

    @Test
    fun copyDefaultCpmCampaignWithBrandLift() {
        val brandLift = BrandSurvey()
            .withBrandSurveyId("qwerty123" + client.login)
            .withName("brandSurveyName")
            .withClientId(client.clientId?.asLong())
            .withSegmentId(0L)
            .withExperimentId(0L)
            .withRetargetingConditionId(0L)

        val campaign = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(client, brandLift)
        val operation = sameClientCampaignCopyOperation(campaign)
        val copiedCampaign = copyValidCampaign<CpmBannerCampaign>(operation)

        softly {
            assertThat(copiedCampaign.brandSurveyId).isNull()
            assertThatKt(copiedCampaign.abSegmentRetargetingConditionId).isNull()
            assertThatKt(copiedCampaign.abSegmentStatisticRetargetingConditionId).isNull()
            assertThat(copiedCampaign.sectionIds).isNull()
            assertThat(copiedCampaign.abSegmentGoalIds).isNull()
        }
    }
}
