package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithAdvancedGeoTargetingTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
        steps.featureSteps().addClientFeature(client.clientId, FeatureName.ADVANCED_GEOTARGETING, true)
    }

    private fun advancedGeoTargetingParameters() =
        listOf(
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(false, true, true),
            listOf(true, true, false),
        )

    @Test
    @TestCaseName(
        "{method}" +
            "useCurrentRegion={0}, useRegularRegion={1}, hasExtendedGeoTargeting={2}"
    )
    @Parameters(method = "advancedGeoTargetingParameters")
    fun advancedGeoTargetingParamsCopied(
        useCurrentRegion : Boolean,
        useRegularRegion : Boolean,
        hasExtendedGeoTargeting: Boolean
    ) {
        val campaign = steps.textCampaignSteps().createCampaign(client, TestTextCampaigns.fullTextCampaign()
            .withUseCurrentRegion(useCurrentRegion)
            .withUseRegularRegion(useRegularRegion)
            .withHasExtendedGeoTargeting(hasExtendedGeoTargeting)
        )

        val copiedCampaign = copyValidCampaign(campaign);
        softly {
            this.assertThat(copiedCampaign.useCurrentRegion).isEqualTo(useCurrentRegion)
            this.assertThat(copiedCampaign.useRegularRegion).isEqualTo(useRegularRegion)
            this.assertThat(copiedCampaign.hasExtendedGeoTargeting).isEqualTo(hasExtendedGeoTargeting)
        }
    }
}
