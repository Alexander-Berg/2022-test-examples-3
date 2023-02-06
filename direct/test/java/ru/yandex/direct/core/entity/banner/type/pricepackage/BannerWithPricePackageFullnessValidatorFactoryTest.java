package ru.yandex.direct.core.entity.banner.type.pricepackage;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithPricePackageFullnessValidatorFactoryTest extends BannerWithPricePackageFullnessTestBase {

    @Test
    public void createForUpdate_notActiveBanner() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);

        Creative creative = createCreativeWithFormat(728L, 90L);
        OldCpmBanner banner = activeCpmBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(), creative.getId())
                .withStatusShow(false);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner, defaultAdGroup);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner.getId());
        prepareAndApplyValid(changes);
    }

    @Test
    public void createForUpdate_activeAndNotActiveBanner() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);

        Creative creative = createCreativeWithFormat(728L, 90L);
        OldCpmBanner activeBanner = activeCpmBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                creative.getId());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), activeBanner, defaultAdGroup);
        OldCpmBanner notActiveBanner = activeCpmBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                creative.getId()).withStatusShow(false);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), notActiveBanner, defaultAdGroup);

        List<ModelChanges<CpmBanner>> changes = List.of(
                modelChangesTriggeringRemoderation(activeBanner.getId()),
                modelChangesTriggeringRemoderation(notActiveBanner.getId())
        );
        MassResult<Long> result = createOperation(changes).prepareAndApply();
        assertFullnessViolatedDefect(result.get(0).getValidationResult());
        assertNoDefect(result.get(1).getValidationResult());
    }

    @Test
    public void createForUpdate_notChangedModeration() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner = createBannerWithCreativeFormat(defaultAdGroup, 728L, 90L);

        ModelChanges<CpmBanner> changes = modelChangesNotTriggeringRemoderation(banner.getId());
        prepareAndApplyValid(changes);
    }

}
