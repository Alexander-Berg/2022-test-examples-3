package ru.yandex.direct.web.testing.data;

import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.web.entity.adgroup.model.PixelKind;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerCreative;
import ru.yandex.direct.web.entity.banner.model.WebBannerImageAd;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBannerContentRes;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.VALID_CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.VALID_CONTENT_PROMOTION_VIDEO_ID;

public final class TestBanners {

    private TestBanners() {
    }

    public static WebBanner randomTitleWebTextBanner(Long id) {
        return new WebBanner()
                .withId(id)
                .withAdType(BannersBannerType.text.getLiteral())
                .withBannerType("desktop")
                .withTitle(randomAlphabetic(8))
                .withTitleExtension(randomAlphabetic(8))
                .withBody(randomAlphabetic(8))
                .withUrlProtocol("https://")
                // URL дефолтного баннера должен матчиться с URL дефолтного сайтлинка
                .withHref("www.yandex.ru")
                .withDisplayHref("яндекс")
                .withTurbolandingHrefParams("param1=value1&param2=value2");
    }

    public static WebBanner webImageCreativeBanner(Long id, Long creativeId) {
        return new WebBanner()
                .withId(id)
                .withAdType(BannersBannerType.image_ad.getLiteral())
                .withUrlProtocol("https://")
                .withHref("www.yandex.ru")
                .withCreative(new WebBannerCreative().withCreativeId(String.valueOf(creativeId)));
    }

    public static WebBanner webImageHashBanner(Long id, String imageHash) {
        return new WebBanner()
                .withId(id)
                .withAdType(BannersBannerType.image_ad.getLiteral())
                .withUrlProtocol("https://")
                .withHref("www.yandex.ru")
                .withImageAd(new WebBannerImageAd().withHash(imageHash));
    }

    public static WebBanner webCpcVideoBanner(Long id, Long creativeId) {
        return new WebBanner()
                .withId(id)
                .withAdType(BannersBannerType.cpc_video.getLiteral())
                .withUrlProtocol("https://")
                .withHref("www.yandex.ru")
                .withCreative(new WebBannerCreative().withCreativeId(String.valueOf(creativeId)));

    }

    public static WebCpmBanner webCpmBanner(Long id, Long creativeId) {
        return new WebCpmBanner()
                .withId(id)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withPixels(singletonList(new WebPixel()
                        .withUrl(yaAudiencePixelUrl())
                        .withKind(PixelKind.AUDIENCE)))
                .withUrlProtocol("https://")
                .withHref("www.yandex.ru/company")
                .withCreative(new WebBannerCreative().withCreativeId(creativeId.toString()))
                .withTurbolandingHrefParams("param1=value1&param2=value2");
    }

    public static WebContentPromotionBanner webContentPromotionCollectionBanner(Long id) {
        return new WebContentPromotionBanner()
                .withId(id)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withTitle(null)
                .withDescription(null)
                .withContentResource(
                        new WebContentPromotionBannerContentRes().withContentId(VALID_CONTENT_PROMOTION_ID))
                .withVisitUrl("https://www.yandex.ru/");
    }

    public static WebContentPromotionBanner webContentPromotionCollectionBanner(Long id,
                                                                                Long contentId) {
        return new WebContentPromotionBanner()
                .withId(id)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withTitle(null)
                .withDescription(null)
                .withContentResource(
                        new WebContentPromotionBannerContentRes().withContentId(contentId))
                .withVisitUrl("https://www.yandex.ru/");
    }

    public static WebContentPromotionBanner webContentPromotionVideoBanner(Long id) {
        return webContentPromotionVideoBanner(id, VALID_CONTENT_PROMOTION_VIDEO_ID);
    }

    public static WebContentPromotionBanner webContentPromotionVideoBanner(Long id, Long contentId) {
        return new WebContentPromotionBanner()
                .withId(id)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withTitle("title")
                .withDescription("description")
                .withVisitUrl("https://www.yandex.ru/")
                .withContentResource(new WebContentPromotionBannerContentRes().withContentId(contentId));
    }

    public static WebCpmBanner webCpmOutdoorBanner(Long id, Long creativeId) {
        return new WebCpmBanner()
                .withId(id)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https://")
                .withHref("www.yandex.ru")
                .withCreative(new WebBannerCreative().withCreativeId(creativeId.toString()));
    }

    public static WebCpmBanner webCpmIndoorBanner(Long id, Long creativeId) {
        return new WebCpmBanner()
                .withId(id)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https://")
                .withHref("www.yandex.ru")
                .withCreative(new WebBannerCreative().withCreativeId(creativeId.toString()));
    }
}
