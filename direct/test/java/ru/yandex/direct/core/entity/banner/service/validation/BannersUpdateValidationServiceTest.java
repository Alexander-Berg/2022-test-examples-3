package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperation;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.service.DatabaseMode;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.actionInArchivedCampaign;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adNotFound;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannersUpdateValidationServiceTest {

    @Autowired
    public ShardHelper shardHelper;

    @Autowired
    private BannersUpdateOperationFactory updateOperationFactory;

    @Autowired
    public FeatureService featureService;

    @Autowired
    private Steps steps;

    private TextBannerInfo bannerInfo;

    @Before
    public void before() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
    }

    @Test
    public void errorWhenInvalidBannerId() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId() + 1, TextBanner.class)
                .process("newHref", BannerWithHref.HREF);
        MassResult<Long> massResult = createPartialUpdateOperation(List.of(modelChanges),
                TextBanner.class, bannerInfo.getClientInfo().getUid()).prepareAndApply();
        assertThat(massResult.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(Banner.ID)), adNotFound())));
    }

    @Test
    public void errorWhenNoRights() {
        var anotherClientInfo = steps.clientSteps().createDefaultClient();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process("newHref", BannerWithHref.HREF);
        MassResult<Long> massResult = createPartialUpdateOperation(List.of(modelChanges),
                TextBanner.class, anotherClientInfo.getUid()).prepareAndApply();
        assertThat(massResult.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(Banner.ID)), adNotFound())));
    }

    @Test
    public void errorWhenUpdateArchivedCampaign() {
        steps.campaignSteps().archiveCampaign(bannerInfo.getShard(), bannerInfo.getCampaignId());
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process("newHref", BannerWithHref.HREF);
        MassResult<Long> massResult = createPartialUpdateOperation(List.of(modelChanges),
                TextBanner.class, bannerInfo.getClientInfo().getUid()).prepareAndApply();
        assertThat(massResult.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(Banner.ID)), actionInArchivedCampaign())));
    }

    private <T extends Banner> BannersUpdateOperation<T> createPartialUpdateOperation(
            List<ModelChanges<T>> changes,
            Class<T> clazz,
            Long operatorUid) {
        ClientId clientId = bannerInfo.getClientId();
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(clientId);

        return updateOperationFactory.createUpdateOperation(
                Applicability.PARTIAL,
                false,
                ModerationMode.DEFAULT,
                changes,
                bannerInfo.getShard(),
                clientId,
                operatorUid,
                clientEnabledFeatures,
                clazz,
                false,
                false,
                DatabaseMode.ONLY_MYSQL,
                null);
    }

}
