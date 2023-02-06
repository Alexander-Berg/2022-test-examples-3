package ru.yandex.direct.core.entity.banner.type.pricepackage;

import java.time.LocalDate;
import java.util.List;

import jdk.jfr.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestPricePackages.frontpageVideoPackage;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithPricePackageFullnessValidatorTest extends BannerWithPricePackageFullnessTestBase {

    @Test
    public void breakingCorrectness() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner.getId());
        ValidationResult<?, Defect> result = prepareAndApplyInvalid(changes);
        assertFullnessViolatedDefect(result);
    }

    @Test
    public void notBreakingCorrectness() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner1 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);
        OldCpmBanner banner2 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner1.getId());
        prepareAndApplyValid(changes);
    }

    @Test
    @Description("Отсутсвие баннеров по отдельности не нарушает полноту, в совокупности - нарушает")
    public void breakingCorrectnessInTotal() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner1 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);
        OldCpmBanner banner2 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);

        List<ModelChanges<CpmBanner>> changes = List.of(
                modelChangesTriggeringRemoderation(banner1.getId()),
                modelChangesTriggeringRemoderation(banner2.getId())
        );
        MassResult<Long> result = createOperation(changes).prepareAndApply();
        assertFullnessViolatedDefect(result.get(0).getValidationResult());
        assertFullnessViolatedDefect(result.get(1).getValidationResult());
    }

    @Test
    public void bannerInSpecificAdGroup() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner = createBannerWithCreativeFormat(specificAdGroup, 1456L, 180L);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner.getId());
        prepareAndApplyValid(changes);
    }

    @Test
    public void allBannersInDefaultAdGroup() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner1 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);
        OldCpmBanner banner2 = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);

        List<ModelChanges<CpmBanner>> changes = List.of(
                modelChangesTriggeringRemoderation(banner1.getId()),
                modelChangesTriggeringRemoderation(banner2.getId())
        );
        MassResult<Long> result = createOperation(changes).prepareAndApply();
        assertFullnessViolatedDefect(result.get(0).getValidationResult());
        assertFullnessViolatedDefect(result.get(1).getValidationResult());
    }

    @Test
    public void notActiveCampaign() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign()
                        .withStatusShow(false));
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner banner = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner.getId());
        prepareAndApplyValid(changes);
    }

    @Test
    public void notCpmPriceCampaign() {
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(campaign);
        OldCpmBanner banner = createBannerWithCreativeFormat(adGroup.getAdGroup(), 1456L, 180L);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner.getId());
        prepareAndApplyValid(changes);
    }

    @Test
    public void bannerInSpecificAndDefaultAdGroup() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign());
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                clientInfo);
        CpmYndxFrontpageAdGroup specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign,
                clientInfo);
        OldCpmBanner defaultBanner = createBannerWithCreativeFormat(defaultAdGroup, 1456L, 180L);
        OldCpmBanner specificBanner = createBannerWithCreativeFormat(specificAdGroup, 1456L, 180L);

        List<ModelChanges<CpmBanner>> changes = List.of(
                modelChangesTriggeringRemoderation(defaultBanner.getId()),
                modelChangesTriggeringRemoderation(specificBanner.getId())
        );
        MassResult<Long> result = createOperation(changes).prepareAndApply();
        assertFullnessViolatedDefect(result.get(0).getValidationResult());
        assertNoDefect(result.get(1).getValidationResult());
    }

    @Test
    public void videoFrontpageNotBreakingCorrectness() {
        //видео на морде. Два баннера. Один можно остановить, два нельзя
        pricePackage = frontpageVideoPackage(clientInfo)
                .withDateStart(LocalDate.now().minusYears(1))
                .withDateEnd(LocalDate.now().plusYears(1))
                .withTargetingsCustom(emptyTargetingsCustom());
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, activeCpmPriceCampaign());
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        var creativeInfo = steps.creativeSteps().addCpmVideoFrontpageCreative(clientInfo);
        var banner1 = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creativeInfo.getCreativeId());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner1, adGroup);
        var banner2 = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creativeInfo.getCreativeId());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner2, adGroup);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner1.getId());
        prepareAndApplyValid(changes);
    }

    @Test
    public void videoFrontpageBreakingCorrectness() {
        //видео на морде. Один баннер. Останавливать нельзя
        pricePackage = frontpageVideoPackage(clientInfo)
                .withDateStart(LocalDate.now().minusYears(1))
                .withDateEnd(LocalDate.now().plusYears(1))
                .withTargetingsCustom(emptyTargetingsCustom());
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, activeCpmPriceCampaign());
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        var creativeInfo = steps.creativeSteps().addCpmVideoFrontpageCreative(clientInfo);
        var banner1 = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creativeInfo.getCreativeId());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner1, adGroup);

        ModelChanges<CpmBanner> changes = modelChangesTriggeringRemoderation(banner1.getId());
        ValidationResult<?, Defect> result = prepareAndApplyInvalid(changes);
        assertFullnessViolatedDefect(result);
    }
}
