package ru.yandex.direct.core.entity.banner.type.pricepackage;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceSalesFullnessViolated;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BannerWithPricePackageFullnessTestBase extends BannerClientInfoUpdateOperationTestBase {

    @Autowired
    protected Steps steps;

    @Autowired
    protected CreativeRepository creativeRepository;

    protected PricePackage pricePackage;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        pricePackage = approvedPricePackage()
                .withClients(List.of(allowedPricePackageClient(clientInfo)))
                .withDateStart(LocalDate.now().minusYears(1))
                .withDateEnd(LocalDate.now().plusYears(1))
                .withTargetingsCustom(emptyTargetingsCustom());
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP))
                .withAllowExpandedDesktopCreative(false);
        steps.pricePackageSteps().createPricePackage(pricePackage);
    }

    protected CpmPriceCampaign activeCpmPriceCampaign() {
        return defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withStartDate(LocalDate.now().minusDays(1))
                .withEndDate(LocalDate.now().plusDays(1))
                .withStatusShow(true)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES);
    }

    protected OldCpmBanner createBannerWithCreativeFormat(AdGroup adGroup, Long width, Long height) {
        Creative creative = createCreativeWithFormat(width, height);
        OldCpmBanner banner = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creative.getId())
                .withHref("http://old.url")
                .withMeasurers(emptyList());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner, adGroup);
        return banner;
    }

    protected Creative createCreativeWithFormat(Long width, Long height) {
        Creative creative = defaultHtml5(clientInfo.getClientId(), steps.creativeSteps().getNextCreativeId())
                .withWidth(width)
                .withHeight(height)
                .withExpandedPreviewUrl(null);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return creative;
    }

    protected ModelChanges<CpmBanner> modelChangesTriggeringRemoderation(Long bannerId) {
        return ModelChanges.build(bannerId, CpmBanner.class, CpmBanner.HREF, "http://new.url");
    }

    protected ModelChanges<CpmBanner> modelChangesNotTriggeringRemoderation(Long bannerId) {
        return ModelChanges.build(bannerId, CpmBanner.class, CpmBanner.MEASURERS,
                List.of(new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.SIZMEK)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false)));
    }

    protected void assertFullnessViolatedDefect(ValidationResult<?, Defect> result) {
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                priceSalesFullnessViolated()))));
    }

    protected void assertNoDefect(ValidationResult<?, Defect> result) {
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

}
