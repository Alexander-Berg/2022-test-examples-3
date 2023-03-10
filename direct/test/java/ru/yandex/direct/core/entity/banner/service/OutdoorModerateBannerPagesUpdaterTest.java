package ru.yandex.direct.core.entity.banner.service;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.banner.container.OutdoorModerateBannerPagesUpdateParams;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmOutdoorAdGroupValidation.OUTDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage.READY;
import static ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage.SENT;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_DURATION;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OutdoorModerateBannerPagesUpdaterTest extends OutdoorModerateBannerPagesUpdaterBaseTest {

    @Autowired
    private OutdoorModerateBannerPagesUpdater updater;

    private Long defaultModerateBannerPageId1;
    private Long defaultModerateBannerPageId2;
    private AdGroupInfo defaultAdGroup;
    private CpmOutdoorBannerInfo defaultBanner;
    private Long defaultBannerId;
    private List<PageBlock> defaultNewPageBlocks;


    @Before
    public void before() {
        super.before();
        defaultAdGroup = createAdGroup();
        initDefaultBanner();
    }

    private void initDefaultBanner() {
        defaultBanner = addBannerWithDefaultCreative(defaultAdGroup);
        defaultBannerId = defaultBanner.getBannerId();
        defaultModerateBannerPageId1 = addModerateBannerPageForBanner(defaultBanner, PAGE_ID_1);
        defaultModerateBannerPageId2 = addModerateBannerPageForBanner(defaultBanner, PAGE_ID_2);
        defaultNewPageBlocks = asList(PAGE_BLOCK_2, PAGE_BLOCK_3);
    }

    @Test
    public void updateModerateBannerPages_ModerateBannerPagesUpdated() {
        updateModerateBannerPages(defaultBanner, asList(PAGE_BLOCK_2, PAGE_BLOCK_3));

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_3, READY)
        ));
    }

    @Test
    public void updateModerateBannerPages_DeletedPagesOnly_ModerateBannerPagesUpdated() {
        updateModerateBannerPages(defaultBanner, singletonList(PAGE_BLOCK_2));

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT)
        ));
    }

    @Test
    public void updateModerateBannerPages_AddedPagesOnly_ModerateBannerPagesUpdated() {
        updateModerateBannerPages(defaultBanner, asList(PAGE_BLOCK_1, PAGE_BLOCK_2, PAGE_BLOCK_3));

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_3, READY)
        ));
    }

    @Test
    public void updateModerateBannerPages_NothingAddedDeleted_ModerateBannerPagesNotUpdated() {
        updateModerateBannerPages(defaultBanner, asList(PAGE_BLOCK_1, PAGE_BLOCK_2));

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT)
        ));
    }

    @Test
    public void updateModerateBannerPages_BannerStatusBsSyncedUpdated() {
        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks);
        assertBannerBsSyncedStatus(defaultBannerId, StatusBsSynced.NO);
    }

    @Test
    public void updateModerateBannerPages_ModReasonsUpdated() {
        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks);

        assertModerateBannerPageModReasons(
                asList(defaultModerateBannerPageId1, defaultModerateBannerPageId2),
                asList(defaultModerateBannerPageId1, defaultModerateBannerPageId2)
        );
    }

    @Test
    public void updateModerateBannerPages_AdGroupGeoMoreThanBannerMinusGeo_ModerateBannerPagesAdded() {
        List<Long> adGroupGeo = asList(RUSSIA_REGION_ID, UKRAINE_REGION_ID, BY_REGION_ID);
        List<Long> bannerMinusGeo = asList(RUSSIA_REGION_ID, UKRAINE_REGION_ID);

        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks, adGroupGeo, bannerMinusGeo);

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_3, READY)
        ));
    }

    @Test
    public void updateModerateBannerPages_AdGroupGeoLessThanBannerMinusGeo_ModerateBannerPagesMarkDeleted() {
        List<Long> adGroupGeo = asList(RUSSIA_REGION_ID, BY_REGION_ID);
        List<Long> bannerMinusGeo = asList(RUSSIA_REGION_ID, UKRAINE_REGION_ID, BY_REGION_ID);

        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks, adGroupGeo, bannerMinusGeo);

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT, true)
        ));
    }

    @Test
    public void updateModerateBannerPages_AdGroupGeoEqualBannerMinusGeo_ModerateBannerPagesMarkDeleted() {
        List<Long> adGroupGeo = asList(RUSSIA_REGION_ID, BY_REGION_ID, UKRAINE_REGION_ID);
        List<Long> bannerMinusGeo = asList(RUSSIA_REGION_ID, UKRAINE_REGION_ID, BY_REGION_ID);

        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks, adGroupGeo, bannerMinusGeo);

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT, true)
        ));
    }

    @Test
    public void updateModerateBannerPages_AdGroupGeoCityEqualBannerMinusGeoCountry_ModerateBannerPagesMarkDeleted() {
        List<Long> adGroupGeo = singletonList(MOSCOW_REGION_ID);
        List<Long> bannerMinusGeo = singletonList(RUSSIA_REGION_ID);

        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks, adGroupGeo, bannerMinusGeo);

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT, true)
        ));
    }

    @Test
    public void updateModerateBannerPages_AdGroupGeoWithMinusRegionLessThanBannerMinusGeo_ModerateBannerPagesMarkDeleted() {
        List<Long> adGroupGeo = asList(RUSSIA_REGION_ID, -MOSCOW_REGION_ID);
        List<Long> bannerMinusGeo = singletonList(RUSSIA_REGION_ID);

        updateModerateBannerPages(defaultBanner, defaultNewPageBlocks, adGroupGeo, bannerMinusGeo);

        assertModerateBannerPages(defaultBannerId, asList(
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT, true)
        ));
    }

    @Test(expected = IllegalStateException.class)
    public void updateModerateBannerPages_BannerWithoutCreative_ExceptionThrown() {
        CpmOutdoorBannerInfo bannerWithoutCreative = addBanner(defaultAdGroup, null);
        updateModerateBannerPages(bannerWithoutCreative, defaultNewPageBlocks);
    }

    @Test
    public void updateModerateBannerPages_CreativeFormatWithNullSize_OneFormatIgnoredOneIsFit() {
        OutdoorBlock outdoorBlock = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock);

        VideoFormat normalVideoFormat = createVideoFormat(SIZE_1);
        VideoFormat playlistVideoFormat = new VideoFormat().withType("playlist");

        Long creativeId = addCreative(normalVideoFormat, playlistVideoFormat);
        long bannerId = addBanner(defaultAdGroup, creativeId).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams =
                createUpdateParams(singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_1)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(defaultModerateBannerPage(bannerId, PAGE_ID_1, READY)));

        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.NO);
    }

    /**
     * pageId: 1, impId: 1, 2
     * ?????? ???????????? ?????????????? ????????????????, ?????? ?????????????? ??????
     */
    @Test
    public void updateModerateBannerPages_OneAdGroupTwoBanners_OneBannerIsFit() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // ????????????1
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_2);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        long bannerId1 = addBanner(defaultAdGroup, creativeId).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams1 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        // ????????????2
        VideoFormat videoFormat3 = createVideoFormat(SIZE_3);
        VideoFormat videoFormat4 = createVideoFormat(SIZE_4);
        Long creativeId2 = addCreative(videoFormat3, videoFormat4);
        long bannerId2 = addBanner(defaultAdGroup, creativeId2).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams2 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));


        updateModerateBannerPages(asList(bannerId1, bannerId2), asList(updateParams1, updateParams2));

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, emptyList());

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.YES);
    }

    /**
     * ??????????????: 1x1, 1s
     * ????????_1: 1x1, 2s
     * ????????_2: 2x2, 1s
     * ??????????????????: ???????????? ???? ????????????????
     */
    @Test
    public void updateModerateBannerPages_MergedBlocksIsFitted_BannerIsNotFit() {
        double badDuration = 1.0;
        double goodDuration = DEFAULT_OUTDOOR_VIDEO_DURATION;
        Size badSize = SIZE_2;
        Size goodSize = SIZE_1;

        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, goodSize, badDuration);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, badSize, goodDuration);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        VideoFormat videoFormat1 = createVideoFormat(goodSize);
        Long creativeId = addCreative(videoFormat1);
        long bannerId1 = addBanner(defaultAdGroup, creativeId).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams1 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(singletonList(bannerId1), singletonList(updateParams1));
        assertModerateBannerPages(bannerId1, emptyList());
        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.YES);
    }

    /**
     * pageId: 1, impId: 1, 2
     * ?????? ?????????????? ?????????????? ???????????????? ???????????? impId=1, ?????? ?????????????? ???????????? impId=2
     */
    @Test
    public void updateModerateBannerPages_OneAdGroupTwoBanners_TwoBannersIsFit() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // ????????????1
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        long bannerId1 = addBanner(defaultAdGroup, creativeId).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams1 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        // ????????????2
        VideoFormat videoFormat3 = createVideoFormat(SIZE_2);
        VideoFormat videoFormat4 = createVideoFormat(SIZE_4);
        Long creativeId2 = addCreative(videoFormat3, videoFormat4);
        long bannerId2 = addBanner(defaultAdGroup, creativeId2).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams2 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(asList(bannerId1, bannerId2), asList(updateParams1, updateParams2));

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, singletonList(defaultModerateBannerPage(bannerId2, PAGE_ID_1, READY)));

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.NO);
    }

    /**
     * pageId: 1, impId: 1
     * pageId: 2, impId: 1
     * ?????? ?????????????? ?????????????? ???????????????? ???????????? pageId=2, ?????? ?????????????? ???????????? pageId=1
     */
    @Test
    public void updateModerateBannerPages_TwoBannersTwoPlacements_TwoBannersIsFit() {
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_2, BLOCK_ID_1, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1);
        addOutdoorPlacement(PAGE_ID_2, outdoorBlock2);

        // ????????????1
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        long bannerId1 = addBanner(defaultAdGroup, creativeId).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams1 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_2, BLOCK_ID_1)));

        // ????????????2
        VideoFormat videoFormat3 = createVideoFormat(SIZE_2);
        VideoFormat videoFormat4 = createVideoFormat(SIZE_4);
        Long creativeId2 = addCreative(videoFormat3, videoFormat4);
        long bannerId2 = addBanner(defaultAdGroup, creativeId2).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams2 = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_2, BLOCK_ID_1)));

        updateModerateBannerPages(asList(bannerId1, bannerId2), asList(updateParams1, updateParams2));

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, singletonList(defaultModerateBannerPage(bannerId2, PAGE_ID_2, READY)));

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.NO);
    }

    /**
     * ?? ?????????????? ?????????????? moderate_banner_pages: pageId=1;
     * ?? ???????????? new page blocks: pageId=1, impId=2;
     * impId=2 ???? ???????????????? => ???????????????? ?????????????????? pageId=1
     */
    @Test
    public void updateModerateBannerPages_BannerWithExistModeration_MarkDeletedModerateBannerPage() {
        // ????????????
        OutdoorBlock oldOutdoorBlock = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock newOutdoorBlock = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, oldOutdoorBlock, newOutdoorBlock);

        // ????????????
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        CpmOutdoorBannerInfo banner = addBanner(defaultAdGroup, creativeId);
        long bannerId = banner.getBannerId();
        Long moderateBannerPageId = addModerateBannerPageForBanner(banner, PAGE_ID_1);

        OutdoorModerateBannerPagesUpdateParams updateParams =
                createUpdateParams(singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(
                defaultModerateBannerPage(bannerId, PAGE_ID_1, SENT, true)
        ));
        assertModerateBannerPageModReasons(singleton(moderateBannerPageId), singletonList(moderateBannerPageId));
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.NO);
    }

    /**
     * ?? ?????????????? ?????????????? moderate_banner_pages: pageId=1;
     * ?? ???????????? new page blocks: pageId=1, impId=2;
     * impId=2 ???????????????? => ???????????? ???? ????????????????
     */
    @Test
    public void updateModerateBannerPages_BannerWithExistModeration_NothingChanged() {
        // ????????????
        OutdoorBlock oldOutdoorBlock = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock newOutdoorBlock = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, oldOutdoorBlock, newOutdoorBlock);

        // ????????????
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_2);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        CpmOutdoorBannerInfo banner = addBanner(defaultAdGroup, creativeId);
        long bannerId = banner.getBannerId();
        Long moderateBannerPageId = addModerateBannerPageForBanner(banner, PAGE_ID_1);

        OutdoorModerateBannerPagesUpdateParams updateParams =
                createUpdateParams(singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(defaultModerateBannerPage(bannerId, PAGE_ID_1, SENT)));
        assertModerateBannerPageModReasons(singleton(moderateBannerPageId), singleton(moderateBannerPageId));
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.YES);
    }

    /**
     * ?? ?????????????? ?????????????? moderate_banner_pages: pageId=1, 2
     * ?? ???????????? new pages: pageId=2,3;
     * ?? ????: 2 ???? ????????????????????, 1 ??????????????????, 3 ??????????????????
     */
    @Test
    public void updateModerateBannerPages_BannerWithExistModeration_OneModerationMarkDeletedAndOneAdded() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_2, BLOCK_ID_1, SIZE_2);
        OutdoorBlock outdoorBlock3 = createOutdoorBlock(PAGE_ID_3, BLOCK_ID_1, SIZE_3);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1);
        addOutdoorPlacement(PAGE_ID_2, outdoorBlock2);
        addOutdoorPlacement(PAGE_ID_3, outdoorBlock3);

        // ????????????
        VideoFormat videoFormat1 = createVideoFormat(SIZE_2);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        CpmOutdoorBannerInfo banner = addBanner(defaultAdGroup, creativeId);
        long bannerId = banner.getBannerId();
        Long moderateBannerPageId1 = addModerateBannerPageForBanner(banner, PAGE_ID_1);
        Long moderateBannerPageId2 = addModerateBannerPageForBanner(banner, PAGE_ID_2);

        OutdoorModerateBannerPagesUpdateParams updateParams = createUpdateParams(
                asList(createPageBlock(PAGE_ID_2, BLOCK_ID_1), createPageBlock(PAGE_ID_3, BLOCK_ID_1)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, asList(
                defaultModerateBannerPage(bannerId, PAGE_ID_1, SENT, true),
                defaultModerateBannerPage(bannerId, PAGE_ID_2, SENT),
                defaultModerateBannerPage(bannerId, PAGE_ID_3, READY)
        ));
        assertModerateBannerPageModReasons(asList(moderateBannerPageId1, moderateBannerPageId2),
                asList(moderateBannerPageId1, moderateBannerPageId2));
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.NO);
    }

    /**
     * ?? ???????????? 2 ??????????:
     * BLOCK_ID_1 - ???????????????? ?????? ?????????????? ??????????????,
     * BLOCK_ID_2 - ???? ????????????????
     * ?? ?????????????? ?????????? ??????????: [BLOCK_ID_1, BLOCK_ID_2]
     * ?? ?????????????? ?????????????? moderate_banner_pages: PAGE_ID_1
     * => ?? moderate_banner_pages ???????? ???????????? ?? PAGE_ID_1
     */
    @Test
    public void updateModerateBannerPages_HadGoodBlockAddBadBlock() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // ????????????
        AdGroupInfo adGroup = createAdGroup();
        VideoFormat videoFormat = createVideoFormat(SIZE_1);
        Long creativeId = addCreative(videoFormat);
        CpmOutdoorBannerInfo banner = addBanner(adGroup, creativeId);
        long bannerId = banner.getBannerId();
        addModerateBannerPageForBanner(banner, PAGE_ID_1);

        OutdoorModerateBannerPagesUpdateParams updateParams = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(defaultModerateBannerPage(bannerId, PAGE_ID_1, SENT)));
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.YES);
    }

    /**
     * ?? ???????????? 2 ??????????:
     * BLOCK_ID_1 - ???????????????? ?????? ?????????????? ??????????????,
     * BLOCK_ID_2 - ???? ????????????????
     * ?? ?????????????? ?????????? ??????????: [BLOCK_ID_1, BLOCK_ID_2]
     * ?? ?????????????? ?????????????? moderate_banner_pages: -
     * => ?? moderate_banner_pages ???????? ???????????? ?? PAGE_ID_1
     */
    @Test
    public void updateModerateBannerPages_HadBadBlockAddGoodBlock() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // ????????????
        AdGroupInfo adGroup = createAdGroup();
        VideoFormat videoFormat = createVideoFormat(SIZE_1);
        Long creativeId = addCreative(videoFormat);
        long bannerId = addBanner(adGroup, creativeId).getBannerId();

        OutdoorModerateBannerPagesUpdateParams updateParams = createUpdateParams(
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(defaultModerateBannerPage(bannerId, PAGE_ID_1, READY)));
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.NO);
    }

    /**
     * ?? ???????????? 2 ??????????:
     * BLOCK_ID_1 - ???????????????? ?????? ?????????????? ??????????????,
     * BLOCK_ID_2 - ???? ????????????????
     * ?? ?????????????? ?????????? ??????????: [BLOCK_ID_1]
     * ?? ?????????????? ?????????????? moderate_banner_pages: PAGE_ID_1
     * => ?? moderate_banner_pages ???????? ???????????? ?? BLOCK_ID_1
     */
    @Test
    public void updateModerateBannerPages_RemoveBadBlock() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // ????????????
        AdGroupInfo adGroup = createAdGroup();
        VideoFormat videoFormat = createVideoFormat(SIZE_1);
        Long creativeId = addCreative(videoFormat);
        CpmOutdoorBannerInfo banner = addBanner(adGroup, creativeId);
        long bannerId = banner.getBannerId();
        addModerateBannerPageForBanner(banner, PAGE_ID_1);

        OutdoorModerateBannerPagesUpdateParams updateParams = createUpdateParams(
                singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_1)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(defaultModerateBannerPage(bannerId, PAGE_ID_1, SENT)));
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.YES);
    }

    /**
     * ?? ???????????? 2 ??????????:
     * BLOCK_ID_1 - ???????????????? ?????? ?????????????? ??????????????,
     * BLOCK_ID_2 - ???? ????????????????
     * ?? ?????????????? ?????????? ??????????: [BLOCK_ID_2]
     * ?? ?????????????? ?????????????? moderate_banner_pages: PAGE_ID_1
     * => ?? moderate_banner_pages ?????? ??????????????
     */
    @Test
    public void updateModerateBannerPages_RemoveGoodBlock() {
        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // ????????????
        AdGroupInfo adGroup = createAdGroup();
        VideoFormat videoFormat = createVideoFormat(SIZE_1);
        Long creativeId = addCreative(videoFormat);
        CpmOutdoorBannerInfo banner = addBanner(adGroup, creativeId);
        long bannerId = banner.getBannerId();
        addModerateBannerPageForBanner(banner, PAGE_ID_1);

        OutdoorModerateBannerPagesUpdateParams updateParams = createUpdateParams(
                singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        updateModerateBannerPages(singletonList(bannerId), singletonList(updateParams));

        assertModerateBannerPages(bannerId, singletonList(
                defaultModerateBannerPage(bannerId, PAGE_ID_1, SENT, true))
        );
        assertBannerBsSyncedStatus(bannerId, StatusBsSynced.NO);
    }


    /**
     * Default ????????????. ???????????????? PAGE_ID_3 ?? ?????????? ???????????????????????? duration:
     * 1) ????????????????????????: ???????????????????? ???? ?????????????? ???? 0.1??
     * 2) ????????????????????: ???????????????????? ???? ?????????????? ???? 0.01??
     */
    @Test
    public void updateModerateBannerPages_NotRoundedBlockDuration_ModerateBannerPageAdded() {
        double badDuration = DEFAULT_OUTDOOR_VIDEO_DURATION - 0.1;
        double goodDuration = DEFAULT_OUTDOOR_VIDEO_DURATION - 0.01;

        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_3, BLOCK_ID_1, DEFAULT_SIZE, badDuration);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_3, BLOCK_ID_2, DEFAULT_SIZE, goodDuration);
        addOutdoorPlacement(PAGE_ID_3, outdoorBlock1, outdoorBlock2);
        PageBlock goodPageBlock = createPageBlock(PAGE_ID_3, BLOCK_ID_1);
        PageBlock badPageBlock = createPageBlock(PAGE_ID_3, BLOCK_ID_2);

        OutdoorModerateBannerPagesUpdateParams updateParams =
                createUpdateParams(asList(goodPageBlock, badPageBlock));

        updateModerateBannerPages(singletonList(defaultBannerId), singletonList(updateParams));

        assertModerateBannerPages(defaultBannerId,
                asList(
                        defaultModerateBannerPage(defaultBannerId, PAGE_ID_1, SENT, true),
                        defaultModerateBannerPage(defaultBannerId, PAGE_ID_2, SENT, true),
                        defaultModerateBannerPage(defaultBannerId, PAGE_ID_3, READY)
                ));

        assertBannerBsSyncedStatus(defaultBannerId, StatusBsSynced.NO);
    }

    /**
     * pageId: 1, impId: 1 (good duration), 2 (bad duration)
     * ?????? ?????????????? ?????????????? impId: 1 ???????????????? => pageId ????????????????, ?????? ?????????????? impId: 1 ???? ???????????????? => pageId ???? ????????????????
     */
    @Test
    public void updateModerateBannerPages_TwoBannersWithDefaultDuration_OneBannerIsNotFitByPageBlockDuration() {
        double badDuration = DEFAULT_OUTDOOR_VIDEO_DURATION - 0.1;
        double goodDuration = DEFAULT_OUTDOOR_VIDEO_DURATION - 0.01;

        // ????????????
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, DEFAULT_SIZE, badDuration);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, DEFAULT_SIZE, goodDuration);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        PageBlock pageBlockWithBadDuration = createPageBlock(PAGE_ID_1, BLOCK_ID_1);
        PageBlock pageBlockWithGoodDuration = createPageBlock(PAGE_ID_1, BLOCK_ID_2);

        // ????????????1
        long bannerId1 = addBannerWithDefaultCreative(defaultAdGroup).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams1 =
                createUpdateParams(asList(pageBlockWithBadDuration, pageBlockWithGoodDuration));

        // ????????????2
        long bannerId2 = addBannerWithDefaultCreative(defaultAdGroup).getBannerId();
        OutdoorModerateBannerPagesUpdateParams updateParams2 =
                createUpdateParams(singletonList(pageBlockWithBadDuration));


        updateModerateBannerPages(asList(bannerId1, bannerId2), asList(updateParams1, updateParams2));

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, emptyList());

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.YES);
    }

    private void updateModerateBannerPages(List<Long> banners,
                                           List<OutdoorModerateBannerPagesUpdateParams> updateParams) {
        Map<Long, OutdoorModerateBannerPagesUpdateParams> updateParamsMap = new HashMap<>();
        Iterator<OutdoorModerateBannerPagesUpdateParams> updateParamsIterator = updateParams.iterator();
        banners.forEach(bannerId -> updateParamsMap.put(bannerId, updateParamsIterator.next()));
        updater.updateModerateBannerPages(updateParamsMap, dslContext);
    }

    private void updateModerateBannerPages(CpmOutdoorBannerInfo banner, List<PageBlock> pages) {
        updateModerateBannerPages(banner, pages, OUTDOOR_GEO_DEFAULT, emptyList());
    }

    private void updateModerateBannerPages(CpmOutdoorBannerInfo banner, List<PageBlock> adGroupPages,
                                           List<Long> adGroupGeo, List<Long> bannerMinusGeo) {
        Map<Long, OutdoorModerateBannerPagesUpdateParams> updateParams = ImmutableMap.of(
                banner.getBannerId(),
                createUpdateParams(adGroupPages, adGroupGeo, bannerMinusGeo)
        );

        updater.updateModerateBannerPages(updateParams, dslContext);
    }

    private OutdoorModerateBannerPagesUpdateParams createUpdateParams(List<PageBlock> pages) {
        return createUpdateParams(pages, OUTDOOR_GEO_DEFAULT, emptyList());
    }

    private OutdoorModerateBannerPagesUpdateParams createUpdateParams(List<PageBlock> pages, List<Long> adGroupGeo,
                                                                      List<Long> bannerMinusGeo) {
        return OutdoorModerateBannerPagesUpdateParams.builder()
                .withAdGroupPageBlocks(pages)
                .withAdGroupGeo(adGroupGeo)
                .withBannerMinusGeo(bannerMinusGeo)
                .withBannerVersion(BANNER_VERSION)
                .build();
    }
}
