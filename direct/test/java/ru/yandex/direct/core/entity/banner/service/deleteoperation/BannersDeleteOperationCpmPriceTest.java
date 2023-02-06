package ru.yandex.direct.core.entity.banner.service.deleteoperation;

import java.util.List;
import java.util.Map;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerAdditionalHrefsRepository;
import ru.yandex.direct.core.entity.banner.service.BannerDeleteOperationFactory;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersDeleteOperationCpmPriceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerDeleteOperationFactory bannerDeleteOperationFactory;

    @Autowired
    private OldBannerAdditionalHrefsRepository bannerAdditionalHrefsRepository;

    private ClientInfo client;
    private CpmPriceCampaign campaign;
    private CpmYndxFrontpageAdGroup adGroup;
    private CreativeInfo creative;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(client.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client);
        adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);
        creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(client, campaign);
    }

    @Test
    @Description("Проверяем, что у баннера, который удаляли, additional_href удаляются. А у баннера, который не " +
            "удаляли, additional_href не удаляются.")
    public void additionalHrefsRemovedWhenBannerRemoved() {
        OldCpmBanner bannerToDelete = activeCpmBanner(campaign.getId(), adGroup.getId(), creative.getCreativeId())
                .withAdditionalHrefs(List.of(new OldBannerAdditionalHref().withHref("http://google.com")))
                .withBsBannerId(null)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withStatusPostModerate(OldBannerStatusPostModerate.NO);
        steps.bannerSteps().createActiveCpmBannerRaw(client.getShard(), bannerToDelete, adGroup);

        OldCpmBanner bannerToKeep = activeCpmBanner(campaign.getId(), adGroup.getId(), creative.getCreativeId())
                .withAdditionalHrefs(List.of(new OldBannerAdditionalHref().withHref("http://yandex.ru")))
                .withBsBannerId(null)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withStatusPostModerate(OldBannerStatusPostModerate.NO);
        steps.bannerSteps().createActiveCpmBannerRaw(client.getShard(), bannerToKeep, adGroup);

        assertThat(bannerDeleteOperationFactory
                .createBannerDeleteOperation(client.getShard(), client.getClientId(), client.getUid(),
                        List.of(bannerToDelete.getId()), Applicability.PARTIAL)
                .prepareAndApply()
                .getValidationResult(), hasNoErrors());

        Map<Long, List<OldBannerAdditionalHref>> bannerIdToAdditionalHrefs =
                bannerAdditionalHrefsRepository.getAdditionalHrefs(client.getShard(), List.of(bannerToDelete.getId(),
                        bannerToKeep.getId()));

        assertThat(bannerIdToAdditionalHrefs.size(), is(1));

        assertThat(bannerIdToAdditionalHrefs.get(bannerToKeep.getId()).get(0).getHref(), is("http://yandex.ru"));
    }

}
