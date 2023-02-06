package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithAdditionalHrefs.ADDITIONAL_HREFS;
import static ru.yandex.direct.core.testing.data.TestNewBanners.clientBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.isEmptyCollection;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdditionalHrefsValidatorProviderTest extends BannerClientInfoAddOperationTestBase {

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        setFeatureAdditionalHrefs(clientInfo, true);
    }

    @Test
    public void nonCpmPriceCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(campaignInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(clientInfo);

        CpmBanner banner = clientBannerWithAdditionalHrefs(adGroupInfo, creativeInfo);
        ValidationResult<?, Defect> result = prepareAndApplyInvalid(banner);
        assertDefectAdditionalHrefsNotAllowed(result);
    }

    @Test
    public void additionalHrefsFeatureDisabled() {
        setFeatureAdditionalHrefs(clientInfo, false);
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);

        CpmBanner banner = clientBannerWithAdditionalHrefs(adGroup, creativeInfo);
        ValidationResult<?, Defect> result = prepareAndApplyInvalid(banner);
        assertDefectAdditionalHrefsNotAllowed(result);
    }

    private void setFeatureAdditionalHrefs(ClientInfo clientInfo, boolean enabled) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, enabled);
    }

    private CpmBanner clientBannerWithAdditionalHrefs(AdGroupInfo adGroupInfo, CreativeInfo creativeInfo) {
        return clientBannerWithAdditionalHrefs(adGroupInfo.getAdGroup(), creativeInfo);
    }

    private CpmBanner clientBannerWithAdditionalHrefs(AdGroup adGroup, CreativeInfo creativeInfo) {
        return clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(clientBannerAdditionalHrefs());
    }

    private void assertDefectAdditionalHrefsNotAllowed(ValidationResult<?, Defect> result) {
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(ADDITIONAL_HREFS)),
                isEmptyCollection()))));
    }
}
