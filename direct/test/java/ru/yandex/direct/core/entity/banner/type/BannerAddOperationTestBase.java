package ru.yandex.direct.core.entity.banner.type;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperation;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.service.DatabaseMode;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
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

abstract class BannerAddOperationTestBase {

    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    public FeatureService featureService;

    @Autowired
    public Steps steps;

    protected Long prepareAndApplyValid(BannerWithAdGroupId banner) {
        return prepareAndApplyValid(banner, false);
    }

    protected Long prepareAndApplyValid(BannerWithAdGroupId banner, boolean saveDraft) {
        return prepareAndApplyValid(singletonList(banner), saveDraft).get(0);
    }

    protected List<Long> prepareAndApplyValid(List<BannerWithAdGroupId> banners) {
        return prepareAndApplyValid(banners, false);
    }

    protected List<Long> prepareAndApplyValid(List<BannerWithAdGroupId> banners, boolean saveDraft) {
        MassResult<Long> result = createOperation(banners, saveDraft).prepareAndApply();

        String defectsDescription = String.join("\n\t",
                mapList(result.getValidationResult().flattenErrors(), DefectInfo::toString));
        assumeThat("Unexpected errors:\n\t" + defectsDescription, result, isFullySuccessful());

        return mapList(result.getResult(), Result::getResult);
    }

    protected ValidationResult<?, Defect> prepareAndApplyInvalid(BannerWithAdGroupId banner) {
        MassResult<Long> result = createOperation(banner, false).prepareAndApply();
        assumeThat(result, isSuccessful(false));
        return result.get(0).getValidationResult();
    }

    protected BannersAddOperation createOperation(BannerWithAdGroupId banner, boolean saveDraft) {
        return createOperation(singletonList(banner), saveDraft);
    }

    protected BannersAddOperation createOperation(List<BannerWithAdGroupId> banners, boolean saveDraft) {
        return createOperation(banners, saveDraft, Applicability.FULL);
    }

    protected BannersAddOperation createOperation(List<BannerWithAdGroupId> banners, boolean saveDraft,
                                                  Applicability applicability) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(getClientId());

        return addOperationFactory.createAddOperation(applicability, false, banners,
                getShard(), getClientId(), getUid(), saveDraft, false, false, false,
                clientEnabledFeatures);
    }

    protected BannersAddOperation createOperation(List<BannerWithAdGroupId> banners, ModerationMode moderationMode,
                                                  Applicability applicability) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(getClientId());

        return addOperationFactory.createAddOperation(applicability, false, banners,
                getShard(), getClientId(), getUid(), moderationMode, false, false, false, DatabaseMode.ONLY_MYSQL,
                clientEnabledFeatures);
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
