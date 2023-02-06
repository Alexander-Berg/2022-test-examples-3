package ru.yandex.direct.core.entity.banner.service;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jooq.DSLContext;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.entity.placements.model1.BlockSize;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.testing.data.TestModerateBannerPages;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.info.ModerateBannerPageInfo;
import ru.yandex.direct.core.testing.repository.TestModerateBannerPagesRepository;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage.SENT;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER_PAGE;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_DURATION;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_FORMAT_HEIGHT;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_FORMAT_WIDTH;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public abstract class OutdoorModerateBannerPagesUpdaterBaseTest {

    protected static final Long PAGE_ID_1 = 1L;
    protected static final Long PAGE_ID_2 = 2L;
    protected static final Long PAGE_ID_3 = 3L;
    protected static final Long BLOCK_ID_1 = 14L;
    protected static final Long BLOCK_ID_2 = 15L;
    protected static final Size SIZE_1 = new Size(10, 20);
    protected static final Size SIZE_2 = new Size(20, 10);
    protected static final Size SIZE_3 = new Size(30, 40);
    protected static final Size SIZE_4 = new Size(40, 30);
    protected static final Size DEFAULT_SIZE =
            new Size(DEFAULT_OUTDOOR_VIDEO_FORMAT_WIDTH, DEFAULT_OUTDOOR_VIDEO_FORMAT_HEIGHT);
    protected static final Long BANNER_VERSION = 101L;
    private static final Long DEFAULT_BLOCK_ID = 13L;
    protected static final PageBlock PAGE_BLOCK_1 = createPageBlock(PAGE_ID_1, DEFAULT_BLOCK_ID);
    protected static final PageBlock PAGE_BLOCK_2 = createPageBlock(PAGE_ID_2, DEFAULT_BLOCK_ID);
    protected static final PageBlock PAGE_BLOCK_3 = createPageBlock(PAGE_ID_3, DEFAULT_BLOCK_ID);
    private static final CompareStrategy COMPARE_STRATEGY =
            allFieldsExcept(newPath(ModerateBannerPage.ID.name()), newPath(ModerateBannerPage.CREATE_TIME.name()));

    @Autowired
    private Steps steps;
    @Autowired
    private PlacementSteps placementSteps;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    private ModerationReasonRepository moderationReasonRepository;
    @Autowired
    private TestModerateBannerPagesRepository testModerateBannerPagesRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo defaultClient;
    protected DSLContext dslContext;
    protected int shard;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        shard = defaultClient.getShard();
        dslContext = dslContextProvider.ppc(shard);
        initDefaultOutdoorPlacements();
    }

    private void initDefaultOutdoorPlacements() {
        placementSteps.clearPlacements();
        placementSteps.addOutdoorPlacementWithCreativeDefaults(PAGE_ID_1, DEFAULT_BLOCK_ID);
        placementSteps.addOutdoorPlacementWithCreativeDefaults(PAGE_ID_2, DEFAULT_BLOCK_ID);
        placementSteps.addOutdoorPlacementWithCreativeDefaults(PAGE_ID_3, DEFAULT_BLOCK_ID);
    }

    protected static PageBlock createPageBlock(long pageId, long blockId) {
        return new PageBlock().withPageId(pageId).withImpId(blockId);
    }

    protected void assertModerateBannerPageModReasons(Collection<Long> moderateBannerPageIds,
                                                      Collection<Long> expectedExistingIds) {
        List<Long> modReasonIds =
                mapList(moderationReasonRepository.fetchRejected(shard, BANNER_PAGE, moderateBannerPageIds),
                        ModerationReason::getObjectId);

        assertThat(modReasonIds, hasSize(expectedExistingIds.size()));
        assertThat(modReasonIds, containsInAnyOrder(expectedExistingIds.toArray()));
    }

    protected void assertModerateBannerPages(long bannerId,
                                             Collection<ModerateBannerPage> expectedModerateBannerPages) {
        List<ModerateBannerPage> actualExternalModeration =
                testModerateBannerPagesRepository.getModerateBannerPages(shard, bannerId);
        assertThat(actualExternalModeration, hasSize(expectedModerateBannerPages.size()));
        assertThat(actualExternalModeration, containsInAnyOrder(mapList(expectedModerateBannerPages,
                expected -> beanDiffer(expected).useCompareStrategy(COMPARE_STRATEGY))));
    }

    protected void assertBannerBsSyncedStatus(long bannerId, StatusBsSynced statusBsSynced) {
        OldBanner actualBanner = bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
        assertThat(actualBanner.getStatusBsSynced(), is(statusBsSynced));
    }

    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createAdGroup(activeCpmOutdoorAdGroup(null, null), defaultClient);
    }

    protected Long addCreative(VideoFormat... videoFormats) {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmOutdoorVideoAddition(defaultClient.getClientId(), creativeId);
        AdditionalData additionalData = creative.getAdditionalData();
        additionalData.setFormats(asList(videoFormats));

        creativeRepository.add(shard, singletonList(creative));
        return creativeId;
    }

    protected CpmOutdoorBannerInfo addBannerWithDefaultCreative(AdGroupInfo adGroup) {
        return addBannerWithDefaultCreative(adGroup, OldBannerStatusModerate.YES);
    }

    protected CpmOutdoorBannerInfo addBannerWithDefaultCreative(AdGroupInfo adGroup,
                                                                OldBannerStatusModerate statusModerate) {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(defaultClient, creativeId);
        return addBanner(adGroup, creativeId, statusModerate);
    }

    protected CpmOutdoorBannerInfo addBanner(AdGroupInfo adGroup, Long creativeId) {
        return addBanner(adGroup, creativeId, OldBannerStatusModerate.YES);
    }

    private CpmOutdoorBannerInfo addBanner(AdGroupInfo adGroup, Long creativeId,
                                           OldBannerStatusModerate statusModerate) {
        CpmOutdoorBannerInfo banner = steps.bannerSteps().createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroup.getCampaignId(), adGroup.getAdGroupId(), creativeId)
                        .withStatusModerate(statusModerate)
                        .withStatusBsSynced(StatusBsSynced.YES), adGroup);
        steps.bannerModerationVersionSteps().addBannerModerationVersion(banner.getShard(), banner.getBannerId(),
                BANNER_VERSION);
        return banner;
    }

    protected void addOutdoorPlacement(long pageId, OutdoorBlock... blocks) {
        placementSteps.addOutdoorPlacement(pageId, Arrays.asList(blocks));
    }

    /**
     * создать запись в moderate_banner_pages и в mod_reasons
     */
    protected Long addModerateBannerPageForBanner(CpmOutdoorBannerInfo banner, long pageId) {
        ModerateBannerPage moderateBannerPage = defaultModerateBannerPage(banner.getBannerId(), pageId, SENT);

        ModerateBannerPageInfo info =
                steps.moderateBannerPageSteps().createModerateBannerPage(banner, moderateBannerPage);

        steps.moderationReasonSteps().insertRejectReasonForModerateBannerPage(info);
        return info.getModerateBannerPageId();
    }

    protected OutdoorBlock createOutdoorBlock(long pageId, long blockId, Size size, Double duration) {
        BlockSize resolution = new BlockSize(size.width, size.height);
        return outdoorBlockWithOneSize(pageId, blockId, resolution, duration);
    }

    protected OutdoorBlock createOutdoorBlock(long pageId, long blockId, Size size) {
        return createOutdoorBlock(pageId, blockId, size, DEFAULT_OUTDOOR_VIDEO_DURATION);
    }

    protected VideoFormat createVideoFormat(Size size) {
        return new VideoFormat()
                .withWidth(size.width)
                .withHeight(size.height)
                .withType("video/mp4")
                .withUrl("http://abc.com/1");
    }

    protected ModerateBannerPage defaultModerateBannerPage(Long bannerId, Long pageId,
                                                           StatusModerateBannerPage status) {
        return defaultModerateBannerPage(bannerId, pageId, status, false);
    }

    protected ModerateBannerPage defaultModerateBannerPage(Long bannerId, Long pageId,
                                                           StatusModerateBannerPage status,
                                                           Boolean isRemoved) {
        return TestModerateBannerPages.defaultModerateBannerPage()
                .withBannerId(bannerId)
                .withPageId(pageId)
                .withVersion(BANNER_VERSION)
                .withComment(null)
                .withStatusModerate(status)
                .withIsRemoved(isRemoved);
    }

    static class Size {
        private int width;
        private int height;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
