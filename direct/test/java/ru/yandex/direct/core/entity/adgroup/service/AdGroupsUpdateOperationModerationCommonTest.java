package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationModerationCommonTest extends AdGroupsUpdateOperationTestBase {

    @Test
    public void statusPostModerateSwitchedToNoWhenNotRejected() {
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(activeTextAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.NO));

        prepareAndApplyAdGroupUpdateWithSuccess(
                adGroupInfo,
                g -> modelChangesWithGeo(g.getAdGroup()),
                ModerationMode.DEFAULT);

        List<AdGroup> updatedAdGroups = adGroupRepository
                .getAdGroups(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId()));

        assertThat(updatedAdGroups.get(0).getStatusPostModerate(), equalTo(StatusPostModerate.NO));
    }

    @Test
    public void prepareAndApply_AdGroupWithBannerWithoutForexFlag_ExpectBannerStatusModerateDoesntChanged() {
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(
                new AdGroupInfo()
                        .withAdGroup(activeTextAdGroup(null).withGeo(singletonList(Region.TURKEY_REGION_ID))));

        TextBannerInfo bannerInfo = bannerSteps.createBanner(
                activeTextBanner()
                        .withStatusModerate(OldBannerStatusModerate.YES),
                adGroupInfo);

        prepareAndApplyAdGroupUpdateWithSuccess(
                adGroupInfo,
                modelChangesWith(
                        mc -> mc.process(asList(Region.MOSCOW_REGION_ID, Region.TURKEY_REGION_ID), AdGroup.GEO),
                        TextAdGroup.class),
                ModerationMode.DEFAULT);

        OldBanner modifiedBanner = bannerRepository.getBanners(
                bannerInfo.getShard(), singleton(bannerInfo.getBannerId()))
                .get(0);

        assertThat(
                modifiedBanner,
                hasProperty(
                        "statusModerate",
                        equalTo(OldBannerStatusModerate.YES)));
    }

    @Test
    public void prepareAndApply_AdGroupWithBannerWithForexFlag_ExpectBannerStatusModerateChangeToReady() {
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(
                new AdGroupInfo()
                        .withAdGroup(activeTextAdGroup(null).withGeo(singletonList(Region.TURKEY_REGION_ID))));

        TextBannerInfo bannerInfo = bannerSteps.createBanner(
                activeTextBanner()
                        .withFlags(new BannerFlags().with(BannerFlags.FOREX, true))
                        .withStatusModerate(OldBannerStatusModerate.YES),
                adGroupInfo);

        prepareAndApplyAdGroupUpdateWithSuccess(
                adGroupInfo,
                modelChangesWith(
                        mc -> mc.process(asList(Region.MOSCOW_REGION_ID, Region.TURKEY_REGION_ID), AdGroup.GEO),
                        TextAdGroup.class),
                ModerationMode.DEFAULT);

        OldBanner modifiedBanner = bannerRepository.getBanners(
                bannerInfo.getShard(), singleton(bannerInfo.getBannerId()))
                .get(0);

        assertThat(
                modifiedBanner,
                hasProperty(
                        "statusModerate",
                        equalTo(OldBannerStatusModerate.READY)));
    }

    @Test
    public void prepareAndApply_AdGroupWithBannerWithForexFlag_ExpectBannerStatusModerateDoesntChange() {
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(
                new AdGroupInfo()
                        .withAdGroup(activeTextAdGroup(null).withGeo(singletonList(Region.TURKEY_REGION_ID))));

        TextBannerInfo bannerInfo = bannerSteps.createBanner(
                activeTextBanner()
                        .withStatusModerate(OldBannerStatusModerate.YES),
                adGroupInfo);

        prepareAndApplyAdGroupUpdateWithSuccess(
                adGroupInfo,
                modelChangesWith(
                        mc -> mc.process(singletonList(Region.MOSCOW_REGION_ID), AdGroup.GEO),
                        TextAdGroup.class),
                ModerationMode.DEFAULT);

        OldBanner modifiedBanner = bannerRepository.getBanners(
                bannerInfo.getShard(), singleton(bannerInfo.getBannerId()))
                .get(0);

        assertThat(
                modifiedBanner,
                hasProperty(
                        "statusModerate",
                        equalTo(OldBannerStatusModerate.YES)));
    }

    @Test
    public void correctStatusesWithSaveDraftFlag() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidName(adGroup1);
        AdGroupUpdateOperationParams operationParams = AdGroupUpdateOperationParams.builder()
                .withModerationMode(ModerationMode.FORCE_SAVE_DRAFT)
                .withValidateInterconnections(true)
                .build();
        AdGroupsUpdateOperation adGroupsUpdateOperation =
                adGroupsUpdateOperationFactory.newInstance(
                        Applicability.FULL,
                        singletonList(modelChanges),
                        operationParams,
                        geoTree,
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                        operatorUid,
                        clientId,
                        shard);
        adGroupsUpdateOperation.prepareAndApply();
        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroup1.getId())).get(0);
        assertThat(actual.getStatusModerate(), is(StatusModerate.NEW));

    }

    private void prepareAndApplyAdGroupUpdateWithSuccess(AdGroupInfo adGroupInfo,
                                                         Function<AdGroupInfo, ModelChanges<AdGroup>> modelChanges, ModerationMode moderationMode) {
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL,
                singletonList(modelChanges.apply(adGroupInfo)), moderationMode,
                adGroupInfo.getUid(), adGroupInfo.getClientId(), adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();

        assumeThat("результат операции должен быть положительный",
                result.getValidationResult(), hasNoDefectsDefinitions());
    }
}
