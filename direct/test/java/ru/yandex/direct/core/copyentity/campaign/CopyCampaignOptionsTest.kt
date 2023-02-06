package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns.fullCpmBannerCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns.fullCpmYndxFrontpageCampaign
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignOptionsTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithTitleSubstitutionForbiddenAndEnabled`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withHasTitleSubstitution(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.hasTitleSubstitution).isTrue
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithSimplifiedStrategyViewForbidden`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withIsSimplifiedStrategyViewEnabled(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.isSimplifiedStrategyViewEnabled).isNull()
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithOrderPhraseLengthPrecedenceForbidden`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withIsOrderPhraseLengthPrecedenceEnabled(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.isSimplifiedStrategyViewEnabled).isNull()
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithOptionalS2sTrackingForbidden`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withIsS2sTrackingEnabled(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.isS2sTrackingEnabled).isNull()
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaForbidden`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withIsMeaningfulGoalsValuesFromMetrikaEnabled(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.isMeaningfulGoalsValuesFromMetrikaEnabled).isNull()
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithForbiddenCheckPositionEvent`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withEnableCheckPositionEvent(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.enableCheckPositionEvent).isFalse
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithEnableCpcHoldForbidden`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withEnableCpcHold(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.enableCpcHold).isFalse
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithEnableCompanyInfoForbiddenAndEnabled`(value: Boolean?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, fullCpmBannerCampaign()
            .withEnableCompanyInfo(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThatKt(copiedCampaign.enableCompanyInfo).isTrue
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithEnableCompanyInfoForbiddenAndDisabled`(value: Boolean?) {
        val campaign = steps.cpmYndxFrontPageSteps().createCampaign(client, fullCpmYndxFrontpageCampaign()
                .withEnableCompanyInfo(value))
        val copiedCampaign = copyValidCampaign(campaign)
        // CampaignWithEnableCompanyInfoForbiddenAndDisabledAddOperationSupport
        assertThat(copiedCampaign.enableCompanyInfo).isFalse
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithExtendedGeoTargetingForbidden`(value: Boolean?) {
        val campaign = steps.cpmYndxFrontPageSteps().createCampaign(client, fullCpmYndxFrontpageCampaign()
                .withHasExtendedGeoTargeting(value))
        val copiedCampaign = copyValidCampaign(campaign)
        // CampaignWithExtendedGeoTargetingForbiddenAddOperationSupport
        assertThat(copiedCampaign.hasExtendedGeoTargeting).isFalse
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy CampaignWithAllowedOnAdultContentForbidden`(value: Boolean?) {
        val campaign = steps.cpmYndxFrontPageSteps().createCampaign(client, fullCpmYndxFrontpageCampaign()
                .withIsAllowedOnAdultContent(value))
        val copiedCampaign = copyValidCampaign(campaign)
        // CampaignWithAllowedOnAdultContentForbidden
        assertThatKt(copiedCampaign.isAllowedOnAdultContent).isNull()
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy requireFiltrationByDontShowDomains without feature`(value: Boolean?) {
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false)
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_IN_CPM, false)
        val campaign = steps.textCampaignSteps()
            .createCampaign(client, fullTextCampaign().withRequireFiltrationByDontShowDomains(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.requireFiltrationByDontShowDomains).isFalse
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy text campaign with requireFiltrationByDontShowDomains with cpm campaign type feature`(value: Boolean?) {
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false)
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_IN_CPM, true)
        val campaign = steps.textCampaignSteps()
            .createCampaign(client, fullTextCampaign().withRequireFiltrationByDontShowDomains(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.requireFiltrationByDontShowDomains).isFalse
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy requireFiltrationByDontShowDomains with all campaign types feature`(value: Boolean?) {
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true)
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_IN_CPM, false)
        val campaign = steps.cpmBannerCampaignSteps()
            .createCampaign(client, fullCpmBannerCampaign().withRequireFiltrationByDontShowDomains(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.requireFiltrationByDontShowDomains).isEqualTo(value)
    }

    @Test
    @Parameters("null", "true", "false")
    fun `copy cpm campaign requireFiltrationByDontShowDomains with cpm campaign type feature`(value: Boolean?) {
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false)
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_IN_CPM, true)
        val campaign = steps.cpmBannerCampaignSteps()
            .createCampaign(client, fullCpmBannerCampaign().withRequireFiltrationByDontShowDomains(value))
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.requireFiltrationByDontShowDomains).isEqualTo(value)
    }
}
