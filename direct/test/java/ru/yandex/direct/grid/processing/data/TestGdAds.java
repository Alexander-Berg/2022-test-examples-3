package ru.yandex.direct.grid.processing.data;

import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.banner.GdTurboGalleryParams;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAd;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;

@SuppressWarnings("WeakerAccess")
public class TestGdAds {
    private static final String TITLE = "title";
    private static final String TITLE_EXTENSION = "titleExtension";
    private static final String BODY = "body";
    private static final String HREF = "https://ya.ru";
    private static final String DISPLAY_HREF = "ya.ru";
    private static final String DOMAIN = "https://ya.ru";
    private static final String NAME = "name";
    private static final String TURBOLANDING_HREF_PARAMS = "turbolandingHrefParams";
    private static final String HASH = "hash";
    private static final String TURBO_GALLERY_HREF = "https://yandex.ru/turbo";


    public static GdAdsContainer getDefaultGdAdsContainer() {
        return new GdAdsContainer()
                .withFilter(new GdAdFilter())
                .withOrderBy(emptyList())
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }

    public static GdUpdateAd getDefaultCreativeAd() {
        return new GdUpdateAd()
                .withAdType(GdAdType.IMAGE_AD)
                .withHref(HREF)
                .withDomain(DOMAIN)
                .withIsMobile(false)
                .withTitle(TITLE + randomNumeric(5))
                .withBody(BODY + randomNumeric(5));
    }

    public static GdUpdateAd getDefaultCreativeAdWithStaticIds() {
        return getDefaultCreativeAd()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCreativeId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS);
    }

    public static GdUpdateAd getDefaultImageHashAd() {
        return new GdUpdateAd()
                .withAdType(GdAdType.IMAGE_AD)
                .withHref(HREF)
                .withDomain(DOMAIN)
                .withImageCreativeHash(HASH + randomNumeric(5))
                .withIsMobile(false)
                .withTitle(TITLE + randomNumeric(5))
                .withBody(BODY + randomNumeric(5));
    }

    public static GdUpdateAd getDefaultImageHashAdWithStaticIds() {
        return getDefaultImageHashAd()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS);
    }

    public static GdUpdateAd getDefaultCpcVideoAd() {
        return new GdUpdateAd()
                .withAdType(GdAdType.CPC_VIDEO)
                .withHref(HREF)
                .withIsMobile(false)
                .withTitle(TITLE + randomNumeric(5))
                .withBody(BODY + randomNumeric(5))
                ;
    }

    public static GdUpdateAd getDefaultCpcVideoAdWithStaticIds() {
        return getDefaultCreativeAd()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withAdType(GdAdType.CPC_VIDEO)
                .withCreativeId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS);
    }

    public static GdUpdateAd getDefaultTextAd() {
        return getSimpleTextAd()
                .withTitleExtension(TITLE_EXTENSION + randomNumeric(5))
                .withDisplayHref(DISPLAY_HREF)
                .withDomain(DOMAIN)
                .withTurbolandingId(RandomNumberUtils.nextPositiveLong())
                .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS)
                .withTurboGalleryParams(new GdTurboGalleryParams().withTurboGalleryHref(TURBO_GALLERY_HREF))
                .withTextBannerImageHash(HASH + randomNumeric(5));
    }

    public static GdUpdateAd getDefaultTextAdWithStaticIds() {
        return getDefaultTextAd()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCalloutIds(singletonList(RandomNumberUtils.nextPositiveLong()))
                .withVcardId(RandomNumberUtils.nextPositiveLong());
    }


    public static GdUpdateAd getSimpleTextAd() {
        return new GdUpdateAd()
                .withAdType(GdAdType.TEXT)
                .withIsMobile(false)
                .withTitle(TITLE + randomNumeric(5))
                .withBody(BODY + randomNumeric(5))
                .withHref(HREF);
    }

    public static GdUpdateAd getTextAdWithPrice(GdAdPrice price) {
        return new GdUpdateAd()
                .withAdType(GdAdType.TEXT)
                .withIsMobile(false)
                .withTitle(TITLE + randomNumeric(5))
                .withBody(BODY + randomNumeric(5))
                .withHref(HREF)
                .withAdPrice(price);
    }

    public static GdUpdateAd getTextAdWithPermalink(Long permalinkId) {
        return new GdUpdateAd()
                .withAdType(GdAdType.TEXT)
                .withIsMobile(false)
                .withTitle(TITLE + randomNumeric(5))
                .withBody(BODY + randomNumeric(5))
                .withHref(HREF)
                .withPermalinkId(permalinkId);
    }

    public static GdUpdateAd getSimpleTextAdWithStaticIds() {
        return getSimpleTextAd()
                .withId(RandomNumberUtils.nextPositiveLong());
    }
}
