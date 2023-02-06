package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperation;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.forbiddenToChange;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class OutdoorBannerUpdateValidationTest {
    @Autowired
    public Steps steps;
    @Autowired
    public BannersUpdateOperationFactory operationFactory;
    @Autowired
    public FeatureService featureService;

    private AdGroupInfo adGroupInfo;

    @Before
    public void setUp() {
        adGroupInfo = steps.adGroupSteps().createDefaultCpmOutdoorAdGroup();
    }

    @Test
    public void draftOutdoorBannerCanBeChanged() {
        var bannerId = createBanner(OldBannerStatusModerate.NEW);
        var modelChanges = new ModelChanges<>(bannerId, CpmOutdoorBanner.class)
                .process("http://site.com", CpmOutdoorBanner.HREF);

        Optional<MassResult<Long>> result = createUpdateOperation(modelChanges).prepare();

        assertTrue(result.isEmpty());
    }

    @Test
    public void nonDraftOutdoorBannerShouldNotBeChanged() {
        var bannerId = createBanner(OldBannerStatusModerate.YES);
        var modelChanges = new ModelChanges<>(bannerId, CpmOutdoorBanner.class)
                .process("http://site.com", CpmOutdoorBanner.HREF);

        Optional<MassResult<Long>> result = createUpdateOperation(modelChanges).prepare();

        var expectedError = validationError(path(index(0)), forbiddenToChange());
        assertThat(result.get().getValidationResult(), hasDefectDefinitionWith(expectedError));
    }

    @Test
    public void emptyModelChangesIsValid() {
        var bannerId = createBanner(OldBannerStatusModerate.YES);
        var modelChanges = new ModelChanges<>(bannerId, CpmOutdoorBanner.class);

        Optional<MassResult<Long>> result = createUpdateOperation(modelChanges).prepare();

        assertTrue(result.isEmpty());
    }

    private BannersUpdateOperation<BannerWithSystemFields> createUpdateOperation(
            ModelChanges<CpmOutdoorBanner> modelChanges) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(adGroupInfo.getClientId());

        return operationFactory.createUpdateOperation(Applicability.FULL, false, ModerationMode.DEFAULT,
                singletonList(modelChanges.castModelUp(BannerWithSystemFields.class)),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), clientEnabledFeatures, false);
    }

    private Long createBanner(OldBannerStatusModerate statusModerate) {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        var banner = activeCpmOutdoorBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId);

        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(adGroupInfo.getClientInfo(), creativeId);
        var bannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(
                banner.withStatusModerate(statusModerate),
                adGroupInfo);
        return bannerInfo.getBannerId();
    }
}
