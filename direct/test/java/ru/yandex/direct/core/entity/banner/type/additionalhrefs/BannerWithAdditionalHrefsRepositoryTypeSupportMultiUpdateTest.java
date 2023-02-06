package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_1;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_2;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_3;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_4;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.toNewBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdditionalHrefsRepositoryTypeSupportMultiUpdateTest extends
        BannerClientInfoUpdateOperationTestBase {

    private OldCpmBanner banner1;
    private OldCpmBanner banner2;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        var adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        var creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        enableFeatureAdditionalHrefs(clientInfo);

        banner1 = activeCpmBanner(campaign.getId(), adGroup.getId(), creativeInfo.getCreativeId())
                .withAdditionalHrefs(emptyList());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner1, adGroup);

        banner2 = activeCpmBanner(campaign.getId(), adGroup.getId(), creativeInfo.getCreativeId())
                .withAdditionalHrefs(singletonList(new OldBannerAdditionalHref().withHref(HREF_1)));
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner2, adGroup);
    }

    @Test
    public void setHrefsToOneBannerAndClearHrefsInSecondBanner() {
        List<BannerAdditionalHref> additionalHrefs1 = toNewBannerAdditionalHrefs(asList(HREF_2, HREF_3));

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(banner1.getId(),
                CpmBanner.class, CpmBanner.ADDITIONAL_HREFS, additionalHrefs1);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(banner2.getId(),
                CpmBanner.class, CpmBanner.ADDITIONAL_HREFS, emptyList());

        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        CpmBanner actualBanner1 = getBanner(banner1.getId());
        CpmBanner actualBanner2 = getBanner(banner2.getId());

        assertThat(actualBanner1.getAdditionalHrefs()).isEqualTo(additionalHrefs1);
        assertThat(actualBanner2.getAdditionalHrefs()).isEmpty();
    }

    @Test
    public void changeHrefsInTwoBanners() {
        List<BannerAdditionalHref> additionalHrefs1 = toNewBannerAdditionalHrefs(asList(HREF_2, HREF_3));
        List<BannerAdditionalHref> additionalHrefs2 = toNewBannerAdditionalHrefs(asList(HREF_4, HREF_1));

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(banner1.getId(),
                CpmBanner.class, CpmBanner.ADDITIONAL_HREFS, additionalHrefs1);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(banner2.getId(),
                CpmBanner.class, CpmBanner.ADDITIONAL_HREFS, additionalHrefs2);

        prepareAndApplyValid(asList(
                modelChanges1.castModelUp(BannerWithSystemFields.class),
                modelChanges2.castModelUp(BannerWithSystemFields.class)));

        CpmBanner actualBanner1 = getBanner(banner1.getId());
        CpmBanner actualBanner2 = getBanner(banner2.getId());

        assertThat(actualBanner1.getAdditionalHrefs()).isEqualTo(additionalHrefs1);
        assertThat(actualBanner2.getAdditionalHrefs()).isEqualTo(additionalHrefs2);
    }

    private void enableFeatureAdditionalHrefs(ClientInfo clientInfo) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
    }
}
