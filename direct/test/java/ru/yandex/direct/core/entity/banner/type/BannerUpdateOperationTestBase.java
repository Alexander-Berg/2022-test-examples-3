package ru.yandex.direct.core.entity.banner.type;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperation;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

abstract class BannerUpdateOperationTestBase {

    private static final Applicability DEFAULT_APPLICABILITY = Applicability.FULL;

    @Autowired
    public BannersUpdateOperationFactory updateOperationFactory;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    public FeatureService featureService;

    @Autowired
    public Steps steps;

    protected Long prepareAndApplyValid(ModelChanges<? extends BannerWithSystemFields> modelChanges) {
        return prepareAndApplyValid(singletonList(modelChanges.castModelUp(BannerWithSystemFields.class))).get(0);
    }

    protected List<Long> prepareAndApplyValid(
            List<? extends ModelChanges<? extends BannerWithSystemFields>> modelChangesList) {
        return prepareAndApplyValid(modelChangesList, getModerationMode());
    }

    protected List<Long> prepareAndApplyValid(
            List<? extends ModelChanges<? extends BannerWithSystemFields>> modelChangesList,
            ModerationMode moderationMode) {
        MassResult<Long> result = createOperation(modelChangesList, moderationMode).prepareAndApply();
        String defectsDescription = String.join("\n\t",
                mapList(result.getValidationResult().flattenErrors(), DefectInfo::toString));
        assumeThat("Unexpected errors:\n\t" + defectsDescription, result, isFullySuccessful());
        return mapList(result.getResult(), Result::getResult);
    }

    protected ValidationResult<?, Defect> prepareAndApplyInvalid(
            ModelChanges<? extends BannerWithSystemFields> modelChanges) {
        MassResult<Long> result = createOperation(modelChanges, getModerationMode()).prepareAndApply();
        assumeThat(result, isSuccessful(false));
        return result.get(0).getValidationResult();
    }

    protected ModerationMode getModerationMode() {
        return ModerationMode.DEFAULT;
    }

    protected BannersUpdateOperation<BannerWithSystemFields> createOperation(
            ModelChanges<? extends BannerWithSystemFields> modelChanges) {
        return createOperation(singletonList(modelChanges.castModelUp(BannerWithSystemFields.class)),
                getModerationMode());
    }

    protected BannersUpdateOperation<BannerWithSystemFields> createOperation(
            ModelChanges<? extends BannerWithSystemFields> modelChanges,
            ModerationMode moderationMode) {
        return createOperation(singletonList(modelChanges.castModelUp(BannerWithSystemFields.class)),
                moderationMode);
    }

    protected BannersUpdateOperation<BannerWithSystemFields> createOperation(
            List<? extends ModelChanges<? extends BannerWithSystemFields>> modelChanges) {
        return createOperation(modelChanges, getModerationMode());
    }

    protected BannersUpdateOperation<BannerWithSystemFields> createOperation(
            List<? extends ModelChanges<? extends BannerWithSystemFields>> modelChanges,
            ModerationMode moderationMode) {

        return createOperation(modelChanges, DEFAULT_APPLICABILITY, moderationMode);
    }

    protected BannersUpdateOperation<BannerWithSystemFields> createOperation(
            List<? extends ModelChanges<? extends BannerWithSystemFields>> modelChanges,
            Applicability applicability,
            ModerationMode moderationMode) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(getClientId());

        return updateOperationFactory.createUpdateOperation(
                applicability,
                false,
                moderationMode,
                mapList(modelChanges, mc -> mc.castModelUp(BannerWithSystemFields.class)),
                getShard(),
                getClientId(),
                getUid(),
                clientEnabledFeatures,
                false);
    }

    protected <T extends Banner> T getBanner(Long id, Class<T> bannerClass) {
        return bannerTypedRepository.getStrictly(getShard(), singleton(id), bannerClass).get(0);
    }

    protected <T extends Banner> T getBanner(Long id) {
        return (T) bannerTypedRepository.getTyped(getShard(), singleton(id)).get(0);
    }

    abstract int getShard();

    abstract ClientId getClientId();

    abstract Long getUid();
}
