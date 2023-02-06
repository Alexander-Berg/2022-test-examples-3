package ru.yandex.direct.core.testing.data;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotionvideo.model.ContentPromotionVideo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.libs.video.model.VideoBanner;
import ru.yandex.direct.utils.HashingUtils;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.utils.JsonUtils.fromJson;

@ParametersAreNonnullByDefault
public class TestContentPromotionVideos {
    private static final String VIDEO_HREF = "https://www.youtube.com";
    private static final String VIDEO_PREVIEW_URL = "https://www.google.com";
    private static final String METADATA = "{\"meta\": \"data\"}";
    public static final String DB_META_VIDEO_JSON_WITH_PREVIEW_TITLE_AND_PASSAGE =
            "{"
                    + " \"Title\": \"title\", "
                    + "\"Passage\": [\"description\"], "
                    + "\"thmb_href\":\"//youtube.com\""
                    + "}";

    public static ContentPromotionVideo defaultContentPromotionVideo(ClientId clientId) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        return new ContentPromotionVideo()
                .withClientId(clientId.asLong())
                .withVideoMetadata(METADATA)
                .withVideoMetadataHash(BigInteger.ONE)
                .withVideoHref(VIDEO_HREF)
                .withVideoPreviewUrl(VIDEO_PREVIEW_URL)
                .withVideoHrefHash(BigInteger.TEN)
                .withVideoMetadataRefreshTime(now)
                .withVideoMetadataCreateTime(now)
                .withVideoMetadataModifyTime(now)
                .withIsInaccessible(false);
    }

    public static VideoBanner realLifeVideoBanner() throws IOException {
        URL url = Resources.getResource("ru/yandex/direct/core/entity/contentpromotionvideo/video_banners.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        Map<String, VideoBanner> banners = VideoBanner.parseBannerCollectionJson(response);
        return banners.get("www.youtube.com/watch?v=0hCBBnZI2AU");
    }

    public static ContentPromotionVideo fromVideoBanner(VideoBanner banner, ClientId clientId) {
        String url = "https://" + banner.getUrl();
        BigInteger videoHrefHash = HashingUtils.getMd5HalfHashUtf8(url);
        String videoBannerJson = JsonUtils.toJson(banner);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        return new ContentPromotionVideo()
                .withVideoHref(url)
                .withVideoHrefHash(videoHrefHash)
                .withVideoPreviewUrl(banner.getThmbHref())
                .withVideoMetadata(videoBannerJson)
                .withVideoMetadataHash(HashingUtils.getMd5HalfHashUtf8(videoBannerJson))

                .withClientId(clientId.asLong())
                .withIsInaccessible(false)
                .withVideoMetadataRefreshTime(now)
                .withVideoMetadataCreateTime(now)
                .withVideoMetadataModifyTime(now);
    }

    public static ContentPromotionContent fromVideoBannerAsContent(VideoBanner banner, ClientId clientId) {
        String url = "https://" + banner.getUrl();
        BigInteger videoHrefHash = HashingUtils.getMd5HalfHashUtf8(url);
        String videoBannerJson = JsonUtils.toJson(banner);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        return new ContentPromotionContent()
                .withUrl(url)
                .withType(ContentPromotionContentType.VIDEO)
                .withExternalId(videoHrefHash.toString())
                .withPreviewUrl(banner.getThmbHref())
                .withMetadata(videoBannerJson)
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(videoBannerJson))
                .withClientId(clientId.asLong())
                .withIsInaccessible(false)
                .withMetadataRefreshTime(now)
                .withMetadataCreateTime(now)
                .withMetadataModifyTime(now);
    }

    public static ContentPromotionContent fromVideoBannerAsContent(VideoBanner banner, ClientId clientId, String url) {
        BigInteger videoHrefHash = HashingUtils.getMd5HalfHashUtf8(url);
        banner.setUrl(url);
        String videoBannerJson = JsonUtils.toJson(banner);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        return new ContentPromotionContent()
                .withUrl(url)
                .withType(ContentPromotionContentType.VIDEO)
                .withExternalId(videoHrefHash.toString())
                .withPreviewUrl(banner.getThmbHref())
                .withMetadata(videoBannerJson)
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(videoBannerJson))
                .withClientId(clientId.asLong())
                .withIsInaccessible(false)
                .withMetadataRefreshTime(now)
                .withMetadataCreateTime(now)
                .withMetadataModifyTime(now);
    }
}
