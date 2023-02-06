package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref.HREF;
import static ru.yandex.direct.core.entity.banner.model.BannerWithAdditionalHrefs.ADDITIONAL_HREFS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdditionalHrefsAddNegativeTest extends BannerClientInfoAddOperationTestBase {

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        enableFeatureAdditionalHrefs(clientInfo);
    }

    @Test
    public void invalidAdditionalHref() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(List.of(new BannerAdditionalHref().withHref("invalid href")));

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(banner);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(ADDITIONAL_HREFS), index(0), field(HREF)),
                invalidHref()))));
    }

    private void enableFeatureAdditionalHrefs(ClientInfo clientInfo) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
    }
}
