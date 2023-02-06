package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.domain.v2.promo.BannerListWidget.MixedDatasource.Banner;

import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.collectionToStr;
import static ru.yandex.market.api.domain.v2.promo.BannerListWidget.MixedDatasource;

public class MixedDatasourceMatcher {
    public static Matcher<MixedDatasource> banners(Matcher<Iterable<? extends Banner>> banners) {
        return ApiMatchers.map(
            MixedDatasource::getBanners,
            "'banners'",
            banners,
            MixedDatasourceMatcher::toStr
        );
    }

    public static Matcher<MixedDatasource.AdfoxBanner> adfoxBanner(Matcher<MixedDatasource.AdfoxBanner> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<MixedDatasource.AdfoxBanner> adPlace(String adPlace) {
        return ApiMatchers.map(
            banner -> banner.adPlace,
            "'adPlace'",
            is(adPlace),
            MixedDatasourceMatcher::adfoxToStr
        );
    }

    public static Matcher<MixedDatasource.AdfoxBanner> pp(String pp) {
        return ApiMatchers.map(
            banner -> banner.pp,
            "'pp'",
            is(pp),
            MixedDatasourceMatcher::adfoxToStr
        );
    }

    public static Matcher<MixedDatasource.AdfoxBanner> p2(String p2) {
        return ApiMatchers.map(
            banner -> banner.p2,
            "'p2'",
            is(p2),
            MixedDatasourceMatcher::adfoxToStr
        );
    }

    public static Matcher<MixedDatasource.AdfoxBanner> puidName(String puidName) {
        return ApiMatchers.map(
                banner -> banner.puidName,
                "'puidName'",
                is(puidName),
                MixedDatasourceMatcher::adfoxToStr
        );
    }

    public static Matcher<MixedDatasource.AdfoxBanner> puidValue(String puidValue) {
        return ApiMatchers.map(
                banner -> banner.puidValue,
                "'puidValue'",
                is(puidValue),
                MixedDatasourceMatcher::adfoxToStr
        );
    }

    public static Matcher<MixedDatasource.ImageBanner> imageBanner(Matcher<MixedDatasource.ImageBanner> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<MixedDatasource.ImageBanner> image(Matcher<Image> image) {
        return ApiMatchers.map(
            banner -> banner.image,
            "'image'",
            image,
            MixedDatasourceMatcher::imageToStr
        );
    }

    public static Matcher<MixedDatasource.ImageBanner> link(String link) {
        return ApiMatchers.map(
            banner -> banner.link,
            "'link'",
            is(link),
            MixedDatasourceMatcher::imageToStr
        );
    }

    public static String toStr(MixedDatasource datasource) {
        if (null == datasource) {
            return "null";
        }

        return MoreObjects.toStringHelper(MixedDatasource.class)
            .add(
                "banners",
                collectionToStr(
                    datasource.getBanners(),
                    MixedDatasourceMatcher::bannerToStr
                )
            )
            .toString();
    }

    private static String bannerToStr(Banner banner) {
        if (banner instanceof MixedDatasource.ImageBanner) {
            return imageToStr((MixedDatasource.ImageBanner) banner);
        } else if (banner instanceof MixedDatasource.AdfoxBanner) {
            return adfoxToStr((MixedDatasource.AdfoxBanner) banner);
        } else {
            return "null";
        }
    }

    private static String imageToStr(MixedDatasource.ImageBanner banner) {
        if (null == banner) {
            return "null";
        }
        return MoreObjects.toStringHelper(MixedDatasource.ImageBanner.class)
            .add("image", ImageMatcher.toStr(banner.image))
            .add("link", banner.link)
            .toString();
    }

    private static String adfoxToStr(MixedDatasource.AdfoxBanner banner) {
        if (null == banner) {
            return "null";
        }
        return MoreObjects.toStringHelper(MixedDatasource.AdfoxBanner.class)
                .add("adPlace", banner.adPlace)
                .add("pp", banner.pp)
                .add("p2", banner.p2)
                .add("puidName", banner.puidName)
                .add("puidValue", banner.puidValue)
                .toString();
    }
}
