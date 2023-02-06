package ru.yandex.direct.core.entity.adgroup.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.banner.model.BannerWithStatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.DEFAULT_DEVICE_TYPE_TARGETING;
import static ru.yandex.direct.core.testing.data.TestGroups.DEFAULT_NETWORK_TARGETING;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationStatusBsSyncedTest extends AdGroupsUpdateOperationTestBase {

    private static final String NEW_TRACKING_PARAMS =
            RandomStringUtils.randomAlphanumeric(5) + "=" + RandomStringUtils.randomAlphanumeric(5);
    private static final String NEW_MINIMAL_OPERATING_SYSTEM_VERSION = "7.0";
    private static final Set<MobileContentAdGroupDeviceTypeTargeting> NEW_DEVICE_TYPE_TARGETING =
            EnumSet.complementOf(DEFAULT_DEVICE_TYPE_TARGETING);
    private static final Set<MobileContentAdGroupNetworkTargeting> NEW_NETWORK_TARGETING =
            EnumSet.complementOf(DEFAULT_NETWORK_TARGETING);
    private static final String NEW_DOMAIN_URL = RandomStringUtils.randomAlphanumeric(5) + ".ru";
    private static final Set<RelevanceMatchCategory> NEW_RELEVANCE_MATCH_CATEGORIES =
            EnumSet.of(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.alternative_mark);

    @Test
    public void prepareAndApply_FieldToUseAsNameIsChangedInSyncedPerformanceAdGroup_DropsBannersStatusBsSynced() {
        var performanceBannerInfo = performanceBannerSteps.createDefaultPerformanceBanner();
        checkState(performanceBannerInfo.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "группа должна быть синхронизирована с БК");
        checkState(((PerformanceBanner) performanceBannerInfo.getBanner()).getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupFieldToUseAsNameAndCheckBannerStatusBsSynced(performanceBannerInfo, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_FieldToUseAsNameIsChangedInTextAdGroup_DropsBannersStatusBsSynced() {
        var feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        var textAdGroupWithFeedInfo = steps.adGroupSteps()
                .createActiveTextAdGroup(clientInfo, feedInfo.getFeedId(), null);
        var textBannerInfo =
                textBannerSteps.createBanner(new NewTextBannerInfo().withAdGroupInfo(textAdGroupWithFeedInfo));
        checkState(textBannerInfo.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "группа должна быть синхронизирована с БК");
        checkState(((TextBanner) textBannerInfo.getBanner()).getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupFieldToUseAsNameAndCheckBannerStatusBsSynced(textBannerInfo, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInSyncedPerformanceAdGroup_DropsBannersStatusBsSynced() {
        var performanceBannerInfo = performanceBannerSteps.createDefaultPerformanceBanner();
        checkState(performanceBannerInfo.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "группа должна быть синхронизирована с БК");
        checkState(((PerformanceBanner) performanceBannerInfo.getBanner()).getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(performanceBannerInfo, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInUnsyncedPerformanceAdGroup_DropsBannersStatusBsSynced() {
        var performanceBannerInfo = performanceBannerSteps.createDefaultPerformanceBanner();
        adGroupRepository.updateStatusBsSynced(performanceBannerInfo.getShard(),
                singletonList(performanceBannerInfo.getAdGroupId()), StatusBsSynced.NO
        );
        checkState(((PerformanceBanner) performanceBannerInfo.getBanner()).getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(performanceBannerInfo, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInSyncedDynamicAdGroup_DropsBannersStatusBsSynced() {
        DynamicBannerInfo dynamicBannerInfo = bannerSteps.createActiveDynamicBanner();
        checkState(dynamicBannerInfo.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "группа должна быть синхронизирована с БК");
        checkState(dynamicBannerInfo.getBanner().getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(dynamicBannerInfo, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInUnsyncedDynamicAdGroup_DropsBannersStatusBsSynced() {
        DynamicBannerInfo dynamicBannerInfo = bannerSteps.createActiveDynamicBanner();
        adGroupRepository.updateStatusBsSynced(dynamicBannerInfo.getShard(),
                singletonList(dynamicBannerInfo.getAdGroupId()), StatusBsSynced.NO
        );
        checkState(dynamicBannerInfo.getBanner().getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(dynamicBannerInfo, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInSyncedTextAdGroup_DoesNotDropBannersStatusBsSynced() {
        var textBannerInfo = textBannerSteps.createDefaultTextBanner();
        checkState(textBannerInfo.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "группа должна быть синхронизирована с БК");
        checkState(((TextBanner) textBannerInfo.getBanner()).getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(textBannerInfo, StatusBsSynced.YES);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInUnsyncedTextAdGroup_DoesNotDropBannersStatusBsSynced() {
        var textBannerInfo = textBannerSteps.createDefaultTextBanner();
        adGroupRepository.updateStatusBsSynced(textBannerInfo.getShard(),
                singletonList(textBannerInfo.getAdGroupId()), StatusBsSynced.NO
        );
        checkState(((TextBanner) textBannerInfo.getBanner()).getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");

        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(textBannerInfo, StatusBsSynced.YES);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createDefaultAdGroup(),
                g -> modelChangesWithValidMinusKeywords(g.getAdGroup()));
    }

    @Test
    public void prepareAndApply_GeoIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createDefaultAdGroup(),
                g -> modelChangesWithGeo(g.getAdGroup()));
    }

    @Test
    public void prepareAndApply_ProjectParamConditionsIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createDefaultAdGroup(),
                g -> modelChangesWithProjectParamConditions(g.getAdGroup()));
    }

    @Test
    public void prepareAndApply_TrackingParamsIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createDefaultAdGroup(),
                this::modelChangesWithTrackingParams);
    }

    @Test
    public void prepareAndApply_MinimalOperatingSystemVersionIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createActiveMobileContentAdGroup(),
                modelChangesWithMinimalOperatingSystemVersion());
    }

    @Test
    public void prepareAndApply_DeviceTypeTargetingIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createActiveMobileContentAdGroup(),
                modelChangesWithDeviceTypeTargeting());
    }

    @Test
    public void prepareAndApply_NetworkTargetingIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createActiveMobileContentAdGroup(),
                modelChangesWithNetworkTargeting());
    }

    @Test
    public void prepareAndApply_DomainUrlIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createActiveDynamicTextAdGroup(),
                modelChangesWithDomainUrl());
    }

    @Test
    public void prepareAndApply_RelevanceMatchCategoriesIsChanged_StatusBsSyncIsNoAndLastChangeIsChanged() {
        updateAndCheckAdGroupStatusBsSyncedAndLastChange(
                () -> adGroupSteps.createActiveDynamicTextAdGroup(),
                modelChangesWithRelevanceMatchCategories());
    }

    private void prepareAndApplyAdGroupUpdateWithSuccess(
            AdGroupInfo adGroupInfo, Function<AdGroupInfo, ModelChanges<AdGroup>> modelChanges) {
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL,
                singletonList(modelChanges.apply(adGroupInfo)), adGroupInfo.getUid(),
                adGroupInfo.getClientId(), adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();

        assumeThat("результат операции должен быть положительный",
                result.getValidationResult(), hasNoDefectsDefinitions());
    }

    private void updateAndCheckAdGroupStatusBsSyncedAndLastChange(
            Supplier<AdGroupInfo> adGroupSupplier,
            Function<AdGroupInfo, ModelChanges<AdGroup>> modelChanges) {
        AdGroupInfo adGroupInfo = adGroupSupplier.get();

        adGroupRepository.updateAdGroups(
                adGroupInfo.getShard(),
                adGroupInfo.getClientId(),
                singleton(new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class)
                        .process(LocalDateTime.now().minusDays(1), AdGroup.LAST_CHANGE)
                        .applyTo(adGroupInfo.getAdGroup())));

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(
                adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())
        );

        checkState(adGroups.size() == 1);
        checkState(adGroups.get(0).getStatusBsSynced() == StatusBsSynced.YES);

        prepareAndApplyAdGroupUpdateWithSuccess(adGroupInfo, modelChanges);

        List<AdGroup> updatedAdGroups = adGroupRepository.getAdGroups(
                adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())
        );

        assertThat(updatedAdGroups.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(updatedAdGroups.get(0).getLastChange(), approximatelyNow());
    }

    private void updateAdGroupFieldToUseAsNameAndCheckBannerStatusBsSynced(
            NewBannerInfo bannerInfo, StatusBsSynced expectedStatus) {
        var modelChanges = modelChangesWithFieldToUseAsName(bannerInfo.getAdGroupId())
                .castModel(AdGroup.class);
        updateAdGroupAndCheckBannerStatusBsSynced(modelChanges, bannerInfo, expectedStatus);
    }

    private void updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(
            AdGroupInfo adGroupInfo, Long bannerId, StatusBsSynced expectedStatus) {
        var modelChanges = modelChangesWithValidMinusKeywords(adGroupInfo.getAdGroupId());
        updateAdGroupAndCheckBannerStatusBsSynced(modelChanges, adGroupInfo, bannerId, expectedStatus);
    }

    private void updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(
            NewBannerInfo bannerInfo, StatusBsSynced expectedStatus) {
        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(
                bannerInfo.getAdGroupInfo(), bannerInfo.getBannerId(), expectedStatus);
    }

    private void updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(
            AbstractBannerInfo<?> bannerInfo, StatusBsSynced expectedStatus) {
        updateAdGroupMinusKeywordsAndCheckBannerStatusBsSynced(
                bannerInfo.getAdGroupInfo(), bannerInfo.getBannerId(), expectedStatus);
    }

    private void updateAdGroupAndCheckBannerStatusBsSynced(
            ModelChanges<AdGroup> modelChanges, NewBannerInfo bannerInfo, StatusBsSynced expectedStatus) {
        updateAdGroupAndCheckBannerStatusBsSynced(
                modelChanges, bannerInfo.getAdGroupInfo(), bannerInfo.getBannerId(), expectedStatus);
    }

    private void updateAdGroupAndCheckBannerStatusBsSynced(
            ModelChanges<AdGroup> modelChanges, AdGroupInfo adGroupInfo, Long bannerId, StatusBsSynced expectedStatus) {
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL,
                singletonList(modelChanges), adGroupInfo.getUid(),
                adGroupInfo.getClientId(), adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();

        assumeThat("результат операции должен быть положительный",
                result.getValidationResult(), hasNoDefectsDefinitions());

        var banner = newBannerRepository.type
                .getStrictly(shard, singletonList(bannerId), BannerWithStatusBsSynced.class).get(0);
        assertThat(banner.getStatusBsSynced(), equalTo(expectedStatus));
    }

    private ModelChanges<AdGroup> modelChangesWithValidMinusKeywords(AdGroup adGroup) {
        return modelChangesWithValidMinusKeywords(adGroup.getId());
    }

    private ModelChanges<AdGroup> modelChangesWithTrackingParams(AdGroupInfo adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getAdGroupId(), AdGroup.class);
        modelChanges.process(NEW_TRACKING_PARAMS, AdGroup.TRACKING_PARAMS);
        return modelChanges;
    }

    private Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWithMinimalOperatingSystemVersion() {
        return modelChangesWith(
                mc -> mc.process(NEW_MINIMAL_OPERATING_SYSTEM_VERSION,
                        MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION),
                MobileContentAdGroup.class);
    }

    private Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWithDeviceTypeTargeting() {
        return modelChangesWith(
                mc -> mc.process(NEW_DEVICE_TYPE_TARGETING, MobileContentAdGroup.DEVICE_TYPE_TARGETING),
                MobileContentAdGroup.class);
    }

    private Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWithNetworkTargeting() {
        return modelChangesWith(
                mc -> mc.process(NEW_NETWORK_TARGETING, MobileContentAdGroup.NETWORK_TARGETING),
                MobileContentAdGroup.class);
    }

    private Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWithDomainUrl() {
        return modelChangesWith(
                mc -> mc.process(NEW_DOMAIN_URL, DynamicTextAdGroup.DOMAIN_URL),
                DynamicTextAdGroup.class);
    }

    private Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWithRelevanceMatchCategories() {
        return modelChangesWith(
                mc -> mc.process(NEW_RELEVANCE_MATCH_CATEGORIES, DynamicAdGroup.RELEVANCE_MATCH_CATEGORIES),
                DynamicAdGroup.class);
    }
}
