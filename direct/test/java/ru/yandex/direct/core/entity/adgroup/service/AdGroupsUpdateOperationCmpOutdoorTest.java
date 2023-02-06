package ru.yandex.direct.core.entity.adgroup.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateOperator;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerModerationVersionsRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerateBannerPagesRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmOutdoorAdGroupValidation.OUTDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationCmpOutdoorTest extends AdGroupsUpdateOperationTestBase {

    public static final String NEW_NAME = "имя";

    private static final Long PAGE_ID_1 = 1L;
    private static final Long PAGE_ID_2 = 2L;
    private static final Long PAGE_ID_3 = 3L;
    private static final Long BLOCK_ID_1 = 101L;
    private static final Long BLOCK_ID_2 = 102L;

    @Autowired
    private TestModerateBannerPagesRepository testModerateBannerPagesRepository;

    @Autowired
    private TestBannerModerationVersionsRepository testBannerModerationVersionsRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Before
    public void before() {
        super.before();
        geoTree = geoTreeFactory.getRussianGeoTree();

        placementSteps.clearPlacements();
        placementSteps.addOutdoorPlacementWithCreativeDefaults(PAGE_ID_1, BLOCK_ID_1, BLOCK_ID_2);
        placementSteps.addOutdoorPlacementWithCreativeDefaults(PAGE_ID_2, BLOCK_ID_1, BLOCK_ID_2);
        placementSteps.addOutdoorPlacementWithCreativeDefaults(PAGE_ID_3, BLOCK_ID_1, BLOCK_ID_2);
    }

    @Test
    public void prepareAndApply_ChangeOnlyName_SuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithName(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withGeo(OUTDOOR_GEO_DEFAULT)
                .withMinusKeywords(emptyList())
                .withName(NEW_NAME)
                .withPageBlocks(((CpmOutdoorAdGroup) adGroupInfo.getAdGroup()).getPageBlocks())
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_ChangePageBlocks_SuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();

        List<PageBlock> newPageBlocks = singletonList(new PageBlock().withPageId(PAGE_ID_1).withImpId(BLOCK_ID_1));
        ModelChanges<CpmOutdoorAdGroup> modelChanges =
                new ModelChanges<>(adGroupInfo.getAdGroupId(), CpmOutdoorAdGroup.class);
        modelChanges.process(newPageBlocks, CpmOutdoorAdGroup.PAGE_BLOCKS);

        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> result = updateOperation.prepareAndApply();

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withGeo(OUTDOOR_GEO_DEFAULT)
                .withMinusKeywords(emptyList())
                .withName(adGroupInfo.getAdGroup().getName())
                .withPageBlocks(newPageBlocks)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_SetUnexistingPage_PageBlocksValidationConnected() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();

        List<PageBlock> newPageBlocks = singletonList(createPageBlock(23984L, 4L));
        ModelChanges<CpmOutdoorAdGroup> modelChanges =
                new ModelChanges<>(adGroupInfo.getAdGroupId(), CpmOutdoorAdGroup.class);
        modelChanges.process(newPageBlocks, CpmOutdoorAdGroup.PAGE_BLOCKS);

        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> result = updateOperation.prepareAndApply();

        Path path = path(index(0), field(CpmOutdoorAdGroup.PAGE_BLOCKS), index(0), field("pageId"));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, objectNotFound())));
    }

    @Test
    public void prepareAndApply_ChangeGeo_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithGeo(adGroupInfo.getAdGroup());
        checkUnsuccessfulUpdate(modelChanges, adGroupInfo);
    }

    @Test
    public void prepareAndApply_ChangeGeoToEmptyList_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithEmptyGeo(adGroupInfo.getAdGroup());
        checkUnsuccessfulUpdate(modelChanges, adGroupInfo);
    }

    @Test
    public void prepareAndApply_ChangeMinusKeywords_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithMinusKeywords(adGroupInfo.getAdGroup());
        checkUnsuccessfulUpdate(modelChanges, adGroupInfo);
    }

// internal moderation

    @Test
    public void prepareAndApply_DefaultModerationModeForDraftAdGroup_BannersNotAffected() {
        AdGroupInfo adGroupInfo = createDraftAdGroup();
        CpmOutdoorBannerInfo cpmOutdoorBanner = bannerSteps.createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.READY),
                adGroupInfo);

        ModelChanges<AdGroup> modelChanges = modelChangesWithName(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges),
                ModerationMode.DEFAULT, adGroupInfo.getUid(), adGroupInfo.getClientId(), adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withName(NEW_NAME)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);
        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));

        OldBanner expectedBanner = new OldCpmOutdoorBanner()
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withStatusPostModerate(OldBannerStatusPostModerate.READY);
        OldBanner realBanner = bannerRepository.getBanners(shard, singletonList(cpmOutdoorBanner.getBannerId())).get(0);
        assertThat(realBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_ForceModerateModerationModeForDraftAdGroup_BannersNotAffected() {
        AdGroupInfo adGroupInfo = createDraftAdGroup();
        CpmOutdoorBannerInfo cpmOutdoorBanner = bannerSteps.createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.READY),
                adGroupInfo);

        ModelChanges<AdGroup> modelChanges = modelChangesWithName(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges),
                ModerationMode.FORCE_MODERATE, adGroupInfo.getUid(), adGroupInfo.getClientId(), adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withName(NEW_NAME)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));

        OldBanner expectedBanner = new OldCpmOutdoorBanner()
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withStatusPostModerate(OldBannerStatusPostModerate.READY);
        OldBanner realBanner = bannerRepository.getBanners(shard, singletonList(cpmOutdoorBanner.getBannerId())).get(0);
        assertThat(realBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_ForceSaveDraftModerationModeForActiveAdGroup_BannersNotAffected() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmOutdoorAdGroup();
        CpmOutdoorBannerInfo cpmOutdoorBanner = bannerSteps.createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.READY),
                adGroupInfo);

        ModelChanges<AdGroup> modelChanges = modelChangesWithName(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges),
                ModerationMode.FORCE_SAVE_DRAFT, adGroupInfo.getUid(), adGroupInfo.getClientId(),
                adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withName(NEW_NAME)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);
        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));

        OldBanner expectedBanner = new OldCpmOutdoorBanner()
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withStatusPostModerate(OldBannerStatusPostModerate.READY);
        OldBanner realBanner = bannerRepository.getBanners(shard, singletonList(cpmOutdoorBanner.getBannerId())).get(0);
        assertThat(realBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

// external moderation

    @Test
    public void prepareAndApply_ChangePageBlocks_ExternalModerationDataUpdated() {
        long bannerVersion = 5L;

        AdGroupInfo adGroupInfo = createOutdoorAdGroupWithPageBlocks(
                createPageBlock(PAGE_ID_1, BLOCK_ID_1),
                createPageBlock(PAGE_ID_2, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion, PAGE_ID_1, PAGE_ID_2);

        ModelChanges<AdGroup> modelChanges = modelChangesWithPageBlocks(adGroupInfo.getAdGroup(),
                createPageBlock(PAGE_ID_2, BLOCK_ID_2),
                createPageBlock(PAGE_ID_3, BLOCK_ID_2));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertModerateBannerPages(bannerInfo.getBannerId(), asList(
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_1)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(true),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_2)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_3)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.READY)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false)
        ));
        assertBannerStatusBsSynced(bannerInfo.getBannerId(), StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_AddOnlyPageBlocks_ExternalModerationDataUpdated() {
        long bannerVersion = 5L;

        AdGroupInfo adGroupInfo = createOutdoorAdGroupWithPageBlocks(
                createPageBlock(PAGE_ID_1, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion, PAGE_ID_1);

        ModelChanges<AdGroup> modelChanges = modelChangesWithPageBlocks(adGroupInfo.getAdGroup(),
                createPageBlock(PAGE_ID_1, BLOCK_ID_1),
                createPageBlock(PAGE_ID_2, BLOCK_ID_1));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertModerateBannerPages(bannerInfo.getBannerId(), asList(
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_1)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_2)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.READY)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false)
        ));
        assertBannerStatusBsSynced(bannerInfo.getBannerId(), StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_RemoveOnlyPageBlocks_ExternalModerationDataUpdated() {
        long bannerVersion = 5L;

        AdGroupInfo adGroupInfo = createOutdoorAdGroupWithPageBlocks(
                createPageBlock(PAGE_ID_1, BLOCK_ID_1),
                createPageBlock(PAGE_ID_2, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion, PAGE_ID_1, PAGE_ID_2);

        ModelChanges<AdGroup> modelChanges = modelChangesWithPageBlocks(adGroupInfo.getAdGroup(),
                createPageBlock(PAGE_ID_1, BLOCK_ID_1));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertModerateBannerPages(bannerInfo.getBannerId(), asList(new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_1)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_2)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(true)
        ));
        assertBannerStatusBsSynced(bannerInfo.getBannerId(), StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_ChangeOnlyBlock_ExternalModerationDataNotUpdated() {
        long bannerVersion = 5L;

        AdGroupInfo adGroupInfo =
                createOutdoorAdGroupWithPageBlocks(createPageBlock(PAGE_ID_1, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion, PAGE_ID_1);

        ModelChanges<AdGroup> modelChanges =
                modelChangesWithPageBlocks(adGroupInfo.getAdGroup(), createPageBlock(PAGE_ID_1, BLOCK_ID_2));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertModerateBannerPage(bannerInfo.getBannerId(), new ModerateBannerPage()
                .withBannerId(bannerInfo.getBannerId())
                .withPageId(PAGE_ID_1)
                .withVersion(bannerVersion)
                .withStatusModerate(StatusModerateBannerPage.SENT)
                .withStatusModerateOperator(StatusModerateOperator.NONE)
                .withIsRemoved(false));
        assertBannerStatusBsSynced(bannerInfo.getBannerId(), StatusBsSynced.YES);
    }

    @Test
    public void prepareAndApply_ChangePageBlocksWithoutModeratedBanners_ExternalModerationDataNotUpdated() {
        AdGroupInfo adGroupInfo =
                createOutdoorAdGroupWithPageBlocks(createPageBlock(PAGE_ID_1, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo = bannerSteps.createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.SENT),
                adGroupInfo);

        ModelChanges<AdGroup> modelChanges =
                modelChangesWithPageBlocks(adGroupInfo.getAdGroup(), createPageBlock(PAGE_ID_2, BLOCK_ID_2));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertNoModerateBannerPages(bannerInfo.getBannerId());
        assertBannerStatusBsSynced(bannerInfo.getBannerId(), StatusBsSynced.YES);
    }

    @Test
    public void prepareAndApply_ChangePageBlocksWithMultipleBannersAndMultipleBlocks() {
        long bannerVersion1 = 5L;
        long bannerVersion2 = 10L;

        AdGroupInfo adGroupInfo = createOutdoorAdGroupWithPageBlocks(createPageBlock(PAGE_ID_1, BLOCK_ID_1),
                createPageBlock(PAGE_ID_2, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo1 =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion1, PAGE_ID_1, PAGE_ID_2);
        CpmOutdoorBannerInfo bannerInfo2 =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion2, PAGE_ID_1, PAGE_ID_2);

        ModelChanges<AdGroup> modelChanges = modelChangesWithPageBlocks(adGroupInfo.getAdGroup(),
                createPageBlock(PAGE_ID_2, BLOCK_ID_2), createPageBlock(PAGE_ID_3, BLOCK_ID_2));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertModerateBannerPages(bannerInfo1.getBannerId(), asList(
                new ModerateBannerPage()
                        .withBannerId(bannerInfo1.getBannerId())
                        .withPageId(PAGE_ID_1)
                        .withVersion(bannerVersion1)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(true),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo1.getBannerId())
                        .withPageId(PAGE_ID_2)
                        .withVersion(bannerVersion1)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo1.getBannerId())
                        .withPageId(PAGE_ID_3)
                        .withVersion(bannerVersion1)
                        .withStatusModerate(StatusModerateBannerPage.READY)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false)
        ));
        assertBannerStatusBsSynced(bannerInfo1.getBannerId(), StatusBsSynced.NO);
        assertModerateBannerPages(bannerInfo2.getBannerId(), asList(
                new ModerateBannerPage()
                        .withBannerId(bannerInfo2.getBannerId())
                        .withPageId(PAGE_ID_1)
                        .withVersion(bannerVersion2)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(true),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo2.getBannerId())
                        .withPageId(PAGE_ID_2)
                        .withVersion(bannerVersion2)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false),
                new ModerateBannerPage()
                        .withBannerId(bannerInfo2.getBannerId())
                        .withPageId(PAGE_ID_3)
                        .withVersion(bannerVersion2)
                        .withStatusModerate(StatusModerateBannerPage.READY)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(false)
        ));
        assertBannerStatusBsSynced(bannerInfo2.getBannerId(), StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_ChangePageBlocksAndAdGroupGeoEqualBannerMinusGeo_NoNewExternalModeration() {
        long bannerVersion = 5L;

        AdGroupInfo adGroupInfo =
                createOutdoorAdGroupWithPageBlocks(createPageBlock(PAGE_ID_1, BLOCK_ID_1));
        CpmOutdoorBannerInfo bannerInfo =
                createInternallyModeratedOutdoorBanner(adGroupInfo, bannerVersion, PAGE_ID_1);
        addBannerMinusGeo(bannerInfo, OUTDOOR_GEO_DEFAULT);

        ModelChanges<AdGroup> modelChanges =
                modelChangesWithPageBlocks(adGroupInfo.getAdGroup(), createPageBlock(PAGE_ID_2, BLOCK_ID_2));
        updateAndAssumeResultIsSuccessful(FULL, modelChanges);

        assertModerateBannerPages(bannerInfo.getBannerId(), singletonList(
                new ModerateBannerPage()
                        .withBannerId(bannerInfo.getBannerId())
                        .withPageId(PAGE_ID_1)
                        .withVersion(bannerVersion)
                        .withStatusModerate(StatusModerateBannerPage.SENT)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withIsRemoved(true)));
        assertBannerStatusBsSynced(bannerInfo.getBannerId(), StatusBsSynced.NO);
    }

    private void checkUnsuccessfulUpdate(ModelChanges<AdGroup> modelChanges, AdGroupInfo adGroupInfo) {

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));

        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(realAdGroup.getGeo(), is(OUTDOOR_GEO_DEFAULT));
        assertThat(realAdGroup.getMinusKeywords(), is(emptyList()));
    }

    private ModelChanges<AdGroup> modelChangesWithName(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(NEW_NAME, AdGroup.NAME);
        return modelChanges;
    }

    private ModelChanges<AdGroup> modelChangesWithEmptyGeo(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(emptyList(), AdGroup.GEO);
        return modelChanges;
    }

    private ModelChanges<AdGroup> modelChangesWithMinusKeywords(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(singletonList("abc"), AdGroup.MINUS_KEYWORDS);
        return modelChanges;
    }

    private ModelChanges<AdGroup> modelChangesWithPageBlocks(AdGroup adGroup, PageBlock... pageBlocks) {
        ModelChanges<CpmOutdoorAdGroup> modelChanges =
                new ModelChanges<>(adGroup.getId(), CpmOutdoorAdGroup.class);
        modelChanges.process(asList(pageBlocks), CpmOutdoorAdGroup.PAGE_BLOCKS);
        return modelChanges.castModelUp(AdGroup.class);
    }

    private AdGroupInfo createDraftAdGroup() {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();
        return adGroupSteps.createAdGroup(activeCpmOutdoorAdGroup(null, placement)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW)
                .withStatusPostModerate(ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO));
    }

    private AdGroupInfo createOutdoorAdGroupWithPageBlocks(PageBlock... pageBlocks) {
        return adGroupSteps.createAdGroup(activeCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(pageBlocks)), clientInfo);
    }

    private CpmOutdoorBannerInfo createInternallyModeratedOutdoorBanner(AdGroupInfo adGroup, long version,
                                                                        long... pageIds) {
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(adGroup.getClientInfo(), videoCreativeId);

        CpmOutdoorBannerInfo bannerInfo = bannerSteps.createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroup.getCampaignId(), adGroup.getAdGroupId(), videoCreativeId)
                        .withStatusModerate(OldBannerStatusModerate.YES),
                adGroup);
        testBannerModerationVersionsRepository.addVersion(shard, bannerInfo.getBannerId(), version);
        createModerateBannerPages(bannerInfo.getBannerId(), version, StatusModerateBannerPage.SENT, pageIds);
        return bannerInfo;
    }

    private List<ModerateBannerPage> createModerateBannerPages(long bannerId, long version,
                                                               StatusModerateBannerPage status, long... pageIds) {
        List<ModerateBannerPage> moderateBannerPages = LongStreamEx.of(pageIds)
                .mapToObj(pageId -> new ModerateBannerPage()
                        .withBannerId(bannerId)
                        .withPageId(pageId)
                        .withVersion(version)
                        .withStatusModerate(status)
                        .withStatusModerateOperator(StatusModerateOperator.NONE)
                        .withCreateTime(LocalDateTime.now())
                        .withIsRemoved(false))
                .toList();
        testModerateBannerPagesRepository.addModerateBannerPages(shard, moderateBannerPages);
        return moderateBannerPages;
    }

    private PageBlock createPageBlock(long pageId, long blockId) {
        return new PageBlock()
                .withPageId(pageId)
                .withImpId(blockId);
    }

    private void addBannerMinusGeo(AbstractBannerInfo<? extends OldBanner> banner,
                                   List<Long> minusGeo) {
        testBannerRepository.addMinusGeo(shard, banner.getBannerId(),
                BannersMinusGeoType.current, minusGeo.stream().map(Object::toString).collect(joining(",")));
    }

    private void assertNoModerateBannerPages(long bannerId) {
        List<ModerateBannerPage> actualExternalModeration =
                testModerateBannerPagesRepository.getModerateBannerPages(shard, bannerId);
        assertThat(actualExternalModeration, empty());
    }

    private void assertModerateBannerPage(long bannerId, ModerateBannerPage expectedModerateBannerPage) {
        assertModerateBannerPages(bannerId, singletonList(expectedModerateBannerPage));
    }

    private void assertModerateBannerPages(long bannerId, Collection<ModerateBannerPage> expectedModerateBannerPages) {
        List<ModerateBannerPage> actualExternalModeration =
                testModerateBannerPagesRepository.getModerateBannerPages(shard, bannerId);
        assertThat(actualExternalModeration, hasSize(expectedModerateBannerPages.size()));
        assertThat(actualExternalModeration, containsInAnyOrder(mapList(expectedModerateBannerPages,
                expected -> beanDiffer(expected).useCompareStrategy(onlyExpectedFields()))));
    }

    private void assertBannerStatusBsSynced(long bannerId, StatusBsSynced expectedStatusBsSynced) {
        OldBanner actualBanner = bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
        assertThat(actualBanner.getStatusBsSynced(), is(expectedStatusBsSynced));
    }
}
