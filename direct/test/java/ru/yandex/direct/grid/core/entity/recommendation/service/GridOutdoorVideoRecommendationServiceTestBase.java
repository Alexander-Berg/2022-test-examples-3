package ru.yandex.direct.grid.core.entity.recommendation.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jooq.DSLContext;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.repository.typesupport.AdGroupTypeSupportDispatcher;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.placements.model1.BlockSize;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.testing.data.TestFfmpegResolution;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationWithKpi;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.utils.Counter;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService.GDI_RECOMMENDATION_GD_RECOMMENDATION_FUNCTION;

public abstract class GridOutdoorVideoRecommendationServiceTestBase {
    private static final long DEFAULT_BLOCK_ID = 1;

    protected static final String DEFAULT_RATIO = TestFfmpegResolution.R_21_360p.getRatio();
    protected static final Size SIZE_1 = new Size(TestFfmpegResolution.R_21_360p);
    protected static final Size SIZE_2 = new Size(TestFfmpegResolution.R_21_720p);
    protected static final Size SIZE_3 = new Size(TestFfmpegResolution.R_21_900p);
    protected static final double DEFAULT_DURATION = 5.44;
    protected static final double ROUNDED_DEFAULT_DURATION = 5.4;
    protected static final double DURATION_2 = 5.95;
    protected static final double DURATION_3 = 6.04;
    protected static final double DURATION_4 = 6.54;
    protected static final double ROUNDED_DURATION_2 = 6.0;
    protected static final double ROUNDED_DURATION_4 = 6.5;

    @Autowired
    private Steps steps;
    @Autowired
    private PlacementSteps placementSteps;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private AdGroupTypeSupportDispatcher adGroupTypeSupportDispatcher;
    @Autowired
    private DslContextProvider dslContextProvider;

    private DSLContext dslContext;
    private ClientId defaultClientId;
    private Counter pageIdCounter = new Counter(1);
    protected int shard;
    protected long defaultClientIdLong;
    protected AdGroupInfo defaultAdGroup;
    protected Long defaultAdGroupId;

    @Before
    public void before() {
        ClientInfo defaultClientInfo = steps.clientSteps().createDefaultClient();
        defaultClientId = defaultClientInfo.getClientId();
        defaultClientIdLong = defaultClientId.asLong();
        shard = defaultClientInfo.getShard();
        dslContext = dslContextProvider.ppc(shard);
        defaultAdGroup = steps.adGroupSteps().createAdGroup(activeCpmOutdoorAdGroup(null, null), defaultClientInfo);
        defaultAdGroupId = defaultAdGroup.getAdGroupId();

        placementSteps.clearPlacements();
    }

    protected void addPlacementsToAdGroup(long adGroupId, double commonDuration, Size... sizes) {
        List<PageBlock> newPageBlockList = new ArrayList<>();
        for (Size placementSize : sizes) {
            PageBlock pageBlock = new PageBlock()
                    .withPageId((long) pageIdCounter.next())
                    .withImpId(DEFAULT_BLOCK_ID);

            createPlacement(pageBlock, placementSize, commonDuration);
            newPageBlockList.add(pageBlock);
        }

        //добавить существующие новые пейджблоки к существующим
        List<PageBlock> currentPageBlocks = adGroupRepository
                .getAdGroupsPageTargetByAdGroupId(shard, singleton(adGroupId))
                .compute(adGroupId, (key, value) -> value != null ? value : emptyList());

        newPageBlockList.addAll(currentPageBlocks);

        CpmOutdoorAdGroup cpmOutdoorAdGroup = new CpmOutdoorAdGroup().withId(adGroupId);
        AppliedChanges<CpmOutdoorAdGroup> appliedChanges = new ModelChanges<>(adGroupId,
                CpmOutdoorAdGroup.class)
                .process(newPageBlockList, CpmOutdoorAdGroup.PAGE_BLOCKS)
                .applyTo(cpmOutdoorAdGroup);

        List<AppliedChanges<AdGroup>> appliedChangesList = singletonList(appliedChanges.castModelUp(AdGroup.class));
        adGroupTypeSupportDispatcher.updateAdGroups(dslContext, defaultClientId, appliedChangesList);
    }

    private void createPlacement(PageBlock pageBlock, Size size, Double duration) {
        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(pageBlock.getPageId(), pageBlock.getImpId(),
                new BlockSize(size.getWidth(), size.getHeight()), duration);

        placementSteps.addOutdoorPlacement(pageBlock.getPageId(), singletonList(outdoorBlock));
    }

    protected long addBannerToAdGroup(AdGroupInfo adGroup, double duration, Size... sizes) {
        List<VideoFormat> videoFormats = Arrays.stream(sizes).map(Size::toVideoFormat).collect(toList());
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmOutdoorVideoAddition(defaultClientId, creativeId);
        AdditionalData additionalData = creative.getAdditionalData();
        additionalData.setFormats(videoFormats);
        additionalData.setDuration(BigDecimal.valueOf(duration));
        creativeRepository.add(shard, singletonList(creative));

        CpmOutdoorBannerInfo bannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(adGroup.getCampaignId(), adGroup.getAdGroupId(), creativeId), adGroup);
        steps.bannerModerationVersionSteps().addBannerModerationVersion(bannerInfo.getShard(),
                bannerInfo.getBannerId(), 1L);

        return bannerInfo.getBannerId();
    }

    protected <T> T getKpi(GdiRecommendation gdiRecommendation) {
        GdRecommendationWithKpi gdRecommendation =
                (GdRecommendationWithKpi) GDI_RECOMMENDATION_GD_RECOMMENDATION_FUNCTION.apply(gdiRecommendation);
        return (T) gdRecommendation.getKpi();
    }

    protected static class Size {
        private int width;
        private int height;

        public Size(TestFfmpegResolution ffmpegResolution) {
            this.width = ffmpegResolution.getWidth();
            this.height = ffmpegResolution.getHeight();
        }

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        VideoFormat toVideoFormat() {
            return new VideoFormat()
                    .withWidth(width)
                    .withHeight(height)
                    .withType("video/mp4")
                    .withUrl("http://ya.ru/1");
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

}
