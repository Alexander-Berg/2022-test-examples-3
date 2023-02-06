package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdditionalHrefs;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewBanners.clientBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdditionalHrefsAddPositiveTest extends BannerClientInfoAddOperationTestBase {

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        enableFeatureAdditionalHrefs(clientInfo);
    }

    @Test
    public void addCpmBanner_CmpPriceCampaign() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);

        List<BannerAdditionalHref> bannerAdditionalHrefs = clientBannerAdditionalHrefs();
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(bannerAdditionalHrefs);
        Long bannerId = prepareAndApplyValid(banner);

        BannerWithAdditionalHrefs actualBanner = getBanner(bannerId, BannerWithAdditionalHrefs.class);
        assertThat(actualBanner.getAdditionalHrefs()).isEqualTo(bannerAdditionalHrefs);
    }

    @Test
    public void addCpmBanner_NonCmpPriceCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withAdditionalHrefs(null);
        Long bannerId = prepareAndApplyValid(banner);

        BannerWithAdditionalHrefs actualBanner = getBanner(bannerId, BannerWithAdditionalHrefs.class);
        assertThat(actualBanner.getAdditionalHrefs()).isEqualTo(emptyList());
    }

    private void enableFeatureAdditionalHrefs(ClientInfo clientInfo) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
    }
}
