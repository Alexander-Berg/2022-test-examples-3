package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.banner.type.creative.model.CreativeSizeWithExpand;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoAspect;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoHtml;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoImage;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoSound;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoText;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoVideo;
import ru.yandex.direct.core.entity.creative.model.SourceMediaType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.model.YabsData;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.collectCampaignCreativeSizesWithExpand;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.CPC_VIDEO_LAYOUT_IDS;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.CPM_AUDIO_LAYOUT_IDS;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.CPM_INDOOR_VIDEO_LAYOUT_IDS;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.CPM_OUTDOOR_VIDEO_LAYOUT_IDS;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.CPM_OVERLAY_LAYOUT_IDS;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.CPM_VIDEO_ADDITION_LAYOUT_IDS;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.NON_SKIPPABLE_CPM_VIDEO_LAYOUT_ID;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.TEXT_VIDEO_ADDITION_LAYOUT_IDS;

public class TestCreatives {

    public static final long DEFAULT_VIDEO_DURATION = 4;

    public static final int DEFAULT_OUTDOOR_VIDEO_FORMAT_WIDTH = 1440;
    public static final int DEFAULT_OUTDOOR_VIDEO_FORMAT_HEIGHT = 720;
    public static final double DEFAULT_OUTDOOR_VIDEO_DURATION = 2.5;

    public static final int DEFAULT_INDOOR_VIDEO_FORMAT_WIDTH = 1920;
    public static final int DEFAULT_INDOOR_VIDEO_FORMAT_HEIGHT = 1080;
    public static final double DEFAULT_INDOOR_VIDEO_DURATION = 1.5;

    public static final long DEFAULT_AUDIO_FORMAT_WIDTH = 1920;
    public static final long DEFAULT_AUDIO_FORMAT_HEIGHT = 1080;
    public static final long DEFAULT_AUDIO_DURATION = 3;

    public static final String DEFAULT_EXPANDED_PREVIEW_URL = "https://storage.mds.yandex.net/get-canvas-html5-test/" +
            "995356/fe9880be-4e99-4a06-b86f-e090d2fb8e63/demo_html5_extended_2/video.html";

    private TestCreatives() {
    }

    public static Creative defaultVideoAddition(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("video_addition_creative")
                .withStockCreativeId(creativeId != null ? creativeId + 10 : null)  // чтобы отличался от creativeId
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                .withLayoutId(TEXT_VIDEO_ADDITION_LAYOUT_IDS.lowerEndpoint() + 1L)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withDuration(DEFAULT_VIDEO_DURATION)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withModerationInfo(new ModerationInfo()
                        .withHtml(
                                new ModerationInfoHtml()
                                        .withUrl("http://mds.yandex.ru/somepath/preview?compact=1"))
                        .withVideos(
                                singletonList(
                                        new ModerationInfoVideo()
                                                .withStockId("12345678123456")
                                                .withUrl("http://mds.yandex.ru/somepath/some-video.mp4")
                                )
                        )
                        .withContentId(123456L)
                        .withBgrcolor("#234354")
                )
                .withAdditionalData(
                        new AdditionalData()
                                .withDuration(BigDecimal.valueOf(15.034))
                                .withFormats(
                                        singletonList(
                                                new VideoFormat()
                                                        .withType("some-type")
                                                        .withWidth(640)
                                                        .withHeight(480)
                                                        .withUrl("http://mds.yandex.ru/somepath/some-video.flv")
                                        )
                                )
                )
                .withHasPackshot(true)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withIsBannerstoragePredeployed(false);
    }

    public static Creative defaultCpmVideoAddition(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("cpm_video_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.CPM_VIDEO_CREATIVE)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withDuration(DEFAULT_VIDEO_DURATION)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withLayoutId(CPM_VIDEO_ADDITION_LAYOUT_IDS.span().lowerEndpoint())
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false);
    }

    public static Creative defaultInBannerCreative(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("cpm_video_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.CANVAS)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withDuration(DEFAULT_VIDEO_DURATION)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withLayoutId(21L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false);
    }

    public static Creative defaultCpmOverlay(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("cpm_overlay")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.CPM_OVERLAY)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withDuration(DEFAULT_VIDEO_DURATION)
                .withStatusModerate(StatusModerate.NO)
                .withModerateTryCount(0L)
                .withLayoutId(CPM_OVERLAY_LAYOUT_IDS.lowerEndpoint() + 1L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false);
    }

    public static Creative defaultCpmAudioAddition(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("cpm_audio_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.CPM_AUDIO_CREATIVE)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://music.ya.ru")
                .withWidth(DEFAULT_AUDIO_FORMAT_WIDTH)
                .withHeight(DEFAULT_AUDIO_FORMAT_HEIGHT)
                .withDuration(DEFAULT_AUDIO_DURATION)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withLayoutId(CPM_AUDIO_LAYOUT_IDS.lowerEndpoint() + 1L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false);
    }

    public static Creative defaultCpmGeoPinAddition(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("cpm_geo_pin_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.CANVAS)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://music.ya.ru")
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withModerationInfo(new ModerationInfo().withTexts(emptyList()).withImages(emptyList()));
    }

    public static Creative defaultCpcVideoForCpcVideoBanner(@Nullable ClientId clientId, Long creativeId) {
        return defaultVideoAddition(clientId, creativeId)
                .withName("cpc_video_creative")
                .withType(CreativeType.CPC_VIDEO_CREATIVE)
                .withLayoutId(CPC_VIDEO_LAYOUT_IDS.lowerEndpoint() + 2L)
                .withIsAdaptive(false);
    }

    public static Creative defaultCpmOutdoorVideoAddition(@Nullable ClientId clientId, Long creativeId) {
        return defaultVideoAddition(clientId, creativeId)
                .withName("cpm_outdoor_creative")
                .withType(CreativeType.CPM_OUTDOOR_CREATIVE)
                .withLayoutId(CPM_OUTDOOR_VIDEO_LAYOUT_IDS.lowerEndpoint() + 2L)
                .withIsAdaptive(false)
                .withAdditionalData(new AdditionalData()
                        .withDuration(BigDecimal.valueOf(DEFAULT_OUTDOOR_VIDEO_DURATION))
                        .withFormats(asList(
                                new VideoFormat()
                                        .withWidth(DEFAULT_OUTDOOR_VIDEO_FORMAT_WIDTH)
                                        .withHeight(DEFAULT_OUTDOOR_VIDEO_FORMAT_HEIGHT)
                                        .withType("video/mp4")
                                        .withUrl("http://abc.com/1"),
                                new VideoFormat()
                                        .withWidth(720)
                                        .withHeight(360)
                                        .withType("video/mp4")
                                        .withUrl("http://abc.com/2"))));
    }

    public static Creative defaultCpmIndoorVideoAddition(@Nullable ClientId clientId, Long creativeId) {
        return defaultVideoAddition(clientId, creativeId)
                .withName("cpm_indoor_creative")
                .withType(CreativeType.CPM_INDOOR_CREATIVE)
                .withLayoutId(CPM_INDOOR_VIDEO_LAYOUT_IDS.lowerEndpoint() + 2L)
                .withDuration(BigDecimal.valueOf(DEFAULT_INDOOR_VIDEO_DURATION).longValue())
                .withIsAdaptive(false)
                .withAdditionalData(new AdditionalData()
                        .withDuration(BigDecimal.valueOf(DEFAULT_INDOOR_VIDEO_DURATION))
                        .withFormats(asList(
                                new VideoFormat()
                                        .withWidth(DEFAULT_INDOOR_VIDEO_FORMAT_WIDTH)
                                        .withHeight(DEFAULT_INDOOR_VIDEO_FORMAT_HEIGHT)
                                        .withType("video/mp4")
                                        .withUrl("http://abc.com/1"),
                                new VideoFormat()
                                        .withWidth(640)
                                        .withHeight(360)
                                        .withType("video/mp4")
                                        .withUrl("http://abc.com/2"))));
    }

    public static Creative defaultCpmGeoproductCanvas(@Nullable ClientId clientId, Long creativeId) {
        return defaultCanvas(clientId, creativeId)
                .withWidth(320L)
                .withHeight(50L);
    }

    public static Creative defaultCpmGeoPinCanvas(@Nullable ClientId clientId, Long creativeId) {
        return defaultCanvas(clientId, creativeId)
                .withWidth(900L)
                .withHeight(300L);
    }

    public static Creative defaultCpmVideoForCpmBanner(@Nullable ClientId clientId, Long creativeId) {
        return defaultVideoAddition(clientId, creativeId)
                .withName("cpm_video_creative")
                .withType(CreativeType.CPM_VIDEO_CREATIVE)
                .withLayoutId(CPM_VIDEO_ADDITION_LAYOUT_IDS.span().lowerEndpoint() + 2L);
    }

    public static Creative defaultNonSkippableCpmVideoCreative(@Nullable ClientId clientId, Long creativeId) {
        return defaultVideoAddition(clientId, creativeId)
                .withName("cpm_video_creative")
                .withType(CreativeType.CPM_VIDEO_CREATIVE)
                .withLayoutId(NON_SKIPPABLE_CPM_VIDEO_LAYOUT_ID.span().lowerEndpoint());
    }

    public static Creative defaultCanvas(@Nullable ClientId clientId, Long creativeId) {
        return defaultCanvas(clientId, creativeId, false);
    }

    public static Creative defaultAdaptive(@Nullable ClientId clientId, Long creativeId) {
        return defaultCanvas(clientId, creativeId, true);
    }

    public static Creative defaultCanvas(@Nullable ClientId clientId, Long creativeId, boolean isAdaptive) {
        return new Creative()
                .withId(creativeId)
                .withName("new canvas_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.CANVAS)
                .withIsGenerated(false)
                .withPreviewUrl("http://yandex.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withModerationInfo(new ModerationInfo().withHtml(
                        new ModerationInfoHtml().withUrl("http://mds.yandex.ru/somepath/preview?compact=1")))
                .withWidth(480L)
                .withHeight(320L)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withIsAdaptive(isAdaptive)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withIsBannerstoragePredeployed(false)
                .withLayoutId(1L);
    }

    public static Creative defaultPriceSalesHtml5(@Nullable ClientId clientId, Long creativeId,
                                                  CpmPriceCampaign campaign) {

        Set<CreativeSizeWithExpand> creativeSizeWithExpands =
                collectCampaignCreativeSizesWithExpand(campaign).stream().flatMap(Collection::stream).collect(Collectors.toSet());

        // если есть возможность с расхлопом - создаём с расхлопом
        CreativeSizeWithExpand allowedSizeWithExpand = creativeSizeWithExpands.stream()
                .filter(CreativeSizeWithExpand::getHasExpand)
                .findAny()
                .orElse(creativeSizeWithExpands.iterator().next());
        return defaultHtml5(clientId, creativeId)
                .withWidth(allowedSizeWithExpand.getWidth())
                .withHeight(allowedSizeWithExpand.getHeight())
                .withExpandedPreviewUrl(allowedSizeWithExpand.getHasExpand() ? DEFAULT_EXPANDED_PREVIEW_URL : null);
    }

    public static Creative defaultHtml5(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("new html5_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.HTML5_CREATIVE)
                .withIsGenerated(false)
                .withPreviewUrl("http://yandex.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withArchiveUrl("https://mds.yandex.ru/direct-canvas/some-creative.zip")
                .withYabsData(new YabsData().withBasePath("https://mds.yandex.ru/html5/resources/").withHtml5(true))
                .withWidth(480L)
                .withHeight(320L)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withIsBannerstoragePredeployed(false);
    }

    public static Creative defaultCpmGeoproductHtml5Creative(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("new html5_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.HTML5_CREATIVE)
                .withIsGenerated(false)
                .withPreviewUrl("http://yandex.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withArchiveUrl("https://mds.yandex.ru/direct-canvas/some-creative.zip")
                .withYabsData(new YabsData().withBasePath("https://mds.yandex.ru/html5/resources/").withHtml5(true))
                .withWidth(640L)
                .withHeight(100L)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withLayoutId(10L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withIsBannerstoragePredeployed(false);
    }

    // полностью заполненный объект
    public static Creative fullCreative(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withName("video_addition_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                .withIsGenerated(false)
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withArchiveUrl("http://archived.url.ru")
                .withStatusModerate(StatusModerate.YES)
                .withYabsData(new YabsData().withBasePath("yabsPath").withHtml5(true))
                .withSourceMediaType(SourceMediaType.PNG)
                .withModerateTryCount(0L)
                .withHeight(300L)
                .withWidth(200L)
                .withLayoutId(TEXT_VIDEO_ADDITION_LAYOUT_IDS.lowerEndpoint() + 1L)
                .withDuration(15L)
                .withModerationInfo(new ModerationInfo()
                        .withContentId(1L)
                        .withHtml(new ModerationInfoHtml().withUrl("http://moderation.url.ru"))
                        .withAspects(singletonList(new ModerationInfoAspect().withHeight(100L).withWidth(100L)))
                        .withTexts(singletonList(
                                new ModerationInfoText().withText("text").withColor("red").withType("short")))
                        .withImages(singletonList(new ModerationInfoImage().withAlt("alt url").withType("image").withUrl("image url")))
                        .withVideos(singletonList(new ModerationInfoVideo().withStockId("123L").withUrl("video url")))
                        .withSounds(singletonList(new ModerationInfoSound().withStockId("123L").withUrl("sound url")))
                )
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withIsBannerstoragePredeployed(false);
    }

    public static Creative defaultPerformanceCreative(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withType(CreativeType.PERFORMANCE)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withStockCreativeId(creativeId)
                .withName("performance_creative")
                .withPreviewUrl("http://ya.ru")
                .withIsGenerated(false)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withLayoutId(45L) // "Смарт-плитка 1х1"
                .withThemeId(19L) // "Розничная торговля"
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withIsBannerstoragePredeployed(true);
    }

    public static Creative defaultBannerstorageCreative(@Nullable ClientId clientId, Long creativeId) {
        return new Creative()
                .withId(creativeId)
                .withType(CreativeType.BANNERSTORAGE)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withStockCreativeId(creativeId)
                .withName("Кухни Мария")
                .withPreviewUrl("http://ya.ru")
                .withIsGenerated(false)
                .withStatusModerate(StatusModerate.NEW)
                .withModerateTryCount(0L)
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withDuration(12L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false)
                .withIsBannerstoragePredeployed(false);
    }
}
