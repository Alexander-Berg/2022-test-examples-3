package ru.yandex.direct.core.entity.campaign.converter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class CampaignConverterOptsTest {

    @Test
    public void campaignOptsToDb_TrueValues() {
        CommonCampaign campaign = createCampaignWithOpts(true);
        String opts = CampaignConverter.campaignOptsToDb(campaign);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(opts).doesNotContain("no_title_substitute");
            softly.assertThat(opts).doesNotContain("no_extended_geotargeting");
            softly.assertThat(opts).contains("use_current_region");
            softly.assertThat(opts).contains("use_regular_region");
            softly.assertThat(opts).contains("enable_cpc_hold");
            softly.assertThat(opts).doesNotContain("hide_permalink_info");
            softly.assertThat(opts).contains("is_alone_trafaret_allowed");
            softly.assertThat(opts).contains("has_turbo_smarts");
            softly.assertThat(opts).contains("is_touch");
            softly.assertThat(opts).contains("has_turbo_app");
            softly.assertThat(opts).contains("is_order_phrase_length_precedence_enabled");
            softly.assertThat(opts).contains("is_new_ios_version_enabled");
            softly.assertThat(opts).contains("is_skadnetwork_enabled");
            softly.assertThat(opts).contains("is_allowed_on_adult_content");
            softly.assertThat(opts).contains("is_brand_lift_hidden");
        });
    }

    @Test
    public void campaignOptsToDb_FalseValues() {
        CommonCampaign campaign = createCampaignWithOpts(false);
        String opts = CampaignConverter.campaignOptsToDb(campaign);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(opts).contains("no_title_substitute");
            softly.assertThat(opts).contains("no_extended_geotargeting");
            softly.assertThat(opts).doesNotContain("use_current_region");
            softly.assertThat(opts).doesNotContain("use_regular_region");
            softly.assertThat(opts).doesNotContain("enable_cpc_hold");
            softly.assertThat(opts).contains("hide_permalink_info");
            softly.assertThat(opts).doesNotContain("is_alone_trafaret_allowed");
            softly.assertThat(opts).doesNotContain("has_turbo_smarts");
            softly.assertThat(opts).doesNotContain("is_touch");
            softly.assertThat(opts).doesNotContain("has_turbo_app");
            softly.assertThat(opts).doesNotContain("is_order_phrase_length_precedence_enabled");
            softly.assertThat(opts).doesNotContain("is_new_ios_version_enabled");
            softly.assertThat(opts).doesNotContain("is_skadnetwork_enabled");
            softly.assertThat(opts).doesNotContain("is_allowed_on_adult_content");
            softly.assertThat(opts).doesNotContain("is_brand_lift_hidden");
        });
    }

    @Test
    public void campaignOptsToDb_NullValues() {
        CommonCampaign campaign = createCampaignWithOpts(null);
        String opts = CampaignConverter.campaignOptsToDb(campaign);
        assertThat(opts).isEqualTo("");
    }

    private CommonCampaign createCampaignWithOpts(@Nullable Boolean status) {
        return new TextCampaign()
                .withHasTitleSubstitution(status)
                .withHasExtendedGeoTargeting(status)
                .withUseCurrentRegion(status)
                .withUseRegularRegion(status)
                .withEnableCpcHold(status)
                .withEnableCompanyInfo(status)
                .withIsAloneTrafaretAllowed(status)
                .withHasTurboSmarts(status)
                .withIsTouch(status)
                .withHasTurboApp(status)
                .withIsSimplifiedStrategyViewEnabled(status)
                .withIsOrderPhraseLengthPrecedenceEnabled(status)
                .withIsNewIosVersionEnabled(status)
                .withIsSkadNetworkEnabled(status)
                .withIsAllowedOnAdultContent(status)
                .withIsBrandLiftHidden(status);
    }
}
