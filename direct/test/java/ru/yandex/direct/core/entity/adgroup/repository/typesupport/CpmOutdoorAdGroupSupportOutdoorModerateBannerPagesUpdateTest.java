package ru.yandex.direct.core.entity.adgroup.repository.typesupport;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.service.OutdoorModerateBannerPagesUpdaterBaseTest;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmOutdoorAdGroupValidation.OUTDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage.READY;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpmOutdoorAdGroupSupportOutdoorModerateBannerPagesUpdateTest extends OutdoorModerateBannerPagesUpdaterBaseTest {

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private CpmOutdoorAdGroupSupport cpmOutdoorAdGroupSupport;

    @Before
    public void before() {
        super.before();
    }


    /**
     * adGroup 1:
     * pageId: 1, impId: 1 (для баннера подходит)
     * <p>
     * adGroup 2:
     * pageId: 1, impId: 1 (для баннера не подходит)
     */
    @Test
    public void updateModerateBannerPages_TwoAdGroupsOneOutdoorBlock_OneBannerIsFit() {
        // пейджи
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1);

        // группа1
        AdGroupInfo adGroup1 = createAdGroup();
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        long bannerId1 = addBanner(adGroup1, creativeId).getBannerId();

        // группа2
        AdGroupInfo adGroup2 = createAdGroup();
        VideoFormat videoFormat3 = createVideoFormat(SIZE_2);
        VideoFormat videoFormat4 = createVideoFormat(SIZE_3);
        Long creativeId2 = addCreative(videoFormat3, videoFormat4);
        long bannerId2 = addBanner(adGroup2, creativeId2).getBannerId();


        AppliedChanges<CpmOutdoorAdGroup> ac1 = getAppliedChanges(adGroup1, emptyList(),
                singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_1)));

        AppliedChanges<CpmOutdoorAdGroup> ac2 = getAppliedChanges(adGroup2, emptyList(),
                singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_1)));

        cpmOutdoorAdGroupSupport.updateModerateBannerPages(asList(ac1, ac2), dslContext);

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, emptyList());

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.YES);
    }


    /**
     * adGroup 1:
     * pageId: 1, impId: 1,2 (для баннера не подходит impId=1, но подходит impId=2)
     * <p>
     * adGroup 2:
     * pageId: 1, impId: 1 (для баннера не подходит impId=1 (но impId=2 подошел бы))
     */
    @Test
    public void updateModerateBannerPages_TwoAdGroupsTwoOutdoorBlocks_OneBannerIsFit() {
        // пейджи
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_4);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_3);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // группа1
        AdGroupInfo adGroup1 = createAdGroup();
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        long bannerId1 = addBanner(adGroup1, creativeId).getBannerId();

        // группа2
        AdGroupInfo adGroup2 = createAdGroup();
        VideoFormat videoFormat3 = createVideoFormat(SIZE_2);
        VideoFormat videoFormat4 = createVideoFormat(SIZE_3);
        Long creativeId2 = addCreative(videoFormat3, videoFormat4);
        long bannerId2 = addBanner(adGroup2, creativeId2).getBannerId();


        AppliedChanges<CpmOutdoorAdGroup> ac1 = getAppliedChanges(adGroup1, emptyList(),
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        AppliedChanges<CpmOutdoorAdGroup> ac2 = getAppliedChanges(adGroup2, emptyList(),
                singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_1)));

        cpmOutdoorAdGroupSupport.updateModerateBannerPages(asList(ac1, ac2), dslContext);

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, emptyList());

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.YES);
    }

    /**
     * adGroup 1:
     * pageId: 1, impId: 1,2 (для баннера подходит только impId=2)
     * <p>
     * adGroup 2:
     * pageId: 1, impId: 1,2 (для баннера подходит только impId=1)
     */
    @Test
    public void updateModerateBannerPages_TwoAdGroups_TwoBannersIsFit() {
        // пейджи
        OutdoorBlock outdoorBlock1 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_1, SIZE_1);
        OutdoorBlock outdoorBlock2 = createOutdoorBlock(PAGE_ID_1, BLOCK_ID_2, SIZE_2);
        addOutdoorPlacement(PAGE_ID_1, outdoorBlock1, outdoorBlock2);

        // группа1
        AdGroupInfo adGroup1 = createAdGroup();
        VideoFormat videoFormat1 = createVideoFormat(SIZE_1);
        VideoFormat videoFormat2 = createVideoFormat(SIZE_3);
        Long creativeId = addCreative(videoFormat1, videoFormat2);
        long bannerId1 = addBanner(adGroup1, creativeId).getBannerId();

        // группа2
        AdGroupInfo adGroup2 = createAdGroup();
        VideoFormat videoFormat3 = createVideoFormat(SIZE_2);
        VideoFormat videoFormat4 = createVideoFormat(SIZE_3);
        Long creativeId2 = addCreative(videoFormat3, videoFormat4);
        long bannerId2 = addBanner(adGroup2, creativeId2).getBannerId();


        AppliedChanges<CpmOutdoorAdGroup> ac1 = getAppliedChanges(adGroup1, emptyList(),
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        AppliedChanges<CpmOutdoorAdGroup> ac2 = getAppliedChanges(adGroup2, emptyList(),
                asList(createPageBlock(PAGE_ID_1, BLOCK_ID_1), createPageBlock(PAGE_ID_1, BLOCK_ID_2)));

        cpmOutdoorAdGroupSupport.updateModerateBannerPages(asList(ac1, ac2), dslContext);

        assertModerateBannerPages(bannerId1, singletonList(defaultModerateBannerPage(bannerId1, PAGE_ID_1, READY)));
        assertModerateBannerPages(bannerId2, singletonList(defaultModerateBannerPage(bannerId2, PAGE_ID_1, READY)));

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.NO);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.NO);
    }

    @Test
    public void updateModerateBannerPages_BannerMinusGeoExcludeAdGroupGeo_NoModerateBannerPages() {
        AdGroupInfo adGroup = createAdGroup();
        CpmOutdoorBannerInfo banner = addBannerWithDefaultCreative(adGroup);
        addBannerMinusGeo(banner, OUTDOOR_GEO_DEFAULT);

        AppliedChanges<CpmOutdoorAdGroup> changes = getAppliedChanges(adGroup, emptyList(),
                singletonList(createPageBlock(PAGE_ID_1, BLOCK_ID_1)));

        cpmOutdoorAdGroupSupport.updateModerateBannerPages(singletonList(changes), dslContext);

        assertModerateBannerPages(banner.getBannerId(), emptyList());
        assertBannerBsSyncedStatus(banner.getBannerId(), StatusBsSynced.YES);
    }

    @Test
    public void updateModerateBannerPages_EmptyAdGroups_NoExceptionThrown() {
        cpmOutdoorAdGroupSupport.updateModerateBannerPages(emptyList(), dslContext);
    }

    @Test
    public void updateModerateBannerPages_AdGroupWithoutBanners_NoExceptionThrown() {
        AdGroupInfo adGroup = createAdGroup();
        AppliedChanges<CpmOutdoorAdGroup> ac = getAppliedChanges(adGroup, emptyList(), singletonList(PAGE_BLOCK_1));
        cpmOutdoorAdGroupSupport.updateModerateBannerPages(singleton(ac), dslContext);
    }

    @Test
    public void updateModerateBannerPages_ThreeBanners_OneUpdated() {

        // статус не YES
        AdGroupInfo adGroup1 = createAdGroup();
        Long bannerId1 = addBannerWithDefaultCreative(adGroup1, OldBannerStatusModerate.NO).getBannerId();
        AppliedChanges<CpmOutdoorAdGroup> ac1 = getAppliedChanges(adGroup1, emptyList(), singletonList(PAGE_BLOCK_1));

        // pageIds не изменены
        AdGroupInfo adGroup2 = createAdGroup();
        Long bannerId2 = addBannerWithDefaultCreative(adGroup2, OldBannerStatusModerate.YES).getBannerId();
        AppliedChanges<CpmOutdoorAdGroup> ac2 = getAppliedChanges(adGroup2, emptyList(), emptyList());

        // все ок
        AdGroupInfo adGroup3 = createAdGroup();
        Long bannerId3 = addBannerWithDefaultCreative(adGroup3, OldBannerStatusModerate.YES).getBannerId();
        AppliedChanges<CpmOutdoorAdGroup> ac3 = getAppliedChanges(adGroup3, emptyList(), singletonList(PAGE_BLOCK_1));

        cpmOutdoorAdGroupSupport.updateModerateBannerPages(asList(ac1, ac2, ac3), dslContext);

        assertModerateBannerPages(bannerId1, emptyList());
        assertModerateBannerPages(bannerId2, emptyList());
        assertModerateBannerPages(bannerId3, singletonList(defaultModerateBannerPage(bannerId3, PAGE_ID_1, READY)));

        assertBannerBsSyncedStatus(bannerId1, StatusBsSynced.YES);
        assertBannerBsSyncedStatus(bannerId2, StatusBsSynced.YES);
        assertBannerBsSyncedStatus(bannerId3, StatusBsSynced.NO);
    }

    private AppliedChanges<CpmOutdoorAdGroup> getAppliedChanges(AdGroupInfo adGroupInfo, List<PageBlock> oldPages,
                                                                List<PageBlock> newPages) {
        CpmOutdoorAdGroup adGroup = (CpmOutdoorAdGroup) adGroupInfo.getAdGroup();

        adGroup.setPageBlocks(oldPages);
        ModelChanges<CpmOutdoorAdGroup> modelChanges =
                ModelChanges.build(adGroup.getId(), CpmOutdoorAdGroup.class, CpmOutdoorAdGroup.PAGE_BLOCKS, newPages);
        return modelChanges.applyTo(adGroup);
    }

    private void addBannerMinusGeo(CpmOutdoorBannerInfo banner, List<Long> minusGeo) {
        testBannerRepository.addMinusGeo(shard, banner.getBannerId(),
                BannersMinusGeoType.current, minusGeo.stream().map(Object::toString).collect(joining(",")));
    }
}
