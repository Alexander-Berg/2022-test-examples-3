package ru.yandex.direct.core.entity.moderation.service.receiving;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerVideoAdditionModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerVideoAdditionModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.image.ImageModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;

@ParametersAreNonnullByDefault
public class ModerationResponseUtil {
    public static final List<Long> REASONS_IDS_RESPONSE = List.of(1L, 2L, 3L, 4L, 5L, 7L);
    public static final List<ModerationReasonDetailed> DETAILED_REASONS_RESPONSE = List.of(
            new ModerationReasonDetailed()
                    .withId(1L)
                    .withComment("test comment1")
                    .withScreenshots(List.of("/screenshot_url1", "/screenshot_url2")),
            new ModerationReasonDetailed()
                    .withId(2L)
                    .withComment("test comment2")
                    .withScreenshots(List.of("/screenshot_url3", "/screenshot_url4")),
            new ModerationReasonDetailed()
                    .withId(1L)
                    .withComment("ignored")
                    .withScreenshots(List.of("/screenshot_url5", "/screenshot_url6")),
            new ModerationReasonDetailed()
                    .withId(3L)
                    .withComment("test comment4"),
            new ModerationReasonDetailed()
                    .withId(4L)
                    .withScreenshots(List.of("/screenshot_url7", "/screenshot_url8")),
            new ModerationReasonDetailed()
                    .withId(5L)
                    .withComment("")
                    .withScreenshots(List.of()),
            new ModerationReasonDetailed()
                    .withId(6L)
                    .withComment("ignored")
    );

    public static final List<ModerationReasonDetailed> EXPECTED_REASONS_IN_DB = List.of(
            new ModerationReasonDetailed()
                    .withId(1L)
                    .withComment("test comment1")
                    .withScreenshots(List.of("/screenshot_url1", "/screenshot_url2")),
            new ModerationReasonDetailed()
                    .withId(2L)
                    .withComment("test comment2")
                    .withScreenshots(List.of("/screenshot_url3", "/screenshot_url4")),
            new ModerationReasonDetailed()
                    .withId(3L)
                    .withComment("test comment4"),
            new ModerationReasonDetailed()
                    .withId(4L)
                    .withScreenshots(List.of("/screenshot_url7", "/screenshot_url8")),
            new ModerationReasonDetailed()
                    .withId(5L)
                    .withComment("")
                    .withScreenshots(List.of()),
            new ModerationReasonDetailed()
                    .withId(7L)
    );

    public static BannerModerationResponse makeTextBannerResponse(long bid, long version, ModerationDecision status,
                                                              List<Long> reasons) {
        return makeBannerResponse(bid, version, status, reasons, null, ModerationObjectType.TEXT_AD);
    }

    public static BannerModerationResponse makeBannerResponse(long bid, long version, ModerationDecision status,
                                                              List<Long> reasons,
                                                              ModerationObjectType objectType) {
        return makeBannerResponse(bid, version, status, reasons, null, objectType);
    }

    public static BannerModerationResponse makeBannerResponse(long bid, long version, ModerationDecision status,
                                                              long unixtime,
                                                              ModerationObjectType objectType) {
        return makeBannerResponse(bid, version, status, List.of(), null, unixtime, null, objectType);
    }

    public static BannerModerationResponse makeBannerResponse(long bid, long version, ModerationDecision status,
                                                              Map<String, String> flags,
                                                              ModerationObjectType objectType) {
        return makeBannerResponse(bid, version, status, List.of(), null, 0, flags, objectType);
    }

    public static BannerAssetModerationResponse makeAssetResponse(long bid, long version, ModerationDecision status,
                                                                  List<Long> reasons,
                                                                  @Nullable List<ModerationReasonDetailed> detailedReasons,
                                                                  ModerationObjectType objectType) {
        BannerAssetModerationMeta meta = new BannerAssetModerationMeta();
        meta.setBannerId(bid);
        meta.setVersionId(version);
        BannerAssetModerationResponse response = new BannerAssetModerationResponse();
        response.setType(objectType);
        response.setMeta(meta);
        response.setResult(createVerdict(status, reasons, detailedReasons, null));
        return response;
    }

    public static BannerVideoAdditionModerationResponse makeVideoAdditionResponse(long bid, long version, long creativeId,
                                                                                  ModerationDecision status,
                                                                                  List<Long> reasons,
                                                                                  @Nullable List<ModerationReasonDetailed> detailedReasons) {
        BannerVideoAdditionModerationMeta meta = new BannerVideoAdditionModerationMeta();
        meta.setBannerId(bid);
        meta.setVersionId(version);
        meta.setCreativeId(creativeId);

        BannerVideoAdditionModerationResponse response = new BannerVideoAdditionModerationResponse();
        response.setType(ModerationObjectType.BANNER_VIDEO_ADDITION);
        response.setMeta(meta);
        response.setResult(createVerdict(status, reasons, detailedReasons, null));
        return response;
    }

    public static BannerModerationResponse makeBannerResponse(long bid, long version, ModerationDecision status,
                                                              List<Long> reasons,
                                                              @Nullable List<ModerationReasonDetailed> detailedReasons,
                                                              ModerationObjectType objectType) {
        return makeBannerResponse(bid, version, status, reasons, detailedReasons, 0, null, objectType);
    }

    public static BannerModerationResponse makeBannerResponse(long bid, long version, ModerationDecision status,
                                                              List<Long> reasons,
                                                              @Nullable List<ModerationReasonDetailed> detailedReasons,
                                                              long unixtime, @Nullable Map<String, String> flags,
                                                              ModerationObjectType objectType) {
        BannerModerationMeta bannerModerationMeta = new BannerModerationMeta();
        bannerModerationMeta.setBannerId(bid);
        bannerModerationMeta.setVersionId(version);

        BannerModerationResponse bannerModerationResponse = new BannerModerationResponse();
        bannerModerationResponse.setType(objectType);
        bannerModerationResponse.setResult(createVerdict(status, reasons, detailedReasons, flags));
        bannerModerationResponse.setMeta(bannerModerationMeta);
        bannerModerationResponse.setUnixtime(unixtime);

        return bannerModerationResponse;
    }

    public static ImageModerationResponse makeImageResponse(long bid, long version, ModerationDecision status,
                                                            List<Long> reasons,
                                                            @Nullable List<ModerationReasonDetailed> detailedReasons) {
        BannerAssetModerationMeta imageModerationMeta = new BannerAssetModerationMeta();
        imageModerationMeta.setBannerId(bid);
        imageModerationMeta.setVersionId(version);

        ImageModerationResponse imageModerationResponse = new ImageModerationResponse();
        imageModerationResponse.setType(ModerationObjectType.IMAGES);
        imageModerationResponse.setResult(createVerdict(status, reasons, detailedReasons, null));
        imageModerationResponse.setMeta(imageModerationMeta);

        return imageModerationResponse;
    }

    public static DisplayHrefsModerationResponse makeDisplayHrefResponse(long bid, long version,
                                                                         ModerationDecision status,
                                                                         List<Long> reasons,
                                                                         @Nullable List<ModerationReasonDetailed> detailedReasons) {
        DisplayHrefsModerationMeta displayHrefModerationMeta = new DisplayHrefsModerationMeta();
        displayHrefModerationMeta.setBannerId(bid);
        displayHrefModerationMeta.setVersionId(version);

        DisplayHrefsModerationResponse displayHrefModerationResponse = new DisplayHrefsModerationResponse();
        displayHrefModerationResponse.setType(ModerationObjectType.DISPLAYHREFS);
        displayHrefModerationResponse.setResult(createVerdict(status, reasons, detailedReasons, null));
        displayHrefModerationResponse.setMeta(displayHrefModerationMeta);

        return displayHrefModerationResponse;
    }

    public static SitelinksModerationResponse makeSitelinkSetResponse(long bid, long version, ModerationDecision status,
                                                                      List<Long> reasons,
                                                                      @Nullable List<ModerationReasonDetailed> detailedReasons) {
        SitelinksModerationMeta sitelinksModerationMeta = new SitelinksModerationMeta();
        sitelinksModerationMeta.setBannerId(bid);
        sitelinksModerationMeta.setVersionId(version);

        SitelinksModerationResponse sitelinksModerationResponse = new SitelinksModerationResponse();
        sitelinksModerationResponse.setType(ModerationObjectType.SITELINKS_SET);
        sitelinksModerationResponse.setResult(createVerdict(status, reasons, detailedReasons, null));
        sitelinksModerationResponse.setMeta(sitelinksModerationMeta);

        return sitelinksModerationResponse;
    }

    public static Verdict createVerdict(ModerationDecision status, List<Long> reasons,
                                        @Nullable List<ModerationReasonDetailed> detailedReasons,
                                        @Nullable Map<String, String> flags) {
        Verdict verdict = new Verdict();
        verdict.setVerdict(status);
        verdict.setReasons(List.copyOf(reasons));
        if (detailedReasons != null) {
            verdict.setDetailedReasons(List.copyOf(detailedReasons));
        }
        if (flags != null) {
            verdict.setFlags(flags);
        }
        return verdict;
    }

}
