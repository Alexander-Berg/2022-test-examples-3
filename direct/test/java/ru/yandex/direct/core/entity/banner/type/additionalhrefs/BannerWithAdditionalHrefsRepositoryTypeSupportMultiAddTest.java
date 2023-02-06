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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_1;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_2;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_3;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.toNewBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdditionalHrefsRepositoryTypeSupportMultiAddTest extends BannerClientInfoAddOperationTestBase {

    private CpmPriceCampaign campaign;
    private CpmYndxFrontpageAdGroup adGroup;
    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        enableFeatureAdditionalHrefs(clientInfo);
    }

    @Test
    public void oneBannerWithAdditionalHrefsAndOneWithout() {
        List<BannerAdditionalHref> additionalHrefs1 = toNewBannerAdditionalHrefs(asList(HREF_1, HREF_2));

        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(additionalHrefs1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(emptyList());

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getAdditionalHrefs()).isEqualTo(additionalHrefs1);
        assertThat(actualBanner2.getAdditionalHrefs()).isEmpty();
    }

    @Test
    public void severalBannersWithAdditionalHrefs() {
        List<BannerAdditionalHref> additionalHrefs1 = toNewBannerAdditionalHrefs(asList(HREF_1, HREF_2));
        List<BannerAdditionalHref> additionalHrefs2 = toNewBannerAdditionalHrefs(asList(HREF_1, HREF_3));

        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(additionalHrefs1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(additionalHrefs2);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getAdditionalHrefs()).isEqualTo(additionalHrefs1);
        assertThat(actualBanner2.getAdditionalHrefs()).isEqualTo(additionalHrefs2);
    }

    private void enableFeatureAdditionalHrefs(ClientInfo clientInfo) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
    }

}
