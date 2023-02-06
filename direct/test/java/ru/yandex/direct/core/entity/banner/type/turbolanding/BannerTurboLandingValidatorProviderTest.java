package ru.yandex.direct.core.entity.banner.type.turbolanding;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding.TURBO_LANDING_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndTurbolandingType;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerTurboLandingValidatorProviderTest {
    @Autowired
    public BannerWithTurboLandingValidatorProvider provider;

    @Autowired
    public Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void validVideoTurboLandingForTextAdGroup() {
        var adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientCpmBanner(null)
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        var vr = validate(container(adGroupInfo.getAdGroup(), banner), banner);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void invalidTurboLandingForCpmBannerInCpmGeoProductAdGroup() {
        var adGroupInfo = steps.adGroupSteps().createActiveCpmGeoproductAdGroup(clientInfo);
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientCpmBanner(null)
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        var vr = validate(container(adGroupInfo.getAdGroup(), banner), banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(TURBO_LANDING_ID)),
                inconsistentStateBannerTypeAndTurbolandingType())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void nullTurboLandingForCpmBannerInGeoProductAdGroup() {
        var adGroupInfo = steps.adGroupSteps().createActiveCpmGeoproductAdGroup(clientInfo);
        var banner = clientCpmBanner(null)
                .withTurboLandingId(null)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        var vr = validate(container(adGroupInfo.getAdGroup(), banner), banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(TURBO_LANDING_ID)),
                notNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private ValidationResult<List<BannerWithTurboLanding>, Defect> validate(
            BannersAddOperationContainer container, BannerWithTurboLanding banner) {
        return ListValidationBuilder.<BannerWithTurboLanding, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithTurboLandingValidator(container, singletonList(banner)))
                .getResult();
    }

    private BannersAddOperationContainer container(AdGroupForBannerOperation adGroup, BannerWithTurboLanding banner) {
        return newBannerValidationContainer()
                .withClientInfo(clientInfo)
                .withIndexToAdGroupForOperationMap(Map.of(0, adGroup))
                .withBannerToIndexMap(Map.of(banner, 0))
                .build();
    }
}
